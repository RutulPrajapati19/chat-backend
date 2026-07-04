package com.chat.chat_backend.controller;

import com.chat.chat_backend.repository.ChatMessageRepository;
import com.chat.chat_backend.repository.ChatRoomRepository;
import com.chat.chat_backend.repository.JoinRequestRepository;
import com.chat.chat_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCleanupController {

    private final ChatRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository messageRepository;
    private final JoinRequestRepository joinRequestRepository;

    // DELETE ALL DATA — call this once then delete this file
    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanup(@RequestParam String secret) {
        if (!"chatapp-cleanup-2024".equals(secret))
            return ResponseEntity.status(403).body(Map.of("error", "Wrong secret"));

        roomRepository.deleteAll();
        userRepository.deleteAll();
        messageRepository.deleteAll();
        joinRequestRepository.deleteAll();

        return ResponseEntity.ok(Map.of("message", "All data deleted successfully"));
    }
}