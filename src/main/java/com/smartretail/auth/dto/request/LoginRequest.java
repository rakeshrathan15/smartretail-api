package com.smartretail.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** {@code identifier} is an email address or a mobile number. */
public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
}
