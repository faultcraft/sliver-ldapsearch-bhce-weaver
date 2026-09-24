package com.faultcraft.weaver.ad.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelperEnumsTest {

    @Test
    void propertiesLevel_parse_standard() {
        assertEquals(PropertiesLevel.STANDARD, PropertiesLevel.parse("Standard"));
        assertEquals(PropertiesLevel.STANDARD, PropertiesLevel.parse("STANDARD"));
    }

    @Test
    void propertiesLevel_parse_member() {
        assertEquals(PropertiesLevel.MEMBER, PropertiesLevel.parse("Member"));
    }

    @Test
    void propertiesLevel_parse_all() {
        assertEquals(PropertiesLevel.ALL, PropertiesLevel.parse("All"));
        assertEquals(PropertiesLevel.ALL, PropertiesLevel.parse(null));
        assertEquals(PropertiesLevel.ALL, PropertiesLevel.parse(""));
        assertEquals(PropertiesLevel.ALL, PropertiesLevel.parse("unknown"));
    }

    @Test
    void trustDirection_fromValue_maps() {
        assertEquals(TrustDirection.DISABLED, TrustDirection.fromValue(0));
        assertEquals(TrustDirection.INBOUND, TrustDirection.fromValue(1));
        assertEquals(TrustDirection.OUTBOUND, TrustDirection.fromValue(2));
        assertEquals(TrustDirection.BIDIRECTIONAL, TrustDirection.fromValue(3));
        assertEquals(TrustDirection.DISABLED, TrustDirection.fromValue(99));
    }

    @Test
    void trustDirection_labels() {
        assertEquals("Inbound", TrustDirection.INBOUND.label());
        assertEquals(1, TrustDirection.INBOUND.value());
    }

    @Test
    void trustType_fromValue_maps() {
        assertEquals(TrustType.PARENT_CHILD, TrustType.fromValue(0));
        assertEquals(TrustType.CROSS_LINK, TrustType.fromValue(1));
        assertEquals(TrustType.FOREST, TrustType.fromValue(2));
        assertEquals(TrustType.EXTERNAL, TrustType.fromValue(3));
        assertEquals(TrustType.UNKNOWN, TrustType.fromValue(99));
    }

    @Test
    void trustType_labels() {
        assertEquals("Forest", TrustType.FOREST.label());
        assertEquals(2, TrustType.FOREST.value());
    }
}
