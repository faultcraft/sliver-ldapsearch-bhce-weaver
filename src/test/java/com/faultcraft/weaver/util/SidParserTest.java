package com.faultcraft.weaver.util;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidParserTest {

    @Test
    void parse_validSid_returnsCorrectString() {
        // S-1-5-32-544 (Administrators) -- well-known, easy to verify
        byte[] bytes = {
            0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
            0x20, 0x00, 0x00, 0x00,
            0x20, 0x02, 0x00, 0x00
        };
        assertEquals("S-1-5-32-544", SidParser.parse(bytes));
    }

    @Test
    void parse_builtinSid_returnsCorrectString() {
        // S-1-5-32-544 (Administrators)
        byte[] bytes = {
            0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
            0x20, 0x00, 0x00, 0x00,
            0x20, 0x02, 0x00, 0x00
        };
        assertEquals("S-1-5-32-544", SidParser.parse(bytes));
    }

    @Test
    void parse_nullBytes_returnsEmpty() {
        assertEquals("", SidParser.parse(null));
    }

    @Test
    void parse_tooShort_returnsEmpty() {
        assertEquals("", SidParser.parse(new byte[]{0x01, 0x02}));
    }

    @Test
    void parseBase64_validEncoding_returnsSid() {
        byte[] bytes = {
            0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
            0x20, 0x00, 0x00, 0x00,
            0x20, 0x02, 0x00, 0x00
        };
        String b64 = Base64.getEncoder().encodeToString(bytes);
        assertEquals("S-1-5-32-544", SidParser.parseBase64(b64));
    }

    @Test
    void parseBase64_invalidEncoding_returnsEmpty() {
        assertEquals("", SidParser.parseBase64("not-base64!!!"));
    }

    @Test
    void parseAny_stringSid_returnsAsIs() {
        assertEquals("S-1-5-21-12345-67890-1013", SidParser.parseAny("S-1-5-21-12345-67890-1013"));
    }

    @Test
    void parseAny_null_returnsEmpty() {
        assertEquals("", SidParser.parseAny(null));
        assertEquals("", SidParser.parseAny(""));
    }
}
