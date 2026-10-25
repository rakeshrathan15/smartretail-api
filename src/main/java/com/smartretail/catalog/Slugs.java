package com.smartretail.catalog;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** URL-safe identifiers such as "thums-up-750ml-pet". */
final class Slugs {

    private Slugs() {
    }

    static String of(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "item" : slug;
    }

    /** Returns the slug for {@code text}, adding -2, -3 … until {@code taken} says it is free. */
    static String unique(String text, Predicate<String> taken) {
        String base = of(text);
        String candidate = base;
        for (int n = 2; taken.test(candidate); n++) {
            candidate = base + "-" + n;
        }
        return candidate;
    }
}
