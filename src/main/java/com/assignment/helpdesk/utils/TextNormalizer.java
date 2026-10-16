package com.assignment.helpdesk.utils;

import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String email(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String slug(String slug) {
        return slug == null ? null : slug.trim().toLowerCase(Locale.ROOT);
    }
}
