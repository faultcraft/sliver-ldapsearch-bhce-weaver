package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodHoundOUTest {

    @Test
    void fromEntry_basicOU_setsObjectType() {
        BloodHoundOU ou = BloodHoundOU.fromEntry(baseEntry());
        assertEquals("ous", ou.objectType());
    }

    @Test
    void fromEntry_blocksInheritance_setsTrue() {
        var entry = baseEntry();
        entry.put("gpoptions", List.of("1"));

        BloodHoundOU ou = BloodHoundOU.fromEntry(entry);

        assertTrue(ou.getProperties().getBoolean("blocksinheritance"));
    }

    @Test
    void fromEntry_noGpoOptions_defaultsFalse() {
        BloodHoundOU ou = BloodHoundOU.fromEntry(baseEntry());
        assertFalse(ou.getProperties().getBoolean("blocksinheritance"));
    }

    @Test
    void toJson_containsContainerKeys() {
        BloodHoundOU ou = BloodHoundOU.fromEntry(baseEntry());
        Map<String, Object> json = ou.toJson();

        assertTrue(json.containsKey("ChildObjects"));
        assertTrue(json.containsKey("Links"));
        assertTrue(json.containsKey("GPOChanges"));
    }

    private static Map<String, List<String>> baseEntry() {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname",
                List.of("OU=Sales,DC=test,DC=local"));
        entry.put("objectguid", List.of("{12345678-1234-1234-1234-123456789ABC}"));
        return entry;
    }
}
