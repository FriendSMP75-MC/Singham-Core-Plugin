package com.friendsmp.singhamcore.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationUtils {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);

    private DurationUtils() {
    }

    public static long parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return -1L;
        }
        String normalized = input.trim().toLowerCase();
        Matcher matcher = DURATION_PATTERN.matcher(normalized);
        long totalMillis = 0L;
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                return -1L;
            }
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            totalMillis += switch (unit) {
                case "d" -> Duration.ofDays(value).toMillis();
                case "h" -> Duration.ofHours(value).toMillis();
                case "m" -> Duration.ofMinutes(value).toMillis();
                case "s" -> Duration.ofSeconds(value).toMillis();
                default -> 0L;
            };
            lastEnd = matcher.end();
        }
        if (lastEnd != normalized.length() || totalMillis <= 0) {
            return -1L;
        }
        return totalMillis;
    }

    public static String formatDuration(long durationMillis) {
        if (durationMillis <= 0) {
            return "0s";
        }
        long seconds = durationMillis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m ");
        }
        if (seconds > 0 || builder.isEmpty()) {
            builder.append(seconds).append("s");
        }
        return builder.toString().trim();
    }

    public static String formatRemaining(Instant expiresAt) {
        if (expiresAt == null) {
            return "permanent";
        }
        long remaining = Duration.between(Instant.now(), expiresAt).toMillis();
        if (remaining <= 0) {
            return "expired";
        }
        return formatDuration(remaining);
    }
}
