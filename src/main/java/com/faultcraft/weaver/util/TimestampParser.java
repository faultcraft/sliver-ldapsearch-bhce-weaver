package com.faultcraft.weaver.util;

/** Converts Active Directory timestamp formats to Unix epoch seconds. */
public final class TimestampParser {

    private static final long FILETIME_EPOCH_DIFF = 11644473600L;
    private static final long FILETIME_TICKS_PER_SECOND = 10_000_000L;
    private static final long NEVER_EXPIRES = 9223372036854775807L;

    private TimestampParser() {}

    /**
     * Converts a Windows FILETIME (100-nanosecond intervals since 1601-01-01) to Unix epoch seconds.
     *
     * <p>Returns 0 for the "never expires" sentinel value and for values that represent
     * "not set" (0 or negative).
     *
     * @param filetime the FILETIME value as a string (from LDAP attributes like lastLogon, pwdLastSet)
     * @return Unix epoch seconds, or 0 if the value is not set or never expires
     */
    public static long fromFiletime(String filetime) {
        if (filetime == null || filetime.isBlank()) {
            return 0;
        }
        try {
            long ticks = Long.parseLong(filetime.strip());
            if (ticks <= 0 || ticks == NEVER_EXPIRES) {
                return 0;
            }
            return (ticks / FILETIME_TICKS_PER_SECOND) - FILETIME_EPOCH_DIFF;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Converts an LDAP GeneralizedTime string to Unix epoch seconds.
     *
     * <p>Handles the common AD format: YYYYMMDDHHmmss.0Z
     *
     * @param generalizedTime the GeneralizedTime string (from LDAP attributes like whenCreated)
     * @return Unix epoch seconds, or 0 if the value cannot be parsed
     */
    public static long fromGeneralizedTime(String generalizedTime) {
        if (generalizedTime == null || generalizedTime.isBlank()) {
            return 0;
        }
        try {
            String s = generalizedTime.strip();
            if (s.length() < 14) {
                return 0;
            }
            int year = Integer.parseInt(s.substring(0, 4));
            int month = Integer.parseInt(s.substring(4, 6));
            int day = Integer.parseInt(s.substring(6, 8));
            int hour = Integer.parseInt(s.substring(8, 10));
            int minute = Integer.parseInt(s.substring(10, 12));
            int second = Integer.parseInt(s.substring(12, 14));

            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.of(
                    year, month, day, hour, minute, second, 0,
                    java.time.ZoneOffset.UTC);
            return zdt.toEpochSecond();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Attempts to parse a timestamp from either FILETIME or GeneralizedTime format.
     *
     * @param value the raw timestamp string
     * @return Unix epoch seconds, or 0 if unparseable
     */
    public static long parseAny(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String s = value.strip();
        if (s.length() >= 14 && !s.contains("-") && Character.isDigit(s.charAt(0))) {
            try {
                long numeric = Long.parseLong(s.replaceAll("[.Z]", "").substring(0,
                        Math.min(s.length(), 18)));
                if (numeric > 100_000_000_000L) {
                    return fromFiletime(s);
                }
            } catch (NumberFormatException ignored) {}
            return fromGeneralizedTime(s);
        }
        return fromFiletime(s);
    }
}
