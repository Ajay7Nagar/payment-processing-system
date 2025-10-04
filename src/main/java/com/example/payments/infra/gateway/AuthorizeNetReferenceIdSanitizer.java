package com.example.payments.infra.gateway;

import java.util.regex.Pattern;

/**
 * Utility to normalise {@code refId}/invoice identifiers going to Authorize.Net.
 *
 * <p>The Authorize.Net API restricts {@code refId} values to a maximum of 20 characters and
 * accepts only textual data. Internally we use UUIDs which exceed this limit, so this class trims
 * and strips unsupported characters while guaranteeing a valid fallback identifier.</p>
 */
public final class AuthorizeNetReferenceIdSanitizer {

    private static final int MAX_LENGTH = 20;
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9]+");

    private AuthorizeNetReferenceIdSanitizer() {
    }

    public static String resolve(String candidate) {
        String sanitised = sanitise(candidate);
        return sanitised != null ? sanitised : fallback();
    }

    private static String sanitise(String candidate) {
        if (candidate == null) {
            return null;
        }

        String stripped = candidate.replaceAll("[^A-Za-z0-9]", "");
        if (stripped.isBlank()) {
            return null;
        }

        String trimmed = stripped.length() > MAX_LENGTH ? stripped.substring(0, MAX_LENGTH) : stripped;
        return ALLOWED.matcher(trimmed).matches() ? trimmed : null;
    }

    private static String fallback() {
        String base = "REF" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        return base.length() > MAX_LENGTH ? base.substring(0, MAX_LENGTH) : base;
    }
}

