package com.chat.chat_backend.repository;

import com.chat.chat_backend.model.JoinRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends MongoRepository<JoinRequest, String> {
    List<JoinRequest> findByRoomIdAndStatus(String roomId, JoinRequest.Status status);
    Optional<JoinRequest> findByRoomIdAndUsernameAndStatus(String roomId, String username, JoinRequest.Status status);
    List<JoinRequest> findByUsername(String username);
    long countByRoomIdAndUsernameAndRequestedAtAfter(String roomId, String username, java.time.LocalDateTime after);
}