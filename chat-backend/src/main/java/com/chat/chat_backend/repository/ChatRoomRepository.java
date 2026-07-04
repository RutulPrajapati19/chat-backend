package com.chat.chat_backend.repository;

import com.chat.chat_backend.model.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    List<ChatRoom> findAllByOrderByCreatedAtDesc();
    Optional<ChatRoom> findByName(String name);
}