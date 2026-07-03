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
    private String passwordHash;
    private String createdBy;
    private List<String> approvedMemberIds = new ArrayList<>();
    private List<String> bannedUserIds = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isAdmin(String username) {
        return createdBy != null && createdBy.equals(username);
    }

    public boolean isApprovedMember(String username) {
        return isAdmin(username) || approvedMemberIds.contains(username);
    }

    public boolean isBanned(String username) {
        return bannedUserIds != null && bannedUserIds.contains(username);
    }
}