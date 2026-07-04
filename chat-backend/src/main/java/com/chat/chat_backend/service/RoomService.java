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

        long attempts = joinRequestRepository.countByRoomIdAndUsernameAndRequestedAtAfter(
                roomId, username, LocalDateTime.now().minusMinutes(15));
        if (attempts >= 5)
            throw new IllegalStateException("Too many attempts. Try again later.");

        if (room.getPasswordHash() != null && !room.getPasswordHash().isEmpty()) {
            if (request.getPassword() == null ||
                    !passwordEncoder.matches(request.getPassword(), room.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid room password");
            }
        }

        var existing = joinRequestRepository
                .findByRoomIdAndUsernameAndStatus(roomId, username, JoinRequest.Status.PENDING);
        if (existing.isPresent()) return existing.get();

        User user = userRepository.findByUsername(username).orElse(null);
        String userEmail = user != null ? user.getEmail() : null;

        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setRoomId(roomId);
        joinRequest.setRoomName(room.getName());
        joinRequest.setUsername(username);
        joinRequest.setUserEmail(userEmail);
        JoinRequest saved = joinRequestRepository.save(joinRequest);

        User admin = userRepository.findByUsername(room.getCreatedBy()).orElse(null);
        if (admin != null && admin.getEmail() != null) {
            emailService.sendJoinRequestNotificationToAdmin(
                    admin.getEmail(), admin.getUsername(),
                    username, room.getName(), saved.getId());
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
        JoinRequest req = getPendingRequest(requestId, roomId);

        req.setStatus(JoinRequest.Status.APPROVED);
        req.setResolvedAt(LocalDateTime.now());
        req.setResolvedBy(adminUsername);
        joinRequestRepository.save(req);

        if (!room.getApprovedMemberIds().contains(req.getUsername())) {
            room.getApprovedMemberIds().add(req.getUsername());
            roomRepository.save(room);
        }

        String email = req.getUserEmail();
        if (email == null) {
            User user = userRepository.findByUsername(req.getUsername()).orElse(null);
            if (user != null) email = user.getEmail();
        }
        if (email != null) {
            emailService.sendApprovalNotificationToUser(email, req.getUsername(), room.getName());
        }
    }

    public void declineRequest(String roomId, String requestId, String adminUsername) {
        ChatRoom room = getRoomAsAdmin(roomId, adminUsername);
        JoinRequest req = getPendingRequest(requestId, roomId);

        req.setStatus(JoinRequest.Status.DECLINED);
        req.setResolvedAt(LocalDateTime.now());
        req.setResolvedBy(adminUsername);
        joinRequestRepository.save(req);

        if (req.getUserEmail() != null) {
            emailService.sendDeclineNotificationToUser(
                    req.getUserEmail(), req.getUsername(), room.getName());
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
                .map(room -> room.isAdmin(username) || room.isApprovedMember(username))
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
        JoinRequest req = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!req.getRoomId().equals(roomId))
            throw new IllegalArgumentException("Request does not belong to this room");
        if (req.getStatus() != JoinRequest.Status.PENDING)
            throw new IllegalStateException("Request already resolved");
        return req;
    }
}