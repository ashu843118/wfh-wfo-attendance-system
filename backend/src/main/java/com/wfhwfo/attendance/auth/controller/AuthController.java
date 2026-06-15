package com.wfhwfo.attendance.auth.controller;

import com.wfhwfo.attendance.auth.dto.LoginRequest;
import com.wfhwfo.attendance.auth.dto.LoginResponse;
import com.wfhwfo.attendance.auth.dto.UserProfileResponse;
import com.wfhwfo.attendance.auth.service.AuthService;
import com.wfhwfo.attendance.common.dto.ApiResponse;
import com.wfhwfo.attendance.config.OpenApiResponseDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Demo JWT authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with demo user credentials")
    @OpenApiResponseDocs.LoginResponses
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    @SecurityRequirement(name = "bearerAuth")
    @OpenApiResponseDocs.StandardApiResponses
    public ResponseEntity<ApiResponse<UserProfileResponse>> me() {
        UserProfileResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("User profile fetched", response));
    }
}
