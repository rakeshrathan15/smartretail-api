package com.smartretail.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Rupee amounts in customer-facing messages, with Indian digit grouping: ₹1,00,000. */
public final class Money {

    private static final Locale INDIA = Locale.of("en", "IN");

    private Money() {
    }

    public static String format(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(INDIA);
        boolean whole = amount.stripTrailingZeros().scale() <= 0;
        format.setMinimumFractionDigits(whole ? 0 : 2);
        format.setMaximumFractionDigits(whole ? 0 : 2);
        return "₹" + format.format(amount.setScale(2, RoundingMode.HALF_UP));
    }
}
