package com.smartretail.route;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The weekdays a route is delivered, stored as {MON,THU}. */
public final class DeliveryDays {

    private final Set<DayOfWeek> days;

    private DeliveryDays(Set<DayOfWeek> days) {
        this.days = days.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(days);
    }

    public static DeliveryDays of(Collection<DayOfWeek> days) {
        return new DeliveryDays(days == null ? Set.of() : Set.copyOf(days));
    }

    /** Reads stored codes such as MON or THU; anything else is rejected. */
    public static DeliveryDays fromCodes(String[] codes) {
        EnumSet<DayOfWeek> parsed = EnumSet.noneOf(DayOfWeek.class);
        if (codes != null) {
            for (String code : codes) {
                parsed.add(dayFor(code));
            }
        }
        return new DeliveryDays(parsed);
    }

    public String[] toCodes() {
        return days.stream().map(DeliveryDays::codeFor).toArray(String[]::new);
    }

    public List<DayOfWeek> asList() {
        return List.copyOf(days);
    }

    public boolean isEmpty() {
        return days.isEmpty();
    }

    /**
     * The first delivery day after {@code orderDate}: orders placed today go on the next run, never
     * today's van, which is already loaded. With no days set, the next day.
     */
    public LocalDate nextDeliveryAfter(LocalDate orderDate) {
        LocalDate date = orderDate.plusDays(1);
        if (days.isEmpty()) {
            return date;
        }
        while (!days.contains(date.getDayOfWeek())) {
            date = date.plusDays(1);
        }
        return date;
    }

    private static String codeFor(DayOfWeek day) {
        return day.name().substring(0, 3);
    }

    private static DayOfWeek dayFor(String code) {
        String upper = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(DayOfWeek.values())
                .filter(day -> codeFor(day).equals(upper))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown delivery day: " + code));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeliveryDays that && days.equals(that.days);
    }

    @Override
    public int hashCode() {
        return days.hashCode();
    }
}
