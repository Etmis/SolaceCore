package com.etmisthefox.solacecore.utils;

import java.util.concurrent.TimeUnit;

public final class TimeUtil {

    public static long parseDuration(String input) {
        try {
            char unit = input.charAt(input.length() - 1);
            long value = Long.parseLong(input.substring(0, input.length() - 1));

            return switch (unit) {
                case 's' -> TimeUnit.SECONDS.toSeconds(value);
                case 'm' -> TimeUnit.MINUTES.toSeconds(value);
                case 'h' -> TimeUnit.HOURS.toSeconds(value);
                case 'd' -> TimeUnit.DAYS.toSeconds(value);
                default -> -1;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String formatDuration(long seconds) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long remainingSeconds = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (remainingSeconds > 0) sb.append(remainingSeconds).append("s");

        return sb.toString().trim();
    }
}
