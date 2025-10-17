package com.shopapplication.service;

import com.shopapplication.dto.AuthRequest;
import com.shopapplication.dto.RegisterRequest;
import com.shopapplication.models.Role;
import com.shopapplication.models.User;
import com.shopapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Builder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public String register(RegisterRequest request) {
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
}
