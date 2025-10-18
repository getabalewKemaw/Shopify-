package com.shopapplication.controller;

import com.shopapplication.dto.AuthRequest;
import com.shopapplication.dto.RegisterRequest;
import com.shopapplication.dto.ForgotPasswordRequest;
import com.shopapplication.dto.ResetPasswordRequest;
import com.shopapplication.service.AuthService;
import com.shopapplication.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AdminService adminService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            String token = authService.register(request);
            return ResponseEntity.ok().body(Map.of("token", token, "message", "User registered successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            String token = authService.login(request);
            return ResponseEntity.ok().body(Map.of("token", token, "message", "User logged in successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            String resetToken = authService.forgotPassword(request);
            return ResponseEntity.ok().body(Map.of(
                "message", "Password reset token generated. In production, this would be sent via email.",
                "resetToken", resetToken
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok().body(Map.of("message", "Password reset successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@RequestBody RegisterRequest request) {
        try {
            String token = adminService.registerAdmin(request);
            return ResponseEntity.ok().body(Map.of("token", token, "message", "Admin registered successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest request) {
        try {
            String token = adminService.loginAdmin(request);
            return ResponseEntity.ok().body(Map.of("token", token, "message", "Admin logged in successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // In a stateless JWT system, logout is handled client-side by removing the token
        // Server-side logout would require token blacklisting (Redis) or token revocation
        return ResponseEntity.ok().body(Map.of(
            "message", "Logged out successfully. Please remove the token from client storage."
        ));
    }
}
