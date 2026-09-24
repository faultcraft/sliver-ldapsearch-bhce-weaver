package com.faultcraft.weaver.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WellKnownSidsTest {

    @Test
    void getName_administrators_returnsCorrectName() {
        assertEquals("Administrators", WellKnownSids.getName("S-1-5-32-544"));
    }

    @Test
    void getName_everyone_returnsCorrectName() {
        assertEquals("Everyone", WellKnownSids.getName("S-1-1-0"));
    }

    @Test
    void getName_unknown_returnsNull() {
        assertNull(WellKnownSids.getName("S-1-5-21-123456789-987654321-1001"));
    }

    @Test
    void isWellKnown_builtinSid_returnsTrue() {
        assertTrue(WellKnownSids.isWellKnown("S-1-5-32-544"));
    }

    @Test
    void isWellKnown_domainSid_returnsFalse() {
        assertFalse(WellKnownSids.isWellKnown("S-1-5-21-123456789-987654321-1001"));
    }

    @Test
    void getDomainGroupName_domainAdmins_returnsCorrectName() {
        assertEquals("Domain Admins", WellKnownSids.getDomainGroupName("512"));
    }

    @Test
    void getDomainGroupName_unknown_returnsNull() {
        assertNull(WellKnownSids.getDomainGroupName("9999"));
    }

    @Test
    void extractRid_validSid_returnsLastComponent() {
        assertEquals("1013", WellKnownSids.extractRid("S-1-5-21-12345-67890-1013"));
    }

    @Test
    void extractRid_null_returnsEmpty() {
        assertEquals("", WellKnownSids.extractRid(null));
    }
}
