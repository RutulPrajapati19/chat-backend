package com.chat.chat_backend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class RoomSummaryResponse {
    private String id;
    private String name;
    private String createdBy;
    private boolean isAdmin;
    private String membershipStatus;
    private int memberCount;
}