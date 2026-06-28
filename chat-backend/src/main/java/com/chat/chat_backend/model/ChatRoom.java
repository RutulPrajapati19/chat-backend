package com.chat.chat_backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "rooms")
public class ChatRoom {
    @Id
    private String id;
    private String name;
    private String createdBy;
    private List<String> memberIds = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
}