package com.chat.chat_backend.repository;

import com.chat.chat_backend.model.JoinRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends MongoRepository<JoinRequest, String> {
    List<JoinRequest> findByRoomIdAndStatus(String roomId, JoinRequest.Status status);
    Optional<JoinRequest> findByRoomIdAndUsernameAndStatus(String roomId, String username, JoinRequest.Status status);
    List<JoinRequest> findByUsernameOrderByRequestedAtDesc(String username);
    long countByRoomIdAndUsernameAndRequestedAtAfter(String roomId, String username, LocalDateTime after);
}