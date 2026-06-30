package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.ChatMessage;
import com.chat.chat_backend.repository.ChatMessageRepository;
import com.chat.chat_backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

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

        if (principal == null) {
            throw new SecurityException("Not authenticated");
        }

        String username = principal.getName();

        if (!roomService.canSendMessage(roomId, username)) {
            throw new SecurityException("You are not a member of this room");
        }

        String cleanContent = roomService.validateAndCleanMessage(message.getContent());

        message.setRoomId(roomId);
        message.setSenderUsername(username);
        message.setContent(cleanContent);
        message.setTimestamp(java.time.LocalDateTime.now());
        message.setType(ChatMessage.MessageType.CHAT);

        return messageRepository.save(message);
    }
}