package com.chat.chat_backend.controller;

import com.chat.chat_backend.dto.JoinRoomRequest;
import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.model.JoinRequest;
import com.chat.chat_backend.repository.ChatRoomRepository;
import com.chat.chat_backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class NotificationController {

    private final RoomService roomService;
    private final ChatRoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{roomId}/request-join")
    public ResponseEntity<?> requestJoin(
            @PathVariable String roomId,
            @RequestBody JoinRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            JoinRequest jr = roomService.requestToJoin(roomId, request, userDetails.getUsername());
            String adminUsername = roomService.getRoomAdmin(roomId);
            messagingTemplate.convertAndSendToUser(
                    adminUsername, "/queue/notifications",
                    Map.of("type", "JOIN_REQUEST",
                            "requestId", jr.getId(),
                            "roomId", roomId,
                            "roomName", jr.getRoomName(),
                            "username", userDetails.getUsername()));
            return ResponseEntity.ok(Map.of("status", jr.getStatus().name(), "requestId", jr.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/requests/{requestId}/approve")
    public ResponseEntity<?> approve(
            @PathVariable String roomId,
            @PathVariable String requestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String requesterUsername = roomService.getRequesterUsername(requestId);
            String roomName = roomService.getRoomName(roomId);
            roomService.approveRequest(roomId, requestId, userDetails.getUsername());
            messagingTemplate.convertAndSendToUser(
                    requesterUsername, "/queue/notifications",
                    Map.of("type", "REQUEST_APPROVED", "roomId", roomId, "roomName", roomName));
            return ResponseEntity.ok(Map.of("message", "Approved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/requests/{requestId}/decline")
    public ResponseEntity<?> decline(
            @PathVariable String roomId,
            @PathVariable String requestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String requesterUsername = roomService.getRequesterUsername(requestId);
            String roomName = roomService.getRoomName(roomId);
            roomService.declineRequest(roomId, requestId, userDetails.getUsername());
            messagingTemplate.convertAndSendToUser(
                    requesterUsername, "/queue/notifications",
                    Map.of("type", "REQUEST_DECLINED", "roomId", roomId, "roomName", roomName));
            return ResponseEntity.ok(Map.of("message", "Declined"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/requests")
    public ResponseEntity<?> getPendingRequests(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(roomService.getPendingRequests(roomId, userDetails.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}