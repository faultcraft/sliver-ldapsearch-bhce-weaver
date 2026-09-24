package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodHoundGroupTest {

    @Test
    void fromEntry_basicGroup_setsProperties() {
        var entry = baseEntry();
        entry.put("samaccountname", List.of("Domain Admins"));
        entry.put("admincount", List.of("1"));

        BloodHoundGroup group = BloodHoundGroup.fromEntry(entry);

        assertEquals("groups", group.objectType());
        assertEquals("Domain Admins", group.getProperties().get("samaccountname"));
        assertTrue(group.getProperties().getBoolean("admincount"));
    }

    @Test
    void getMembers_mutableAndSerializedImmutably() {
        var entry = baseEntry();
        BloodHoundGroup group = BloodHoundGroup.fromEntry(entry);

        group.getMembers().add(Map.of(
                "ObjectIdentifier", "S-1-5-21-111-222-333-1001",
                "ObjectType", "User"));

        assertEquals(1, group.getMembers().size());

        Map<String, Object> json = group.toJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> members = (List<Map<String, Object>>) json.get("Members");
        assertEquals(1, members.size());
    }

    @Test
    void toJson_containsRequiredKeys() {
        var entry = baseEntry();
        BloodHoundGroup group = BloodHoundGroup.fromEntry(entry);
        Map<String, Object> json = group.toJson();

        assertTrue(json.containsKey("ObjectIdentifier"));
        assertTrue(json.containsKey("Properties"));
        assertTrue(json.containsKey("Members"));
    }

    private static Map<String, List<String>> baseEntry() {
        var entry = new HashMap<String, List<String>>();
        entry.put("distinguishedname",
                List.of("CN=Domain Admins,CN=Users,DC=test,DC=local"));
        entry.put("objectsid", List.of("S-1-5-21-111-222-333-512"));
        return entry;
    }
}
