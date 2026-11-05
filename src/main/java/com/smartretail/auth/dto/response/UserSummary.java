package com.smartretail.auth.dto.response;

import java.util.List;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String phone,
        String fullName,
        List<String> roles,
        boolean shopProfileComplete) {
}
