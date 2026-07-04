package com.chat.chat_backend.controller;

import com.chat.chat_backend.service.RoomService;
import com.chat.chat_backend.repository.JoinRequestRepository;
import com.chat.chat_backend.model.JoinRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailApprovalController {

    private final RoomService roomService;
    private final JoinRequestRepository joinRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/approve/{requestId}/{adminUsername}")
    public RedirectView approveFromEmail(
            @PathVariable String requestId,
            @PathVariable String adminUsername) {
        try {
            JoinRequest req = joinRequestRepository.findById(requestId).orElse(null);
            if (req == null || req.getStatus() != JoinRequest.Status.PENDING) {
                return new RedirectView(
                        "https://chat-frontend-angular.vercel.app/rooms?msg=already_resolved");
            }

            roomService.approveRequest(req.getRoomId(), requestId, adminUsername);

            // Notify user via WebSocket
            messagingTemplate.convertAndSendToUser(
                    req.getUsername(),
                    "/queue/notifications",
                    java.util.Map.of(
                            "type", "REQUEST_APPROVED",
                            "roomId", req.getRoomId(),
                            "roomName", req.getRoomName()
                    )
            );

            return new RedirectView(
                    "https://chat-frontend-angular.vercel.app/rooms?msg=approved");
        } catch (Exception e) {
            return new RedirectView(
                    "https://chat-frontend-angular.vercel.app/rooms?msg=error");
        }
    }

    @GetMapping("/decline/{requestId}/{adminUsername}")
    public RedirectView declineFromEmail(
            @PathVariable String requestId,
            @PathVariable String adminUsername) {
        try {
            JoinRequest req = joinRequestRepository.findById(requestId).orElse(null);
            if (req == null || req.getStatus() != JoinRequest.Status.PENDING) {
                return new RedirectView(
                        "https://chat-frontend-angular.vercel.app/rooms?msg=already_resolved");
            }

            roomService.declineRequest(req.getRoomId(), requestId, adminUsername);

            // Notify user via WebSocket
            messagingTemplate.convertAndSendToUser(
                    req.getUsername(),
                    "/queue/notifications",
                    java.util.Map.of(
                            "type", "REQUEST_DECLINED",
                            "roomId", req.getRoomId(),
                            "roomName", req.getRoomName()
                    )
            );

            return new RedirectView(
                    "https://chat-frontend-angular.vercel.app/rooms?msg=declined");
        } catch (Exception e) {
            return new RedirectView(
                    "https://chat-frontend-angular.vercel.app/rooms?msg=error");
        }
    }
}