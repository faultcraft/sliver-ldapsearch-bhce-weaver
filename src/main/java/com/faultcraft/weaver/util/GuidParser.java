package com.faultcraft.weaver.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Locale;

/** Converts binary GUID bytes to string representation. */
public final class GuidParser {

    private GuidParser() {}

    /**
     * Converts a binary GUID to its string form ({xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}).
     *
     * <p>The first three components are little-endian; the last two are big-endian,
     * per the Microsoft GUID/UUID wire format.
     *
     * @param bytes raw GUID bytes (16 bytes from objectGUID LDAP attribute)
     * @return string GUID with braces, or empty string if invalid
     */
    public static String parse(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return "";
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int data1 = buf.getInt();
        short data2 = buf.getShort();
        short data3 = buf.getShort();

        buf.order(ByteOrder.BIG_ENDIAN);
        byte[] data4 = new byte[8];
        buf.get(data4);

        return String.format("{%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x}",
                data1, data2 & 0xFFFF, data3 & 0xFFFF,
                data4[0], data4[1], data4[2], data4[3],
                data4[4], data4[5], data4[6], data4[7])
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Converts a base64-encoded GUID to its string form.
     *
     * @param base64 base64-encoded GUID bytes
     * @return string GUID, or empty string if invalid
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
     * Attempts to parse a GUID from either string form or base64 binary.
     *
     * <p>If the value already looks like a string GUID (contains hyphens), returns it as-is.
     * Otherwise attempts base64 decode.
     */
    public static String parseAny(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.strip();
        if (trimmed.contains("-") && (trimmed.startsWith("{") || trimmed.length() == 36)) {
            return trimmed.startsWith("{") ? trimmed.toUpperCase(Locale.ROOT)
                    : ("{" + trimmed + "}").toUpperCase(Locale.ROOT);
        }
        return parseBase64(trimmed);
    }
}
