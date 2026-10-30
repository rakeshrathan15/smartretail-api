package com.smartretail.auth;

import com.smartretail.common.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Slows down password guessing: too many wrong passwords for one account blocks it for a while.
 * Kept in memory, so it counts per server and resets on restart; move it to a shared store
 * before running more than one server.
 */
@Component
public class LoginAttempts {

    static final int MAX_FAILURES = 10;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED = 100_000;

    private record Failures(int count, Instant windowEnd) {
    }

    private final Map<String, Failures> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttempts() {
        this(Clock.systemUTC());
    }

    LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String identifier) {
        Failures current = failures.get(key(identifier));
        if (current != null && current.count() >= MAX_FAILURES && clock.instant().isBefore(current.windowEnd())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong passwords. Please wait 15 minutes and try again.");
        }
    }

    public void recordFailure(String identifier) {
        Instant now = clock.instant();
        if (failures.size() >= MAX_TRACKED) {
            failures.values().removeIf(f -> !now.isBefore(f.windowEnd()));
        }
        failures.merge(key(identifier), new Failures(1, now.plus(WINDOW)),
                (old, first) -> now.isBefore(old.windowEnd()) ? new Failures(old.count() + 1, old.windowEnd()) : first);
    }

    public void reset(String identifier) {
        failures.remove(key(identifier));
    }

    private static String key(String identifier) {
        return identifier.trim().toLowerCase(Locale.ROOT);
    }
}
