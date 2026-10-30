package com.smartretail.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** The business runs on India time: "today", delivery dates and date filters use it. */
@Component
public class BusinessCalendar {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final Clock clock;

    public BusinessCalendar() {
        this(Clock.system(ZONE));
    }

    BusinessCalendar(Clock clock) {
        this.clock = clock.withZone(ZONE);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
