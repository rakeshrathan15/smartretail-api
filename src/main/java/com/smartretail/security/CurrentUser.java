package com.smartretail.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Helpers for reading the authenticated user from a validated access token. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
