package com.chat.chat_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
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

    @Async("taskExecutor")
    public void sendRegistrationConfirmation(String toEmail, String username) {
        sendEmail(toEmail, "Welcome to ChatApp! 🎉", """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;">💬 ChatApp</h1>
                <h2>Welcome, %s! 👋</h2>
                <p style="color:#64748b;">Your account has been successfully created!</p>
                <div style="background:#f0f4ff;border-radius:10px;padding:16px;margin:20px 0;">
                  <p style="color:#4338ca;margin:0;">✅ Account created<br/>✅ Real-time messaging<br/>✅ Create and join rooms</p>
                </div>
              </div>
            </div>
            """.formatted(username));
    }

    @Async("taskExecutor")
    public void sendPasswordResetOtp(String toEmail, String username, String otp) {
        sendEmail(toEmail, "ChatApp — Password Reset OTP", """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;">💬 ChatApp</h1>
                <h2>Password Reset</h2>
                <p style="color:#64748b;">Hi %s, your OTP expires in 10 minutes.</p>
                <div style="text-align:center;margin:28px 0;">
                  <div style="display:inline-block;background:#6366f1;color:#fff;font-size:32px;font-weight:700;letter-spacing:10px;padding:16px 32px;border-radius:12px;">%s</div>
                </div>
              </div>
            </div>
            """.formatted(username, otp));
    }

    @Async("taskExecutor")
    public void sendJoinRequestNotificationToAdmin(String adminEmail, String adminUsername,
                                                   String requesterUsername, String roomName,
                                                   String requestId) {
        String approveUrl = "https://chat-backend-vdje.onrender.com/api/email/approve/"
                + requestId + "/" + adminUsername;
        String declineUrl = "https://chat-backend-vdje.onrender.com/api/email/decline/"
                + requestId + "/" + adminUsername;

        sendEmail(adminEmail, "🔔 New Join Request — #" + roomName, """
        <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
          <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
            <h1 style="color:#6366f1;">💬 ChatApp</h1>
            <h2>New Join Request</h2>
            <p style="color:#64748b;">Hi <strong>%s</strong>,</p>
            <p style="color:#64748b;"><strong>%s</strong> wants to join <strong style="color:#6366f1;">#%s</strong>.</p>
            <div style="display:flex;gap:12px;margin-top:24px;">
              <a href="%s" style="flex:1;display:inline-block;padding:12px 24px;background:#22c55e;color:#fff;text-decoration:none;border-radius:10px;font-weight:700;font-size:15px;text-align:center;">✅ Approve</a>
              <a href="%s" style="flex:1;display:inline-block;padding:12px 24px;background:#ef4444;color:#fff;text-decoration:none;border-radius:10px;font-weight:700;font-size:15px;text-align:center;">❌ Decline</a>
            </div>
          </div>
        </div>
        """.formatted(adminUsername, requesterUsername, roomName, approveUrl, declineUrl));
    }

    @Async("taskExecutor")
    public void sendApprovalNotificationToUser(String toEmail, String username, String roomName) {
        sendEmail(toEmail, "✅ Approved — #" + roomName, """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;">💬 ChatApp</h1>
                <h2>Request Approved! 🎉</h2>
                <p style="color:#64748b;">Hi <strong>%s</strong>, you can now enter <strong style="color:#6366f1;">#%s</strong>!</p>
              </div>
            </div>
            """.formatted(username, roomName));
    }

    @Async("taskExecutor")
    public void sendDeclineNotificationToUser(String toEmail, String username, String roomName) {
        sendEmail(toEmail, "❌ Request Declined — #" + roomName, """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;">💬 ChatApp</h1>
                <h2>Request Declined</h2>
                <p style="color:#64748b;">Hi <strong>%s</strong>, your request to join <strong style="color:#6366f1;">#%s</strong> was declined.</p>
              </div>
            </div>
            """.formatted(username, roomName));
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            headers.set("accept", "application/json");

            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlBody
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    BREVO_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {}: {}", toEmail, subject);
            } else {
                log.error("Email failed to {}: {} — {}", toEmail, subject, response.getBody());
            }
        } catch (Exception e) {
            log.error("Email error to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void sendRoomCreatedConfirmation(String toEmail, String username, String roomName) {
        sendEmail(toEmail, "🏠 Room Created — #" + roomName, """
        <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
          <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
            <h1 style="color:#6366f1;">💬 ChatApp</h1>
            <h2>Room Created! 🎉</h2>
            <p style="color:#64748b;">Hi <strong>%s</strong>, your room <strong style="color:#6366f1;">#%s</strong> has been created successfully.</p>
            <p style="color:#64748b;">Share the room name and password with people you want to invite.</p>
          </div>
        </div>
        """.formatted(username, roomName));
    }
}