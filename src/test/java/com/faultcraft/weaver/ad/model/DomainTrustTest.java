package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTrustTest {

    @Test
    void fromEntry_basicTrust_parsesDirection() {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname",
                List.of("CN=other.local,CN=System,DC=test,DC=local"));
        entry.put("trustpartner", List.of("other.local"));
        entry.put("securityidentifier", List.of("S-1-5-21-999-888-777"));
        entry.put("trustdirection", List.of("3"));
        entry.put("trusttype", List.of("2"));

        DomainTrust trust = DomainTrust.fromEntry(entry);

        assertEquals("OTHER.LOCAL", trust.getTargetDomainName());
        assertEquals("S-1-5-21-999-888-777", trust.getTargetDomainSid());
    }

    @Test
    void fromEntry_trustAttributes_parsesTransitivity() {
        var entry = new HashMap<String, List<String>>();
        entry.put("trustpartner", List.of("child.local"));
        entry.put("trustattributes", List.of("0"));

        DomainTrust trust = DomainTrust.fromEntry(entry);
        Map<String, Object> json = trust.toJson();

        assertTrue((Boolean) json.get("IsTransitive"));
    }

    @Test
    void fromEntry_sidFilteringEnabled() {
        var entry = new HashMap<String, List<String>>();
        entry.put("trustpartner", List.of("partner.local"));
        entry.put("trustattributes", List.of("4"));

        DomainTrust trust = DomainTrust.fromEntry(entry);
        Map<String, Object> json = trust.toJson();

        assertTrue((Boolean) json.get("SidFilteringEnabled"));
    }

    @Test
    void toJson_containsAllKeys() {
        var entry = new HashMap<String, List<String>>();
        entry.put("trustpartner", List.of("trust.local"));
        entry.put("trustdirection", List.of("2"));
        entry.put("trusttype", List.of("2"));

        DomainTrust trust = DomainTrust.fromEntry(entry);
        Map<String, Object> json = trust.toJson();

        assertTrue(json.containsKey("TargetDomainName"));
        assertTrue(json.containsKey("TargetDomainSid"));
        assertTrue(json.containsKey("IsTransitive"));
        assertTrue(json.containsKey("TrustDirection"));
        assertTrue(json.containsKey("TrustType"));
        assertTrue(json.containsKey("SidFilteringEnabled"));
        assertEquals("Outbound", json.get("TrustDirection"));
        assertEquals("Forest", json.get("TrustType"));
    }

    @Test
    void fromEntry_usesTrustpartner_notName() {
        var entry = new HashMap<String, List<String>>();
        entry.put("trustpartner", List.of("actual.local"));
        entry.put("name", List.of("should-ignore"));

        DomainTrust trust = DomainTrust.fromEntry(entry);

        assertEquals("ACTUAL.LOCAL", trust.getTargetDomainName());
    }
}
