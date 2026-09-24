package com.faultcraft.weaver.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

/** Converts binary SID (Security Identifier) bytes to string representation. */
public final class SidParser {

    private SidParser() {}

    /**
     * Converts a binary SID to its string form (S-1-5-21-...).
     *
     * @param bytes raw SID bytes (typically from objectSid LDAP attribute)
     * @return string SID, or empty string if the input is invalid
     */
    public static String parse(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return "";
        }

        int revision = bytes[0] & 0xFF;
        int subAuthorityCount = bytes[1] & 0xFF;

        if (bytes.length < 8 + subAuthorityCount * 4) {
            return "";
        }

        long authority = 0;
        for (int i = 2; i < 8; i++) {
            authority = (authority << 8) | (bytes[i] & 0xFF);
        }

        var sb = new StringBuilder();
        sb.append("S-").append(revision).append('-').append(authority);

        ByteBuffer buf = ByteBuffer.wrap(bytes, 8, subAuthorityCount * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < subAuthorityCount; i++) {
            sb.append('-').append(Integer.toUnsignedLong(buf.getInt()));
        }

        return sb.toString();
    }

    /**
     * Converts a base64-encoded SID string to its string form.
     *
     * @param base64 base64-encoded SID bytes
     * @return string SID, or empty string if invalid
     */
    public static String parseBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return "";
        }
        try {
            return parse(Base64.getDecoder().decode(base64.strip()));
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Attempts to parse a SID from either string form or base64 binary.
     *
     * <p>If the value already looks like a string SID (starts with S-), returns it as-is.
     * Otherwise attempts base64 decode.
     */
    public static String parseAny(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.strip();
        if (trimmed.startsWith("S-")) {
            return trimmed;
        }
        return parseBase64(trimmed);
    }
}
