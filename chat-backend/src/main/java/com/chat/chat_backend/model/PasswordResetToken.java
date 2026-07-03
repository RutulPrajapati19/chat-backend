package com.chat.chat_backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;
    private String email;
    private String otp;
    private LocalDateTime expiresAt;
    private boolean used = false;
}