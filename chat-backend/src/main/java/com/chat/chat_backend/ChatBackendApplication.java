package com.chat.chat_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.chat"})
public class ChatBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatBackendApplication.class, args);
    }
}