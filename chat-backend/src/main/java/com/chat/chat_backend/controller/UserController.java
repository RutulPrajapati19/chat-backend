package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.User;
import com.chat.chat_backend.repository.UserRepository;
import com.chat.chat_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .map(user -> ResponseEntity.ok(Map.of(
                        "username", user.getUsername(),
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/change-username")
    public ResponseEntity<?> changeUsername(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String newUsername = body.get("username");
            if (newUsername == null || newUsername.trim().length() < 3)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username must be at least 3 characters"));

            newUsername = newUsername.trim();

            if (userRepository.existsByUsername(newUsername))
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username already taken"));

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setUsername(newUsername);
            userRepository.save(user);

            String newToken = jwtService.generateToken(newUsername);
            return ResponseEntity.ok(Map.of(
                    "username", newUsername,
                    "token", newToken
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String newEmail = body.get("email");
            if (newEmail == null || !newEmail.contains("@"))
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid email address"));

            newEmail = newEmail.trim().toLowerCase();

            if (userRepository.existsByEmail(newEmail))
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already in use"));

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setEmail(newEmail);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("email", newEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");

            if (currentPassword == null || newPassword == null)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "All fields required"));

            if (newPassword.length() < 6)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password must be at least 6 characters"));

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!passwordEncoder.matches(currentPassword, user.getPassword()))
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Current password is incorrect"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}