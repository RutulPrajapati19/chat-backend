package com.chat.chat_backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "join_requests")
public class JoinRequest {
    @Id
    private String id;

    private String roomId;
    private String roomName;
    private String username;
    private Status status = Status.PENDING;
    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;
    private String resolvedBy;

    public enum Status { PENDING, APPROVED, DECLINED }
}