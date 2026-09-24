package com.faultcraft.weaver.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimestampParserTest {

    @Test
    void fromFiletime_validValue_convertsCorrectly() {
        // 132500000000000000 = 2020-12-01T00:00:00Z (approx)
        long result = TimestampParser.fromFiletime("132500000000000000");
        assertTrue(result > 0, "Should produce a positive epoch value");
        assertTrue(result > 1_600_000_000L, "Should be after year 2020");
        assertTrue(result < 1_700_000_000L, "Should be before year 2024");
    }

    @Test
    void fromFiletime_zero_returnsZero() {
        assertEquals(0, TimestampParser.fromFiletime("0"));
    }

    @Test
    void fromFiletime_neverExpires_returnsZero() {
        assertEquals(0, TimestampParser.fromFiletime("9223372036854775807"));
    }

    @Test
    void fromFiletime_null_returnsZero() {
        assertEquals(0, TimestampParser.fromFiletime(null));
        assertEquals(0, TimestampParser.fromFiletime(""));
    }

    @Test
    void fromFiletime_invalid_returnsZero() {
        assertEquals(0, TimestampParser.fromFiletime("not-a-number"));
    }

    @Test
    void fromGeneralizedTime_validValue_convertsCorrectly() {
        // 20210615120000.0Z = 2021-06-15T12:00:00Z
        long result = TimestampParser.fromGeneralizedTime("20210615120000.0Z");
        assertEquals(1623758400L, result);
    }

    @Test
    void fromGeneralizedTime_null_returnsZero() {
        assertEquals(0, TimestampParser.fromGeneralizedTime(null));
        assertEquals(0, TimestampParser.fromGeneralizedTime(""));
    }

    @Test
    void fromGeneralizedTime_tooShort_returnsZero() {
        assertEquals(0, TimestampParser.fromGeneralizedTime("2021"));
    }
}
