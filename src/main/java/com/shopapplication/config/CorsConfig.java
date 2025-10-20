package com.shopapplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * CORS Configuration Bean
     * This allows the frontend (localhost:3000) to communicate with the backend (localhost:8091)
     * 
     * Why this is needed:
     * - Browsers block cross-origin requests by default (security feature)
     * - Frontend and backend are on different ports (3000 vs 8091)
     * - CORS policy allows specific origins to make requests
     * 
     * Preflight Requests:
     * - Browser sends OPTIONS request before actual request
     * - This checks if the server allows the cross-origin request
     * - Spring Security must allow OPTIONS requests to pass
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow requests from Next.js frontend
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        
        // Allow all HTTP methods (GET, POST, PUT, DELETE, OPTIONS, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow all headers (including Authorization, Content-Type, etc.)
        configuration.setAllowedHeaders(List.of("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Expose headers to the frontend (optional, but good for debugging)
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        // Cache preflight response for 1 hour (reduces OPTIONS requests)
        configuration.setMaxAge(3600L);
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
