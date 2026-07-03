package com.chat.chat_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChatBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatBackendApplication.class, args);
    }
}