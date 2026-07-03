package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.ChatMessage;
import com.chat.chat_backend.repository.ChatMessageRepository;
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
    private final RoomService roomService;

    @MessageMapping("/chat/{roomId}/send")
    @SendTo("/topic/room/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable String roomId,
                                   @Payload ChatMessage message,
                                   Principal principal) {

        if (principal == null) throw new SecurityException("Not authenticated");

        String username = principal.getName();

        // ✅ Allow admin OR approved member
        if (!roomService.canAccessRoom(roomId, username) &&
                !roomService.getRoomAdmin(roomId).equals(username)) {
            throw new SecurityException("Not a member of this room");
        }

        String content = message.getContent();
        if (content == null || content.trim().isEmpty())
            throw new IllegalArgumentException("Empty message");
        if (content.length() > 2000)
            content = content.substring(0, 2000);

        message.setRoomId(roomId);
        message.setSenderUsername(username);
        message.setContent(content.trim());
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessage.MessageType.CHAT);

        return messageRepository.save(message);
    }
}