package com.chat.chat_backend.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;
    private String currentPassword;
    private String newPassword;
}