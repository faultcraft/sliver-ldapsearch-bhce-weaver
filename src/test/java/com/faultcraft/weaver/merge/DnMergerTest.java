package com.faultcraft.weaver.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DnMergerTest {

    private static Map<String, List<String>> entry(String dn, String... keyValues) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("distinguishedname", new ArrayList<>(List.of(dn)));
        for (int i = 0; i < keyValues.length; i += 2) {
            map.computeIfAbsent(keyValues[i], k -> new ArrayList<>()).add(keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void merge_sameDnDifferentAttrs_mergesBoth() {
        var entries = List.of(
                entry("CN=admin,DC=test,DC=local", "samaccountname", "admin"),
                entry("CN=admin,DC=test,DC=local", "memberof", "CN=Group1,DC=test,DC=local")
        );

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, null);

        assertEquals(1, merged.size(), "Same DN should merge into one entry");
        Map<String, List<String>> obj = merged.get(0);
        assertTrue(obj.containsKey("samaccountname"), "Should have samaccountname from first entry");
        assertTrue(obj.containsKey("memberof"), "Should have memberof from second entry");
    }

    @Test
    void merge_caseInsensitiveDn_treatedAsSame() {
        var entries = List.of(
                entry("CN=admin,DC=test,DC=local", "samaccountname", "admin"),
                entry("cn=admin,dc=test,dc=local", "objectclass", "user")
        );

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, null);
        assertEquals(1, merged.size(), "Case-insensitive DNs should merge");
    }

    @Test
    void merge_multiValuedUnion_deduplicates() {
        var entries = List.of(
                entry("CN=admin,DC=test,DC=local", "memberof", "CN=Group1,DC=test,DC=local"),
                entry("CN=admin,DC=test,DC=local", "memberof", "CN=Group1,DC=test,DC=local"),
                entry("CN=admin,DC=test,DC=local", "memberof", "CN=Group2,DC=test,DC=local")
        );

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, null);
        assertEquals(1, merged.size());
        List<String> memberOf = merged.get(0).get("memberof");
        assertEquals(2, memberOf.size(), "Duplicate values should be deduplicated");
    }

    @Test
    void merge_domainFilter_dnsForm() {
        var entries = List.of(
                entry("CN=admin,DC=test,DC=local", "samaccountname", "admin"),
                entry("CN=user,DC=other,DC=com", "samaccountname", "user")
        );

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, "test.local");

        assertEquals(1, merged.size(), "Only test.local entries should remain");
        assertEquals(List.of("admin"), merged.get(0).get("samaccountname"));
    }

    @Test
    void merge_domainFilter_dnForm() {
        var entries = List.of(
                entry("CN=admin,DC=test,DC=local", "samaccountname", "admin"),
                entry("CN=user,DC=other,DC=com", "samaccountname", "user")
        );

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, "DC=test,DC=local");

        assertEquals(1, merged.size(), "Only DC=test,DC=local entries should remain");
    }

    @Test
    void merge_noDn_skippedWithWarning() {
        Map<String, List<String>> noDn = new HashMap<>();
        noDn.put("samaccountname", new ArrayList<>(List.of("orphan")));

        var entries = List.of(
                noDn,
                entry("CN=admin,DC=test,DC=local", "samaccountname", "admin")
        );

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, null);
        assertEquals(1, merged.size(), "Entry without DN should be skipped");
    }

    @Test
    void merge_emptyInput_returnsEmptyList() {
        List<Map<String, List<String>>> merged = DnMerger.merge(List.of(), null);
        assertTrue(merged.isEmpty());
    }

    @Test
    void merge_preservesInsertionOrder() {
        var entries = List.of(
                entry("CN=zebra,DC=test,DC=local", "samaccountname", "zebra"),
                entry("CN=alpha,DC=test,DC=local", "samaccountname", "alpha"),
                entry("CN=middle,DC=test,DC=local", "samaccountname", "middle")
        );

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, null);
        assertEquals(3, merged.size());
        assertEquals(List.of("zebra"), merged.get(0).get("samaccountname"));
        assertEquals(List.of("alpha"), merged.get(1).get("samaccountname"));
        assertEquals(List.of("middle"), merged.get(2).get("samaccountname"));
    }

    @Test
    void dnMatchesDomain_dnsForm_matches() {
        assertTrue(DnMerger.dnMatchesDomain("CN=admin,DC=test,DC=local", "test.local"));
        assertFalse(DnMerger.dnMatchesDomain("CN=admin,DC=test,DC=local", "other.com"));
    }

    @Test
    void dnMatchesDomain_dnForm_matches() {
        assertTrue(DnMerger.dnMatchesDomain("CN=admin,DC=test,DC=local", "DC=test,DC=local"));
        assertFalse(DnMerger.dnMatchesDomain("CN=admin,DC=test,DC=local", "DC=other,DC=com"));
    }

    @Test
    void dnMatchesDomain_caseInsensitive() {
        assertTrue(DnMerger.dnMatchesDomain("CN=admin,dc=TEST,dc=LOCAL", "test.local"));
    }
}
