package com.shopapplication.service;

import com.shopapplication.models.Admin;
import com.shopapplication.models.User;
import com.shopapplication.repository.AdminRepository;
import com.shopapplication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    // This tells Spring Security how to load a user from the DB during login/auth
    // Checks both User and Admin tables
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // First check if it's a regular user
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return user.get();
        }
        
        // Then check if it's an admin
        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            return admin.get();
        }
        
        throw new UsernameNotFoundException("User or Admin not found with email: " + email);
    }
}
