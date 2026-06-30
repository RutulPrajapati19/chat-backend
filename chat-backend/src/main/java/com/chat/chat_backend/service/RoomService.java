package com.chat.chat_backend.service;

import com.chat.chat_backend.dto.*;
import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.model.JoinRequest;
import com.chat.chat_backend.repository.ChatRoomRepository;
import com.chat.chat_backend.repository.JoinRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final int MAX_JOIN_ATTEMPTS = 5;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;
    private static final int MAX_ROOM_NAME_LENGTH = 50;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MIN_PASSWORD_LENGTH = 4;

    private final ChatRoomRepository roomRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public ChatRoom createRoom(CreateRoomRequest request, String username) {
        String name = sanitizeRoomName(request.getName());

        if (request.getPassword() == null || request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Room password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        ChatRoom room = new ChatRoom();
        room.setName(name);
        room.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        room.setCreatedBy(username);
        return roomRepository.save(room);
    }

    public List<RoomSummaryResponse> getAllRoomsFor(String username) {
        return roomRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(room -> toSummary(room, username))
                .collect(Collectors.toList());
    }

    private RoomSummaryResponse toSummary(ChatRoom room, String username) {
        String status;
        if (room.isAdmin(username)) {
            status = "ADMIN";
        } else if (room.isApprovedMember(username)) {
            status = "MEMBER";
        } else {
            var pending = joinRequestRepository.findByRoomIdAndUsernameAndStatus(
                    room.getId(), username, JoinRequest.Status.PENDING);
            status = pending.isPresent() ? "PENDING" : "NONE";
        }

        return new RoomSummaryResponse(
                room.getId(),
                room.getName(),
                room.getCreatedBy(),
                room.isAdmin(username),
                status,
                room.getApprovedMemberIds().size() + 1
        );
    }

    public JoinRequest requestToJoin(String roomId, JoinRoomRequest request, String username) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid room or password"));

        if (room.isBanned(username)) {
            throw new IllegalStateException("You cannot join this room");
        }

        if (room.isApprovedMember(username)) {
            throw new IllegalStateException("You are already a member of this room");
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MINUTES);
        long attempts = joinRequestRepository.countByRoomIdAndUsernameAndRequestedAtAfter(roomId, username, windowStart);
        if (attempts >= MAX_JOIN_ATTEMPTS) {
            throw new IllegalStateException("Too many attempts. Try again later.");
        }

        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), room.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid room or password");
        }

        var existing = joinRequestRepository.findByRoomIdAndUsernameAndStatus(roomId, username, JoinRequest.Status.PENDING);
        if (existing.isPresent()) {
            return existing.get();
        }

        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setRoomId(roomId);
        joinRequest.setRoomName(room.getName());
        joinRequest.setUsername(username);
        return joinRequestRepository.save(joinRequest);
    }

    public List<JoinRequestResponse> getPendingRequests(String roomId, String adminUsername) {
        ChatRoom room = getRoomAsAdmin(roomId, adminUsername);

        return joinRequestRepository.findByRoomIdAndStatus(roomId, JoinRequest.Status.PENDING).stream()
                .map(r -> new JoinRequestResponse(
                        r.getId(), r.getRoomId(), r.getRoomName(), r.getUsername(),
                        r.getStatus().name(), r.getRequestedAt().toString()))
                .collect(Collectors.toList());
    }

    public void approveRequest(String roomId, String requestId, String adminUsername) {
        ChatRoom room = getRoomAsAdmin(roomId, adminUsername);
        JoinRequest request = getValidPendingRequest(requestId, roomId);

        request.setStatus(JoinRequest.Status.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(adminUsername);
        joinRequestRepository.save(request);

        if (!room.getApprovedMemberIds().contains(request.getUsername())) {
            room.getApprovedMemberIds().add(request.getUsername());
            roomRepository.save(room);
        }
    }

    public void declineRequest(String roomId, String requestId, String adminUsername) {
        getRoomAsAdmin(roomId, adminUsername);
        JoinRequest request = getValidPendingRequest(requestId, roomId);

        request.setStatus(JoinRequest.Status.DECLINED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(adminUsername);
        joinRequestRepository.save(request);
    }

    public void removeMember(String roomId, String targetUsername, String adminUsername) {
        ChatRoom room = getRoomAsAdmin(roomId, adminUsername);

        if (room.isAdmin(targetUsername)) {
            throw new IllegalStateException("Cannot remove the room admin");
        }

        room.getApprovedMemberIds().remove(targetUsername);
        if (!room.getBannedUserIds().contains(targetUsername)) {
            room.getBannedUserIds().add(targetUsername);
        }
        roomRepository.save(room);
    }

    public boolean canAccessMessages(String roomId, String username) {
        ChatRoom room = roomRepository.findById(roomId).orElse(null);
        return room != null && room.isApprovedMember(username);
    }

    public boolean canSendMessage(String roomId, String username) {
        return canAccessMessages(roomId, username);
    }

    public String validateAndCleanMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long");
        }
        return trimmed;
    }

    private ChatRoom getRoomAsAdmin(String roomId, String username) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (!room.isAdmin(username)) {
            throw new IllegalStateException("Only the room admin can perform this action");
        }
        return room;
    }

    private JoinRequest getValidPendingRequest(String requestId, String roomId) {
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getRoomId().equals(roomId)) {
            throw new IllegalArgumentException("Request does not belong to this room");
        }
        if (request.getStatus() != JoinRequest.Status.PENDING) {
            throw new IllegalStateException("Request already resolved");
        }
        return request;
    }

    private String sanitizeRoomName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Room name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_ROOM_NAME_LENGTH) {
            throw new IllegalArgumentException("Room name too long");
        }
        return trimmed;
    }
}
