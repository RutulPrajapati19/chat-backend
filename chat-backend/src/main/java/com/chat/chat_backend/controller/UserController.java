package com.chat.chat_backend.controller;

import com.chat.chat_backend.dto.UpdateProfileRequest;
import com.chat.chat_backend.dto.UserProfileResponse;
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
    public ResponseEntity<?> getMyProfile(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getStatus(), user.getCreatedAt().toString()));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateEmail(@RequestBody UpdateProfileRequest request,
                                         Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !request.getEmail().isBlank())
            user.setEmail(request.getEmail().trim().toLowerCase());

        userRepository.save(user);
        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getStatus(), user.getCreatedAt().toString()));
    }

    @PostMapping("/change-username")
    public ResponseEntity<?> changeUsername(@RequestBody Map<String, String> body,
                                            Principal principal) {
        String newUsername = body.get("username");
        if (newUsername == null || newUsername.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        if (newUsername.trim().length() < 3)
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be at least 3 characters"));

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getUsername().equals(newUsername.trim()) &&
                userRepository.existsByUsername(newUsername.trim()))
            return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));

        user.setUsername(newUsername.trim());
        userRepository.save(user);

        String newToken = jwtService.generateToken(newUsername.trim());
        return ResponseEntity.ok(Map.of("token", newToken, "username", newUsername.trim()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body,
                                            Principal principal) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || newPassword == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Both passwords required"));
        if (newPassword.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}