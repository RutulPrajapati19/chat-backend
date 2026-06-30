package com.chat.chat_backend.dto;

import lombok.Data;

@Data
public class CreateRoomRequest {
    private String name;
    private String password;
}