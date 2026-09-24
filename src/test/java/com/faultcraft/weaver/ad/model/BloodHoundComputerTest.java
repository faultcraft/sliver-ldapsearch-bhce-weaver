package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodHoundComputerTest {

    @Test
    void fromEntry_basicComputer_setsProperties() {
        var entry = baseEntry();
        entry.put("samaccountname", List.of("WS01$"));
        entry.put("operatingsystem", List.of("Windows 10 Pro"));
        entry.put("dnshostname", List.of("ws01.test.local"));
        entry.put("useraccountcontrol", List.of("4096"));

        BloodHoundComputer comp = BloodHoundComputer.fromEntry(entry);

        assertEquals("computers", comp.objectType());
        assertEquals("Windows 10 Pro", comp.getProperties().get("operatingsystem"));
        assertEquals("WS01.TEST.LOCAL", comp.getProperties().getString("name"));
        assertTrue(comp.getProperties().getBoolean("enabled"));
    }

    @Test
    void fromEntry_domainController_setsIsDc() {
        var entry = baseEntry();
        entry.put("useraccountcontrol", List.of(String.valueOf(0x2000)));

        BloodHoundComputer comp = BloodHoundComputer.fromEntry(entry);

        assertTrue(comp.getProperties().getBoolean("isdc"));
    }

    @Test
    void fromEntry_laps_setsHasLaps() {
        var entry = baseEntry();
        entry.put("ms-mcs-admpwdexpirationtime", List.of("132500000000000000"));

        BloodHoundComputer comp = BloodHoundComputer.fromEntry(entry);

        assertTrue(comp.getProperties().getBoolean("haslaps"));
    }

    @Test
    void fromEntry_noLaps_setsHasLapsFalse() {
        var entry = baseEntry();

        BloodHoundComputer comp = BloodHoundComputer.fromEntry(entry);

        assertFalse(comp.getProperties().getBoolean("haslaps"));
    }

    @Test
    void toJson_containsSessionKeys() {
        var entry = baseEntry();
        BloodHoundComputer comp = BloodHoundComputer.fromEntry(entry);
        Map<String, Object> json = comp.toJson();

        assertTrue(json.containsKey("Sessions"));
        assertTrue(json.containsKey("PrivilegedSessions"));
        assertTrue(json.containsKey("RegistrySessions"));
        assertTrue(json.containsKey("LocalGroups"));
        assertNull(json.get("Status"));
    }

    @Test
    void fromEntry_primaryGroupId_buildsPrimaryGroupSid() {
        var entry = baseEntry();
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-1001"));
        entry.put("primarygroupid", List.of("515"));

        BloodHoundComputer comp = BloodHoundComputer.fromEntry(entry);
        Map<String, Object> json = comp.toJson();

        assertEquals("S-1-5-21-111-222-333-515", json.get("PrimaryGroupSID"));
    }

    private static Map<String, List<String>> baseEntry() {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname",
                List.of("CN=WS01,OU=Computers,DC=test,DC=local"));
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-1001"));
        return entry;
    }
}
