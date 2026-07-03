package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.ChatMessage;
import com.chat.chat_backend.repository.ChatMessageRepository;
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
    private final RoomService roomService;

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getMessages(@PathVariable String roomId, Principal principal) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));

        String username = principal.getName();

        // Allow admin OR approved member
        if (!roomService.canAccessRoom(roomId, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not a member of this room"));
        }

        List<ChatMessage> messages = messageRepository
                .findByRoomIdOrderByTimestampAsc(roomId);
        return ResponseEntity.ok(messages);
    }
}