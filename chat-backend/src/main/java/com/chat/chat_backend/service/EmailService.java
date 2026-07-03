package com.chat.chat_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    public void sendJoinRequestToAdmin(String adminEmail, String adminUsername,
                                       String requesterUsername, String roomName) {
        String subject = "New join request: " + roomName;
        String html = "<div style='font-family:sans-serif;padding:24px;max-width:480px'>"
                + "<h2 style='color:#0f172a'>New Join Request 🔔</h2>"
                + "<p>Hi <strong>" + adminUsername + "</strong>,</p>"
                + "<p><strong>" + requesterUsername + "</strong> wants to join your room <strong>" + roomName + "</strong>.</p>"
                + "<p>Open ChatApp to approve or decline.</p>"
                + "<a href='https://chat-frontend-angular.vercel.app/rooms' "
                + "style='display:inline-block;margin-top:16px;padding:10px 20px;background:#6366f1;color:#fff;"
                + "border-radius:8px;text-decoration:none;font-weight:600'>Review Request →</a>"
                + "</div>";
        send(adminEmail, subject, html);
    }

    public void sendApprovalToUser(String userEmail, String username, String roomName) {
        String subject = "You're approved to join: " + roomName;
        String html = "<div style='font-family:sans-serif;padding:24px;max-width:480px'>"
                + "<h2 style='color:#0f172a'>Request Approved ✅</h2>"
                + "<p>Hi <strong>" + username + "</strong>,</p>"
                + "<p>Your request to join <strong>" + roomName + "</strong> was approved.</p>"
                + "<a href='https://chat-frontend-angular.vercel.app/rooms' "
                + "style='display:inline-block;margin-top:16px;padding:10px 20px;background:#6366f1;color:#fff;"
                + "border-radius:8px;text-decoration:none;font-weight:600'>Open ChatApp →</a>"
                + "</div>";
        send(userEmail, subject, html);
    }

    public void sendDeclineToUser(String userEmail, String username, String roomName) {
        String subject = "Update on your request for: " + roomName;
        String html = "<div style='font-family:sans-serif;padding:24px;max-width:480px'>"
                + "<h2 style='color:#0f172a'>Request Update</h2>"
                + "<p>Hi <strong>" + username + "</strong>,</p>"
                + "<p>Your request to join <strong>" + roomName + "</strong> was not approved.</p>"
                + "<a href='https://chat-frontend-angular.vercel.app/rooms' "
                + "style='display:inline-block;margin-top:16px;padding:10px 20px;background:#6366f1;color:#fff;"
                + "border-radius:8px;text-decoration:none;font-weight:600'>Open ChatApp →</a>"
                + "</div>";
        send(userEmail, subject, html);
    }

    private void send(String toEmail, String subject, String html) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", fromEmail, "name", fromName),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", html
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {}", toEmail);
            } else {
                log.warn("Email failed: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Email error to {}: {}", toEmail, e.getMessage());
        }
    }
}
