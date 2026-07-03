package com.chat.chat_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JoinRequestResponse {
    private String id;
    private String roomId;
    private String roomName;
    private String username;
    private String status;
    private String requestedAt;
}