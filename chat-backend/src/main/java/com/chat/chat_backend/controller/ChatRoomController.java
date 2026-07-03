package com.chat.chat_backend.controller;

import com.chat.chat_backend.dto.CreateRoomRequest;
import com.chat.chat_backend.dto.RoomSummaryResponse;
import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomSummaryResponse>> getAllRooms(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(roomService.getAllRoomsFor(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<?> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ChatRoom room = roomService.createRoom(request, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "id", room.getId(),
                    "name", room.getName(),
                    "createdBy", room.getCreatedBy()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}