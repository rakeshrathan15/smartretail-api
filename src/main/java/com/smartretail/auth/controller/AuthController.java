package com.smartretail.auth.controller;

import com.smartretail.auth.dto.request.LoginRequest;
import com.smartretail.auth.dto.request.RefreshRequest;
import com.smartretail.auth.dto.request.RegisterRequest;
import com.smartretail.auth.dto.response.AuthResponse;
import com.smartretail.auth.dto.response.UserSummary;
import com.smartretail.auth.service.AuthService;
import com.smartretail.common.response.ApiResponse;
import com.smartretail.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/api/auth/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/api/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/api/me")
    public ApiResponse<UserSummary> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(authService.currentUser(CurrentUser.id(jwt)));
    }
}
