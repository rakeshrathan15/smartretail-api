package com.smartretail.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** At least one of email or phone is required; the service enforces that. */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Email @Size(max = 254) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
