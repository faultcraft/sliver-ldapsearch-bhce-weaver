package com.faultcraft.weaver.ldap;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LdapBlockParserTest {

    @Test
    void parse_validBlock_extractsAttributes() {
        String text = """
                --------------------
                distinguishedName: CN=admin,DC=test,DC=local
                sAMAccountName: admin
                objectClass: user
                --------------------
                [+] Retrieved 1 results""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);

        assertEquals(1, entries.size(), "Should parse one entry");
        Map<String, List<String>> entry = entries.get(0);
        assertEquals(List.of("CN=admin,DC=test,DC=local"), entry.get("distinguishedname"));
        assertEquals(List.of("admin"), entry.get("samaccountname"));
        assertEquals(List.of("user"), entry.get("objectclass"));
    }

    @Test
    void parse_multipleBlocks_extractsAll() {
        String text = """
                --------------------
                distinguishedName: CN=user1,DC=test,DC=local
                sAMAccountName: user1
                --------------------
                distinguishedName: CN=user2,DC=test,DC=local
                sAMAccountName: user2
                --------------------
                [+] Retrieved 2 results""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertEquals(2, entries.size(), "Should parse two entries");
    }

    @Test
    void parse_multiValuedAttribute_collectsAll() {
        String text = """
                --------------------
                distinguishedName: CN=admin,DC=test,DC=local
                memberOf: CN=Group1,DC=test,DC=local
                memberOf: CN=Group2,DC=test,DC=local
                memberOf: CN=Group3,DC=test,DC=local
                --------------------""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertEquals(1, entries.size());
        List<String> memberOf = entries.get(0).get("memberof");
        assertEquals(3, memberOf.size(), "Should have three memberOf values");
    }

    @Test
    void parse_statusLines_stripped() {
        String text = """
                --------------------
                [*] Searching for objects...
                distinguishedName: CN=admin,DC=test,DC=local
                [+] Found 1 object
                sAMAccountName: admin
                [-] Some warning
                [!] Alert
                Search returned early
                --------------------""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertEquals(1, entries.size());
        Map<String, List<String>> entry = entries.get(0);
        assertEquals(2, entry.size(), "Should only have dn and samaccountname");
    }

    @Test
    void parse_emptyBlock_skipped() {
        String text = """
                --------------------
                --------------------
                distinguishedName: CN=admin,DC=test,DC=local
                --------------------""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertEquals(1, entries.size(), "Empty block should be skipped");
    }

    @Test
    void parse_emptyInput_returnsEmptyList() {
        assertTrue(LdapBlockParser.parse("").isEmpty());
        assertTrue(LdapBlockParser.parse(null).isEmpty());
        assertTrue(LdapBlockParser.parse("   ").isEmpty());
    }

    @Test
    void parse_noColons_skipsLines() {
        String text = """
                --------------------
                this line has no colon
                distinguishedName: CN=admin,DC=test,DC=local
                --------------------""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertEquals(1, entries.size());
        assertEquals(1, entries.get(0).size(), "Only the DN line should be parsed");
    }

    @Test
    void parse_keysAreLowercased() {
        String text = """
                --------------------
                DistinguishedName: CN=admin,DC=test,DC=local
                SAMAccountName: admin
                ObjectSID: S-1-5-21-123
                --------------------""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertTrue(entries.get(0).containsKey("distinguishedname"));
        assertTrue(entries.get(0).containsKey("samaccountname"));
        assertTrue(entries.get(0).containsKey("objectsid"));
    }

    @Test
    void parse_retrievedLineAsBlock_skipped() {
        String text = """
                --------------------
                Retrieved 5 results
                --------------------""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertTrue(entries.isEmpty(), "Retrieved-only block should be skipped");
    }

    @Test
    void parse_truncatedBlock_returnsPartial() {
        String text = """
                --------------------
                distinguishedName: CN=admin,DC=test,DC=local
                sAMAccountName: admin""";

        List<Map<String, List<String>>> entries = LdapBlockParser.parse(text);
        assertEquals(1, entries.size(), "Truncated block should still parse");
        assertEquals(List.of("admin"), entries.get(0).get("samaccountname"));
    }
}
