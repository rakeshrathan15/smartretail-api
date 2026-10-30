package com.smartretail.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeliveryDaysTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

    @Test
    void nextDeliveryIsTheNextRouteDayAfterTheOrder() {
        DeliveryDays monThu = DeliveryDays.fromCodes(new String[] {"MON", "THU"});
        assertThat(monThu.nextDeliveryAfter(MONDAY)).isEqualTo(MONDAY.plusDays(3));          // Thu
        assertThat(monThu.nextDeliveryAfter(MONDAY.plusDays(3))).isEqualTo(MONDAY.plusDays(7)); // next Mon
        assertThat(monThu.nextDeliveryAfter(MONDAY.minusDays(1))).isEqualTo(MONDAY);         // Sun → Mon
    }

    @Test
    void withNoDaysSetDeliveryIsTheNextDay() {
        assertThat(DeliveryDays.of(List.of()).nextDeliveryAfter(MONDAY)).isEqualTo(MONDAY.plusDays(1));
    }

    @Test
    void storesDaysInWeekOrderAndRejectsUnknownCodes() {
        DeliveryDays days = DeliveryDays.fromCodes(new String[] {"fri", "MON"});
        assertThat(days.toCodes()).containsExactly("MON", "FRI");
        assertThat(days.asList()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        assertThat(days).isEqualTo(DeliveryDays.of(List.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)));
        assertThatThrownBy(() -> DeliveryDays.fromCodes(new String[] {"MONDAY"}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
