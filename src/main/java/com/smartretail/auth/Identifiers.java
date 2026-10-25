package com.smartretail.auth;

import com.smartretail.common.ApiException;
import java.util.Locale;

/** Normalizes login identifiers so "+91 98765-43210" and "9876543210" are the same account. */
public final class Identifiers {

    private Identifiers() {
    }

    public static boolean isEmail(String identifier) {
        return identifier.contains("@");
    }

    public static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Returns a 10-digit Indian mobile number, or throws 400 if the input is not one. */
    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (!digits.matches("[6-9]\\d{9}")) {
            throw ApiException.badRequest("Enter a valid 10-digit mobile number");
        }
        return digits;
    }
}
