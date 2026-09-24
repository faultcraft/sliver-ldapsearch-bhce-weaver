package com.faultcraft.weaver.util;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidParserTest {

    @Test
    void parse_validGuid_returnsCorrectString() {
        // {6F1E78AA-01B3-4A3D-8B5E-7C9F2D3E4A5B}
        byte[] bytes = {
            (byte)0xAA, 0x78, 0x1E, 0x6F,
            (byte)0xB3, 0x01,
            0x3D, 0x4A,
            (byte)0x8B, 0x5E,
            0x7C, (byte)0x9F, 0x2D, 0x3E, 0x4A, 0x5B
        };
        String guid = GuidParser.parse(bytes);
        assertEquals("{6F1E78AA-01B3-4A3D-8B5E-7C9F2D3E4A5B}", guid);
    }

    @Test
    void parse_nullBytes_returnsEmpty() {
        assertEquals("", GuidParser.parse(null));
    }

    @Test
    void parse_wrongLength_returnsEmpty() {
        assertEquals("", GuidParser.parse(new byte[]{0x01, 0x02, 0x03}));
    }

    @Test
    void parseBase64_validEncoding_returnsGuid() {
        byte[] bytes = {
            (byte)0xAA, 0x78, 0x1E, 0x6F,
            (byte)0xB3, 0x01,
            0x3D, 0x4A,
            (byte)0x8B, 0x5E,
            0x7C, (byte)0x9F, 0x2D, 0x3E, 0x4A, 0x5B
        };
        String b64 = Base64.getEncoder().encodeToString(bytes);
        assertEquals("{6F1E78AA-01B3-4A3D-8B5E-7C9F2D3E4A5B}", GuidParser.parseBase64(b64));
    }

    @Test
    void parseAny_stringGuid_normalizes() {
        assertEquals("{6F1E78AA-01B3-4A3D-8B5E-7C9F2D3E4A5B}",
                GuidParser.parseAny("6f1e78aa-01b3-4a3d-8b5e-7c9f2d3e4a5b"));
    }

    @Test
    void parseAny_bracketedGuid_normalizes() {
        assertEquals("{6F1E78AA-01B3-4A3D-8B5E-7C9F2D3E4A5B}",
                GuidParser.parseAny("{6f1e78aa-01b3-4a3d-8b5e-7c9f2d3e4a5b}"));
    }

    @Test
    void parseAny_null_returnsEmpty() {
        assertEquals("", GuidParser.parseAny(null));
        assertEquals("", GuidParser.parseAny(""));
    }
}
