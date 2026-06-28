package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository roomRepository;

    @GetMapping
    public ResponseEntity<List<ChatRoom>> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping
    public ResponseEntity<ChatRoom> createRoom(@RequestBody Map<String, String> body,
                                               Principal principal) {
        ChatRoom room = new ChatRoom();
        room.setName(body.get("name"));
        room.setCreatedBy(principal.getName());
        return ResponseEntity.ok(roomRepository.save(room));
    }
}