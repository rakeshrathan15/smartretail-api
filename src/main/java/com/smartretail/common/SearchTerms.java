package com.smartretail.common;

import java.util.Locale;

/** Turns what someone typed into a safe LIKE pattern. */
public final class SearchTerms {

    private SearchTerms() {
    }

    /** "%term%" in lower case, or null when there is nothing to search for. */
    public static String containsPattern(String search) {
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        return term.isEmpty() ? null : "%" + term + "%";
    }
}
