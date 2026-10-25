package com.smartretail.security;

import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

    private static final int MIN_KEY_BYTES = 32;

    /** Decodes the Base64 secret into an HS256 key, failing fast if it is missing or too short. */
    public SecretKey signingKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) must be set");
        }
        byte[] bytes = Base64.getDecoder().decode(secret.trim());
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("app.jwt.secret must decode to at least " + MIN_KEY_BYTES + " bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
