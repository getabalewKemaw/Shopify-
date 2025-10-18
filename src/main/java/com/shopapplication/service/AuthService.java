package com.shopapplication.service;

import com.shopapplication.dto.AuthRequest;
import com.shopapplication.dto.RegisterRequest;
import com.shopapplication.dto.ForgotPasswordRequest;
import com.shopapplication.dto.ResetPasswordRequest;
import com.shopapplication.models.Role;
import com.shopapplication.models.User;
import com.shopapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Builder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public String register(RegisterRequest request) {
        // Validate request
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        return jwtService.generateToken(user.getEmail());
    }

    public String login(AuthRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return jwtService.generateToken(request.getEmail());
    }

    // In-memory storage for reset tokens (in production, use Redis or database)
    private final Map<String, String> resetTokens = new HashMap<>();

    public String forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));
        
        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        resetTokens.put(request.getEmail(), resetToken);
        
        // In production, send this token via email
        // For now, we'll return it in the response
        return resetToken;
    }

    public void resetPassword(ResetPasswordRequest request) {
        // Validate reset token
        String storedToken = resetTokens.get(request.getEmail());
        if (storedToken == null || !storedToken.equals(request.getResetToken())) {
            throw new RuntimeException("Invalid or expired reset token");
        }
        
        // Find user and update password
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("New password cannot be empty");
        }
        
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        
        // Remove used token
        resetTokens.remove(request.getEmail());
    }
}
