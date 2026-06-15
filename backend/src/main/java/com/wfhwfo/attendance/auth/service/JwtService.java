package com.wfhwfo.attendance.auth.service;

import com.wfhwfo.attendance.common.enums.Role;
import com.wfhwfo.attendance.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(principal.getEmail())
                .claim("employeeId", principal.getEmployeeId())
                .claim("email", principal.getEmail())
                .claim("name", principal.getName())
                .claim("role", principal.getRole().name())
                .claim("teamId", principal.getTeamId())
                .claim("managerId", principal.getManagerId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public UserPrincipal parseToken(String token) {
        Claims claims = parseClaims(token);
        return UserPrincipal.builder()
                .employeeId(claims.get("employeeId", Long.class))
                .email(claims.get("email", String.class))
                .name(claims.get("name", String.class))
                .role(Role.valueOf(claims.get("role", String.class)))
                .teamId(claims.get("teamId", Long.class))
                .managerId(claims.get("managerId", Long.class))
                .build();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
