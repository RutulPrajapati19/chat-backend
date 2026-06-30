package com.chat.chat_backend.controller;

import com.chat.chat_backend.dto.*;
import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.model.JoinRequest;
import com.chat.chat_backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomSummaryResponse>> getAllRooms(Principal principal) {
        return ResponseEntity.ok(roomService.getAllRoomsFor(principal.getName()));
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request, Principal principal) {
        try {
            ChatRoom room = roomService.createRoom(request, principal.getName());
            return ResponseEntity.ok(Map.of(
                    "id", room.getId(),
                    "name", room.getName(),
                    "createdBy", room.getCreatedBy()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> requestToJoin(@PathVariable String roomId,
                                           @RequestBody JoinRoomRequest request,
                                           Principal principal) {
        try {
            JoinRequest joinRequest = roomService.requestToJoin(roomId, request, principal.getName());
            return ResponseEntity.ok(Map.of(
                    "status", joinRequest.getStatus().name(),
                    "message", "Join request submitted. Waiting for admin approval."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid room or password"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/requests")
    public ResponseEntity<?> getPendingRequests(@PathVariable String roomId, Principal principal) {
        try {
            return ResponseEntity.ok(roomService.getPendingRequests(roomId, principal.getName()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/requests/{requestId}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable String roomId,
                                            @PathVariable String requestId,
                                            Principal principal) {
        try {
            roomService.approveRequest(roomId, requestId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Request approved"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/requests/{requestId}/decline")
    public ResponseEntity<?> declineRequest(@PathVariable String roomId,
                                            @PathVariable String requestId,
                                            Principal principal) {
        try {
            roomService.declineRequest(roomId, requestId, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Request declined"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{roomId}/members/{username}")
    public ResponseEntity<?> removeMember(@PathVariable String roomId,
                                          @PathVariable String username,
                                          Principal principal) {
        try {
            roomService.removeMember(roomId, username, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Member removed"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/access")
    public ResponseEntity<?> checkAccess(@PathVariable String roomId, Principal principal) {
        boolean hasAccess = roomService.canAccessMessages(roomId, principal.getName());
        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }
}