package com.chat.chat_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    @Async("taskExecutor")
    public void sendRegistrationConfirmation(String toEmail, String username) {
        String subject = "Welcome to ChatApp! 🎉";
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8fafc;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;margin:0 0 16px;">💬 ChatApp</h1>
                <h2 style="color:#0f172a;margin:0 0 12px;">Welcome, %s! 👋</h2>
                <p style="color:#64748b;line-height:1.6;">Your account has been successfully created. You can now log in and start chatting in real-time!</p>
                <div style="background:#f0f4ff;border-radius:10px;padding:16px;margin:20px 0;">
                  <p style="color:#4338ca;margin:0;line-height:1.8;">✅ Account created successfully<br/>✅ Real-time messaging enabled<br/>✅ Create and join rooms instantly</p>
                </div>
                <p style="color:#94a3b8;font-size:13px;">If you did not create this account, please ignore this email.</p>
              </div>
            </div>
            """.formatted(username);
        sendEmail(toEmail, subject, body);
    }

    @Async("taskExecutor")
    public void sendPasswordResetOtp(String toEmail, String username, String otp) {
        String subject = "ChatApp — Password Reset OTP";
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8fafc;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;margin:0 0 16px;">💬 ChatApp</h1>
                <h2 style="color:#0f172a;margin:0 0 12px;">Password Reset Request</h2>
                <p style="color:#64748b;">Hi %s, use the OTP below to reset your password. It expires in <strong>10 minutes</strong>.</p>
                <div style="text-align:center;margin:28px 0;">
                  <div style="display:inline-block;background:#6366f1;color:#fff;font-size:32px;font-weight:700;letter-spacing:10px;padding:16px 32px;border-radius:12px;">%s</div>
                </div>
                <p style="color:#94a3b8;font-size:13px;">If you did not request this, please ignore this email.</p>
              </div>
            </div>
            """.formatted(username, otp);
        sendEmail(toEmail, subject, body);
    }

    @Async("taskExecutor")
    public void sendJoinRequestNotificationToAdmin(String adminEmail, String adminUsername,
                                                   String requesterUsername, String roomName) {
        String subject = "🔔 New Join Request — #" + roomName;
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8fafc;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;margin:0 0 16px;">💬 ChatApp</h1>
                <h2 style="color:#0f172a;margin:0 0 12px;">New Join Request</h2>
                <p style="color:#64748b;">Hi <strong>%s</strong>,</p>
                <p style="color:#64748b;"><strong style="color:#0f172a;">%s</strong> has requested to join your room <strong style="color:#6366f1;">#%s</strong>.</p>
                <p style="color:#64748b;font-size:13px;margin-top:16px;">Log in to ChatApp to approve or reject this request.</p>
              </div>
            </div>
            """.formatted(adminUsername, requesterUsername, roomName);
        sendEmail(adminEmail, subject, body);
    }

    @Async("taskExecutor")
    public void sendApprovalNotificationToUser(String toEmail, String username, String roomName) {
        String subject = "✅ Join Request Approved — #" + roomName;
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8fafc;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;margin:0 0 16px;">💬 ChatApp</h1>
                <h2 style="color:#0f172a;margin:0 0 12px;">Request Approved! 🎉</h2>
                <p style="color:#64748b;">Hi <strong>%s</strong>, your request to join <strong style="color:#6366f1;">#%s</strong> has been <strong style="color:#16a34a;">approved</strong>!</p>
                <p style="color:#64748b;">You can now enter the room and start chatting.</p>
              </div>
            </div>
            """.formatted(username, roomName);
        sendEmail(toEmail, subject, body);
    }

    @Async("taskExecutor")
    public void sendDeclineNotificationToUser(String toEmail, String username, String roomName) {
        String subject = "❌ Join Request Declined — #" + roomName;
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#f8fafc;">
              <div style="background:#fff;border-radius:16px;padding:32px;border:1px solid #e2e8f0;">
                <h1 style="color:#6366f1;margin:0 0 16px;">💬 ChatApp</h1>
                <h2 style="color:#0f172a;margin:0 0 12px;">Request Declined</h2>
                <p style="color:#64748b;">Hi <strong>%s</strong>, your request to join <strong style="color:#6366f1;">#%s</strong> was <strong style="color:#dc2626;">not approved</strong> by the room admin.</p>
              </div>
            </div>
            """.formatted(username, roomName);
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}