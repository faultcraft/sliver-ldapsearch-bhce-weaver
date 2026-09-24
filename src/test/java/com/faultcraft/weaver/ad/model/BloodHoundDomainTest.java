package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodHoundDomainTest {

    @Test
    void fromEntry_basicDomain_setsProperties() {
        var entry = baseEntry();
        entry.put("msds-behavior-version", List.of("7"));

        BloodHoundDomain domain = BloodHoundDomain.fromEntry(entry);

        assertEquals("domains", domain.objectType());
        assertEquals("2016", domain.getProperties().get("functionallevel"));
    }

    @Test
    void toJson_containsRequiredKeys() {
        var entry = baseEntry();
        BloodHoundDomain domain = BloodHoundDomain.fromEntry(entry);
        Map<String, Object> json = domain.toJson();

        assertTrue(json.containsKey("ChildObjects"));
        assertTrue(json.containsKey("Trusts"));
        assertTrue(json.containsKey("Links"));
        assertTrue(json.containsKey("GPOChanges"));
    }

    @Test
    void mutableLists_acceptAndSerialize() {
        var entry = baseEntry();
        BloodHoundDomain domain = BloodHoundDomain.fromEntry(entry);

        domain.getTrusts().add(Map.of("TargetDomainName", "OTHER.LOCAL"));
        domain.getChildObjects().add(Map.of("ObjectIdentifier", "guid-123"));
        domain.getLinks().add(Map.of("GUID", "gpo-guid"));

        Map<String, Object> json = domain.toJson();
        @SuppressWarnings("unchecked")
        List<?> trusts = (List<?>) json.get("Trusts");
        assertEquals(1, trusts.size());
    }

    @Test
    void fromEntry_noFunctionalLevel_defaults() {
        var entry = baseEntry();
        BloodHoundDomain domain = BloodHoundDomain.fromEntry(entry);

        assertEquals("", domain.getProperties().getString("functionallevel"));
    }

    private static Map<String, List<String>> baseEntry() {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname", List.of("DC=test,DC=local"));
        entry.put("objectsid", List.of("S-1-5-21-111-222-333"));
        return entry;
    }
}
