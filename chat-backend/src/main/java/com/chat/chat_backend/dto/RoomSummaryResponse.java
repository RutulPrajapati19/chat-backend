package com.chat.chat_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomSummaryResponse {
    private String id;
    private String name;
    private String createdBy;
    private boolean isAdmin;
    private String memberStatus;
    private int memberCount;
}