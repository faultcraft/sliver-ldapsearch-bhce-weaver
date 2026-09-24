package com.faultcraft.weaver.ad.model.pki;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

public final class FiletimeSpan {

    private static final long SECONDS_PER_YEAR = 31536000L;
    private static final long SECONDS_PER_MONTH = 2592000L;
    private static final long SECONDS_PER_WEEK = 604800L;
    private static final long SECONDS_PER_DAY = 86400L;
    private static final long SECONDS_PER_HOUR = 3600L;

    private FiletimeSpan() {}

    public static long toSeconds(byte[] filetimeBytes) {
        if (filetimeBytes == null || filetimeBytes.length < 8) {
            return 0;
        }
        ByteBuffer buf = ByteBuffer.wrap(filetimeBytes, 0, 8);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        long span = buf.getLong();
        span = (long) (span * -0.0000001);
        return span;
    }

    public static long toSecondsFromBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return 0;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64.strip());
            return toSeconds(bytes);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    public static String toDisplayString(long seconds) {
        if (seconds <= 0) {
            return "";
        }
        if (seconds % SECONDS_PER_YEAR == 0 && seconds / SECONDS_PER_YEAR >= 1) {
            long years = seconds / SECONDS_PER_YEAR;
            return years == 1 ? "1 year" : years + " years";
        }
        if (seconds % SECONDS_PER_MONTH == 0 && seconds / SECONDS_PER_MONTH >= 1) {
            long months = seconds / SECONDS_PER_MONTH;
            return months == 1 ? "1 month" : months + " months";
        }
        if (seconds % SECONDS_PER_WEEK == 0 && seconds / SECONDS_PER_WEEK >= 1) {
            long weeks = seconds / SECONDS_PER_WEEK;
            return weeks == 1 ? "1 week" : weeks + " weeks";
        }
        if (seconds % SECONDS_PER_DAY == 0 && seconds / SECONDS_PER_DAY >= 1) {
            long days = seconds / SECONDS_PER_DAY;
            return days == 1 ? "1 day" : days + " days";
        }
        if (seconds % SECONDS_PER_HOUR == 0 && seconds / SECONDS_PER_HOUR >= 1) {
            long hours = seconds / SECONDS_PER_HOUR;
            return hours == 1 ? "1 hour" : hours + " hours";
        }
        return "";
    }
}
