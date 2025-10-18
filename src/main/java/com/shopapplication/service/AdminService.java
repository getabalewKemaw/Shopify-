package com.shopapplication.service;

import com.shopapplication.dto.AuthRequest;
import com.shopapplication.dto.RegisterRequest;
import com.shopapplication.models.Admin;
import com.shopapplication.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public String registerAdmin(RegisterRequest request) {
        // Validate request
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        
        // Check if admin already exists
        if (adminRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Admin with this email already exists");
        }
        
        Admin admin = Admin.builder()
                .email(request.getEmail())
                .name(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        
        adminRepository.save(admin);
        return jwtService.generateToken(admin.getEmail());
    }

    public String loginAdmin(AuthRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return jwtService.generateToken(request.getEmail());
    }
}
