package com.vanthucac.auth.controller;

import com.vanthucac.auth.dto.LoginRequest;
import com.vanthucac.auth.dto.LogoutRequest;
import com.vanthucac.auth.dto.RefreshRequest;
import com.vanthucac.auth.dto.RegisterRequest;
import com.vanthucac.auth.dto.TokenResponse;
import com.vanthucac.auth.dto.UserProfileResponse;
import com.vanthucac.auth.service.AuthService;
import com.vanthucac.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        var profile = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", profile));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        var deviceInfo = httpRequest.getHeader("User-Agent");
        var tokens = authService.login(request, deviceInfo);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        var tokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        authService.logout(request, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        authService.logoutAll(jwt);
        return ResponseEntity.ok(ApiResponse.ok("All sessions logged out successfully"));
    }
}