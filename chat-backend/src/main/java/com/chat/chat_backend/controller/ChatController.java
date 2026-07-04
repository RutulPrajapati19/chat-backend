package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.ChatMessage;
import com.chat.chat_backend.model.ChatRoom;
import com.chat.chat_backend.repository.ChatMessageRepository;
import com.chat.chat_backend.repository.ChatRoomRepository;
import com.chat.chat_backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final RoomService roomService;

    @MessageMapping("/chat/{roomName}/send")
    @SendTo("/topic/room/{roomName}")
    public ChatMessage sendMessage(@DestinationVariable String roomName,
                                   @Payload ChatMessage message,
                                   Principal principal) {

        if (principal == null)
            throw new SecurityException("Not authenticated");

        String username = principal.getName();

        // Resolve room name to ID
        ChatRoom room = roomRepository.findByName(roomName)
                .orElseGet(() -> roomRepository.findById(roomName).orElse(null));

        if (room == null)
            throw new IllegalArgumentException("Room not found: " + roomName);

        if (!roomService.canAccessRoom(room.getId(), username))
            throw new SecurityException("Not a member of this room");

        String content = message.getContent();
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("Empty message");
        if (content.length() > 2000)
            content = content.substring(0, 2000);

        message.setRoomId(room.getId());
        message.setSenderUsername(username);
        message.setContent(content.trim());
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessage.MessageType.CHAT);

        return messageRepository.save(message);
    }
}