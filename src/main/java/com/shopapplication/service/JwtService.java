package com.shopapplication.service;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {

    private final String secretKey;

    public JwtService(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String generateToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + 1000 * 60 * 60)) // 1 hour
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

  

      public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }


     public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

        private Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    
  

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
    // public boolean isTokenValid(String token){
    //     try{
    //         Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token);
    //         return true;
    //     }catch (ExpiredJwtException e){
    //         throw new RuntimeException("JWT Token has expired");
    //     } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
    //         throw new RuntimeException("Invalid JWT Token");
    //     }
    // }
}
