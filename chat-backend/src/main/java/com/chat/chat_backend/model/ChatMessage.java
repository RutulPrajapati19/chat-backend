package com.chat.chat_backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "messages")
public class ChatMessage {
    @Id
    private String id;
    private String roomId;
    private String senderUsername;
    private String content;
    private LocalDateTime timestamp = LocalDateTime.now();
    private MessageType type = MessageType.CHAT;

    public enum MessageType { CHAT, JOIN, LEAVE, SYSTEM }
}