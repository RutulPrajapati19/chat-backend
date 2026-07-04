package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.ChatMessage;
import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.repository.ChatMessageRepository;
import com.chat.chat_backend.repository.ChatRoomRepository;
import com.chat.chat_backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageHistoryController {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final RoomService roomService;

    @GetMapping("/{roomName}")
    public ResponseEntity<?> getMessages(@PathVariable String roomName, Principal principal) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));

        String username = principal.getName();

        // Find room by name first
        ChatRoom room = roomRepository.findByName(roomName).orElse(null);

        // If not found by name, try by ID (fallback)
        if (room == null) {
            room = roomRepository.findById(roomName).orElse(null);
        }

        if (room == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Room not found"));

        if (!roomService.canAccessRoom(room.getId(), username))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not a member of this room"));

        List<ChatMessage> messages = messageRepository
                .findByRoomIdOrderByTimestampAsc(room.getId());
        return ResponseEntity.ok(messages);
    }
}