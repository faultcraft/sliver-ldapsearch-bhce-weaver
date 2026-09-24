package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodHoundUserTest {

    @Test
    void fromEntry_basicUser_setsProperties() {
        var entry = baseEntry();
        entry.put("samaccountname", List.of("jdoe"));
        entry.put("displayname", List.of("John Doe"));
        entry.put("mail", List.of("jdoe@test.local"));
        entry.put("useraccountcontrol", List.of("512"));

        BloodHoundUser user = BloodHoundUser.fromEntry(entry);

        assertEquals("users", user.objectType());
        assertEquals("jdoe", user.getProperties().get("samaccountname"));
        assertEquals("John Doe", user.getProperties().get("displayname"));
        assertEquals("jdoe@test.local", user.getProperties().get("email"));
        assertTrue(user.getProperties().getBoolean("enabled"));
    }

    @Test
    void fromEntry_disabledUser_setsEnabledFalse() {
        var entry = baseEntry();
        entry.put("useraccountcontrol", List.of("514"));

        BloodHoundUser user = BloodHoundUser.fromEntry(entry);

        assertFalse(user.getProperties().getBoolean("enabled"));
    }

    @Test
    void fromEntry_delegationFlags_setsCorrectly() {
        var entry = baseEntry();
        entry.put("useraccountcontrol", List.of(String.valueOf(0x80000)));

        BloodHoundUser user = BloodHoundUser.fromEntry(entry);

        assertTrue(user.getProperties().getBoolean("unconstraineddelegation"));
    }

    @Test
    void fromEntry_spns_setsHasspnTrue() {
        var entry = baseEntry();
        entry.put("serviceprincipalname", List.of("HTTP/web.test.local", "MSSQLSvc/db.test.local"));

        BloodHoundUser user = BloodHoundUser.fromEntry(entry);

        assertTrue(user.getProperties().getBoolean("hasspn"));
        @SuppressWarnings("unchecked")
        List<String> spns = (List<String>) user.getProperties().get("serviceprincipalnames");
        assertEquals(2, spns.size());
    }

    @Test
    void fromEntry_primaryGroupId_buildsPrimaryGroupSid() {
        var entry = baseEntry();
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-1001"));
        entry.put("primarygroupid", List.of("513"));

        BloodHoundUser user = BloodHoundUser.fromEntry(entry);
        Map<String, Object> json = user.toJson();

        assertEquals("S-1-5-21-111-222-333-513", json.get("PrimaryGroupSID"));
    }

    @Test
    void fromEntry_adminCount_setsTrue() {
        var entry = baseEntry();
        entry.put("admincount", List.of("1"));

        BloodHoundUser user = BloodHoundUser.fromEntry(entry);

        assertTrue(user.getProperties().getBoolean("admincount"));
    }

    @Test
    void toJson_containsRequiredKeys() {
        var entry = baseEntry();
        BloodHoundUser user = BloodHoundUser.fromEntry(entry);
        Map<String, Object> json = user.toJson();

        assertTrue(json.containsKey("ObjectIdentifier"));
        assertTrue(json.containsKey("Properties"));
        assertTrue(json.containsKey("Aces"));
        assertTrue(json.containsKey("IsACLProtected"));
        assertTrue(json.containsKey("PrimaryGroupSID"));
        assertTrue(json.containsKey("AllowedToDelegate"));
        assertTrue(json.containsKey("SPNTargets"));
        assertTrue(json.containsKey("HasSIDHistory"));
    }

    @Test
    void fromEntry_emptyEntry_defaultsApplied() {
        var entry = baseEntry();
        BloodHoundUser user = BloodHoundUser.fromEntry(entry);

        assertEquals("", user.getProperties().get("samaccountname"));
        assertFalse(user.getProperties().getBoolean("hasspn"));
        assertFalse(user.getProperties().getBoolean("admincount"));
    }

    private static Map<String, List<String>> baseEntry() {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname",
                List.of("CN=Test,OU=Users,DC=test,DC=local"));
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-1001"));
        return entry;
    }
}
