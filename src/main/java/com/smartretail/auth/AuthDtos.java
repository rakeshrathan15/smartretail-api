package com.smartretail.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Request and response bodies for /api/auth and /api/me. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** At least one of email or phone is required; the service enforces that. */
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Email @Size(max = 254) String email,
            @Size(max = 20) String phone,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    /** {@code identifier} is an email address or a mobile number. */
    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record UserSummary(
            UUID id,
            String email,
            String phone,
            String fullName,
            List<String> roles,
            boolean shopProfileComplete) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserSummary user) {
    }
}
