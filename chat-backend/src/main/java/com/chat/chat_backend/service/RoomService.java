package com.chat.chat_backend.service;

import com.chat.chat_backend.dto.*;
import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.model.JoinRequest;
import com.chat.chat_backend.model.User;
import com.chat.chat_backend.repository.ChatRoomRepository;
import com.chat.chat_backend.repository.JoinRequestRepository;
import com.chat.chat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final ChatRoomRepository roomRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ChatRoom createRoom(CreateRoomRequest request, String username) {
        if (request.getName() == null || request.getName().trim().isEmpty())
            throw new IllegalArgumentException("Room name is required");

        ChatRoom room = new ChatRoom();
        room.setName(request.getName().trim());

        // Password is optional
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            room.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        room.setCreatedBy(username);
        return roomRepository.save(room);
    }

    public List<RoomSummaryResponse> getAllRoomsFor(String username) {
        return roomRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(room -> toSummary(room, username))
                .collect(Collectors.toList());
    }

    public RoomSummaryResponse toSummary(ChatRoom room, String username) {
        String status;
        if (room.isAdmin(username)) {
            status = "ADMIN";
        } else if (room.isApprovedMember(username)) {
            status = "MEMBER";
        } else {
            var pending = joinRequestRepository
                    .findByRoomIdAndUsernameAndStatus(room.getId(), username, JoinRequest.Status.PENDING);
            status = pending.isPresent() ? "PENDING" : "NONE";
        }
        return new RoomSummaryResponse(
                room.getId(), room.getName(), room.getCreatedBy(),
                room.isAdmin(username), status,
                room.getApprovedMemberIds().size() + 1
        );
    }

    public JoinRequest requestToJoin(String roomId, JoinRoomRequest request, String username) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (room.isBanned(username))
            throw new IllegalStateException("You are banned from this room");

        if (room.isAdmin(username))
            throw new IllegalStateException("You are the admin of this room");

        if (room.isApprovedMember(username))
            throw new IllegalStateException("You are already a member");

        // Rate limit: max 5 attempts per 15 minutes
        long attempts = joinRequestRepository.countByRoomIdAndUsernameAndRequestedAtAfter(
                roomId, username, LocalDateTime.now().minusMinutes(15));
        if (attempts >= 5)
            throw new IllegalStateException("Too many attempts. Try again later.");

        // Check password if room has one
        if (room.getPasswordHash() != null && !room.getPasswordHash().isEmpty()) {
            if (request.getPassword() == null ||
                    !passwordEncoder.matches(request.getPassword(), room.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid room password");
            }
        }

        // Return existing pending request if already submitted
        var existing = joinRequestRepository
                .findByRoomIdAndUsernameAndStatus(roomId, username, JoinRequest.Status.PENDING);
        if (existing.isPresent()) return existing.get();

        // Get user email
        User user = userRepository.findByUsername(username).orElse(null);
        String userEmail = user != null ? user.getEmail() : null;

        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setRoomId(roomId);
        joinRequest.setRoomName(room.getName());
        joinRequest.setUsername(username);
        joinRequest.setUserEmail(userEmail);
        JoinRequest saved = joinRequestRepository.save(joinRequest);

        // Email the admin
        User admin = userRepository.findByUsername(room.getCreatedBy()).orElse(null);
        if (admin != null && admin.getEmail() != null) {
            emailService.sendJoinRequestNotificationToAdmin(
                    admin.getEmail(), admin.getUsername(), username, room.getName());
        }

        return saved;
    }

    public List<JoinRequestResponse> getPendingRequests(String roomId, String adminUsername) {
        getRoomAsAdmin(roomId, adminUsername);
        return joinRequestRepository.findByRoomIdAndStatus(roomId, JoinRequest.Status.PENDING)
                .stream()
                .map(r -> new JoinRequestResponse(
                        r.getId(), r.getRoomId(), r.getRoomName(),
                        r.getUsername(), r.getStatus().name(),
                        r.getRequestedAt().toString()))
                .collect(Collectors.toList());
    }

    public void approveRequest(String roomId, String requestId, String adminUsername) {
        ChatRoom room = getRoomAsAdmin(roomId, adminUsername);
        JoinRequest request = getPendingRequest(requestId, roomId);

        request.setStatus(JoinRequest.Status.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(adminUsername);
        joinRequestRepository.save(request);

        if (!room.getApprovedMemberIds().contains(request.getUsername())) {
            room.getApprovedMemberIds().add(request.getUsername());
            roomRepository.save(room);
        }

        // Email the user
        String email = request.getUserEmail();
        if (email == null) {
            User user = userRepository.findByUsername(request.getUsername()).orElse(null);
            if (user != null) email = user.getEmail();
        }
        if (email != null) {
            emailService.sendApprovalNotificationToUser(email, request.getUsername(), room.getName());
        }
    }

    public void declineRequest(String roomId, String requestId, String adminUsername) {
        ChatRoom room = getRoomAsAdmin(roomId, adminUsername);
        JoinRequest request = getPendingRequest(requestId, roomId);

        request.setStatus(JoinRequest.Status.DECLINED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(adminUsername);
        joinRequestRepository.save(request);

        if (request.getUserEmail() != null) {
            emailService.sendDeclineNotificationToUser(
                    request.getUserEmail(), request.getUsername(), room.getName());
        }
    }

    public void removeMember(String roomId, String targetUsername, String adminUsername) {
        ChatRoom room = getRoomAsAdmin(roomId, adminUsername);
        if (room.isAdmin(targetUsername))
            throw new IllegalStateException("Cannot remove the room admin");
        room.getApprovedMemberIds().remove(targetUsername);
        if (!room.getBannedUserIds().contains(targetUsername))
            room.getBannedUserIds().add(targetUsername);
        roomRepository.save(room);
    }

    public boolean canAccessRoom(String roomId, String username) {
        return roomRepository.findById(roomId)
                .map(room -> room.isApprovedMember(username))
                .orElse(false);
    }

    public String getRoomAdmin(String roomId) {
        return roomRepository.findById(roomId)
                .map(ChatRoom::getCreatedBy)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    public String getRoomName(String roomId) {
        return roomRepository.findById(roomId)
                .map(ChatRoom::getName)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    public String getRequesterUsername(String requestId) {
        return joinRequestRepository.findById(requestId)
                .map(JoinRequest::getUsername)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
    }

    private ChatRoom getRoomAsAdmin(String roomId, String username) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (!room.isAdmin(username))
            throw new IllegalStateException("Only the room admin can perform this action");
        return room;
    }

    private JoinRequest getPendingRequest(String requestId, String roomId) {
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getRoomId().equals(roomId))
            throw new IllegalArgumentException("Request does not belong to this room");
        if (request.getStatus() != JoinRequest.Status.PENDING)
            throw new IllegalStateException("Request already resolved");
        return request;
    }
}