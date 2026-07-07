package com.chat.chat_backend.controller;

import com.chat.chat_backend.model.User;
import com.chat.chat_backend.repository.UserRepository;
import com.chat.chat_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(buildProfile(user));
    }

    @PostMapping("/change-username")
    public ResponseEntity<?> changeUsername(@RequestBody Map<String, String> body, Principal principal) {
        String newUsername = body.getOrDefault("username", "").trim();
        if (newUsername.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Username cannot be empty"));
        if (newUsername.length() < 3)
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be at least 3 characters"));

        User user = getUser(principal);
        if (user.getUsername().equals(newUsername))
            return ResponseEntity.badRequest().body(Map.of("error", "That is already your username"));
        if (userRepository.existsByUsername(newUsername))
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));

        user.setUsername(newUsername);
        userRepository.save(user);
        String newToken = jwtService.generateToken(newUsername);
        return ResponseEntity.ok(Map.of("token", newToken, "username", newUsername));
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestBody Map<String, String> body, Principal principal) {
        String newEmail = body.getOrDefault("email", "").trim().toLowerCase();
        if (newEmail.isEmpty() || !newEmail.contains("@"))
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid email address"));

        User user = getUser(principal);
        if (user.getEmail().equals(newEmail))
            return ResponseEntity.badRequest().body(Map.of("error", "That is already your email"));
        if (userRepository.findByEmail(newEmail).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));

        user.setEmail(newEmail);
        userRepository.save(user);
        return ResponseEntity.ok(buildProfile(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, Principal principal) {
        String current = body.getOrDefault("currentPassword", "");
        String next = body.getOrDefault("newPassword", "");

        if (current.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Enter your current password"));
        if (next.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters"));
        if (current.equals(next))
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be different"));

        User user = getUser(principal);
        if (!passwordEncoder.matches(current, user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));

        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private User getUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> buildProfile(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "status", user.getStatus() != null ? user.getStatus() : "OFFLINE",
                "createdAt", user.getCreatedAt().toString()
        );
    }
}
