package com.smartretail.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartretail.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptsTest {

    private static final Instant START = Instant.parse("2026-10-30T00:00:00Z");

    private final LoginAttempts attempts = new LoginAttempts(Clock.fixed(START, ZoneOffset.UTC));

    private void fail(String identifier, int times) {
        for (int i = 0; i < times; i++) {
            attempts.recordFailure(identifier);
        }
    }

    @Test
    void blocksAnAccountAfterTooManyWrongPasswordsOnly() {
        fail("Ravi@Shop.com", LoginAttempts.MAX_FAILURES - 1);
        assertThatCode(() -> attempts.checkAllowed("ravi@shop.com")).doesNotThrowAnyException();

        attempts.recordFailure("ravi@shop.com");
        assertThatThrownBy(() -> attempts.checkAllowed(" RAVI@shop.com ")).isInstanceOf(ApiException.class)
                .hasMessageContaining("Too many wrong passwords");
        assertThatCode(() -> attempts.checkAllowed("other@shop.com")).doesNotThrowAnyException();
    }

    @Test
    void theBlockEndsWithItsWindowAndOnSuccess() {
        fail("9876543210", LoginAttempts.MAX_FAILURES);
        LoginAttempts later = new LoginAttempts(Clock.fixed(START.plus(LoginAttempts.WINDOW), ZoneOffset.UTC));
        assertThatCode(() -> later.checkAllowed("9876543210")).doesNotThrowAnyException();

        attempts.reset("9876543210");
        assertThatCode(() -> attempts.checkAllowed("9876543210")).doesNotThrowAnyException();
    }
}
