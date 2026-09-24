package com.faultcraft.weaver.ad.acl;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AceResolverTest {

    private AceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AceResolver();
    }

    @Test
    void resolve_genericAll_returnsGenericAll() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED, 0, 0x000F01FF, "", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertEquals(1, result.size());
        assertEquals("GenericAll", result.get(0).get("RightName"));
        assertEquals("S-1-5-21-111-222-333-512", result.get(0).get("PrincipalSID"));
    }

    @Test
    void resolve_writeOwner_returnsWriteOwner() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED, 0, 0x00080000, "", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertEquals(1, result.size());
        assertEquals("WriteOwner", result.get(0).get("RightName"));
    }

    @Test
    void resolve_writeDacl_returnsWriteDacl() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED, 0, 0x00040000, "", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertEquals(1, result.size());
        assertEquals("WriteDacl", result.get(0).get("RightName"));
    }

    @Test
    void resolve_inheritedAce_markedAsInherited() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED, 0x10, 0x000F01FF, "", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertFalse(result.isEmpty());
        assertTrue((Boolean) result.get(0).get("IsInherited"));
    }

    @Test
    void resolve_denyAce_filtered() {
        var ace = new AceRecord(AceRecord.ACCESS_DENIED, 0, 0x000F01FF, "", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_selfSid_filtered() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED, 0, 0x000F01FF, "", "",
                "S-1-5-10");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_creatorOwnerSid_filtered() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED, 0, 0x000F01FF, "", "",
                "S-1-3-0");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_extendedRight_forceChangePassword() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS,
                "00299570-246d-11d0-a768-00aa006e0529", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertEquals(1, result.size());
        assertEquals("ForceChangePassword", result.get(0).get("RightName"));
    }

    @Test
    void resolve_extendedRight_getChanges() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS,
                "1131f6aa-9c07-11d1-f79f-00c04fc2dcd2", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "domains");

        assertEquals(1, result.size());
        assertEquals("GetChanges", result.get(0).get("RightName"));
    }

    @Test
    void resolve_writeProperty_addMember() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                "bf9679c0-0de6-11d0-a285-00aa003049e2", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "groups");

        assertEquals(1, result.size());
        assertEquals("AddMember", result.get(0).get("RightName"));
    }

    @Test
    void resolve_emptyGuid_extendedRight_allExtended() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS,
                "00000000-0000-0000-0000-000000000000", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertEquals(1, result.size());
        assertEquals("AllExtendedRights", result.get(0).get("RightName"));
    }

    @Test
    void resolve_emptyGuid_writeProperty_genericWrite() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                "00000000-0000-0000-0000-000000000000", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertEquals(1, result.size());
        assertEquals("GenericWrite", result.get(0).get("RightName"));
    }

    @Test
    void resolve_genericAll_shortCircuits() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED, 0,
                0x000F01FF | 0x00080000 | 0x00040000, "", "",
                "S-1-5-21-111-222-333-512");

        List<Map<String, Object>> result = resolver.resolve(List.of(ace), "users");

        assertEquals(1, result.size());
        assertEquals("GenericAll", result.get(0).get("RightName"));
    }

    @Test
    void resolve_emptyInput_returnsEmpty() {
        List<Map<String, Object>> result = resolver.resolve(List.of(), "users");
        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_addMember_onlyOnGroups() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.MEMBER, "",
                "S-1-5-21-111-222-333-512");

        assertTrue(resolver.resolve(List.of(ace), "users").isEmpty());
        assertFalse(resolver.resolve(List.of(ace), "groups").isEmpty());
    }

    @Test
    void resolve_addAllowedToAct_onlyOnComputers() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.ALLOWED_TO_ACT, "",
                "S-1-5-21-111-222-333-512");

        assertTrue(resolver.resolve(List.of(ace), "users").isEmpty());
        assertEquals("AddAllowedToAct",
                resolver.resolve(List.of(ace), "computers").get(0).get("RightName"));
    }

    @Test
    void resolve_writeSPN_onUsersAndComputers() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.SERVICE_PRINCIPAL_NAME, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("WriteSPN",
                resolver.resolve(List.of(ace), "users").get(0).get("RightName"));
        assertEquals("WriteSPN",
                resolver.resolve(List.of(ace), "computers").get(0).get("RightName"));
        assertTrue(resolver.resolve(List.of(ace), "groups").isEmpty());
    }

    @Test
    void resolve_writeGPLink_onOUsAndDomains() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.GPLINK, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("WriteGPLink",
                resolver.resolve(List.of(ace), "ous").get(0).get("RightName"));
        assertEquals("WriteGPLink",
                resolver.resolve(List.of(ace), "domains").get(0).get("RightName"));
        assertTrue(resolver.resolve(List.of(ace), "users").isEmpty());
    }

    @Test
    void resolve_writeAccountRestrictions_skipsDomainAdmins() {
        var ace512 = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.USER_ACCOUNT_RESTRICTIONS_SET, "",
                "S-1-5-21-111-222-333-512");

        assertTrue(resolver.resolve(List.of(ace512), "computers").isEmpty());

        var aceOther = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.USER_ACCOUNT_RESTRICTIONS_SET, "",
                "S-1-5-21-111-222-333-1001");

        assertEquals("WriteAccountRestrictions",
                resolver.resolve(List.of(aceOther), "computers").get(0).get("RightName"));
    }

    @Test
    void resolve_addKeyCredentialLink_onUsersAndComputers() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.KEY_CREDENTIAL_LINK, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("AddKeyCredentialLink",
                resolver.resolve(List.of(ace), "users").get(0).get("RightName"));
        assertEquals("AddKeyCredentialLink",
                resolver.resolve(List.of(ace), "computers").get(0).get("RightName"));
    }

    @Test
    void resolve_writePKINameFlag_onCertTemplates() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.PKI_NAME_FLAG, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("WritePKINameFlag",
                resolver.resolve(List.of(ace), "certtemplates").get(0).get("RightName"));
        assertTrue(resolver.resolve(List.of(ace), "users").isEmpty());
    }

    @Test
    void resolve_writePKIEnrollmentFlag_onCertTemplates() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_WRITE_PROP,
                WellKnownGuids.PKI_ENROLLMENT_FLAG, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("WritePKIEnrollmentFlag",
                resolver.resolve(List.of(ace), "certtemplates").get(0).get("RightName"));
    }

    @Test
    void resolve_enroll_onCertTemplatesAndEnterpriseCA() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS,
                WellKnownGuids.ENROLL, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("Enroll",
                resolver.resolve(List.of(ace), "certtemplates").get(0).get("RightName"));
        assertEquals("Enroll",
                resolver.resolve(List.of(ace), "enterprisecas").get(0).get("RightName"));
    }

    @Test
    void resolve_allExtendedRights_onlyWithLapsForComputers() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS,
                "", "",
                "S-1-5-21-111-222-333-512");

        assertTrue(resolver.resolve(List.of(ace), "computers", false).isEmpty());
        assertEquals("AllExtendedRights",
                resolver.resolve(List.of(ace), "computers", true).get(0).get("RightName"));
    }

    @Test
    void resolve_readLAPSPassword_withLapsGuid() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_CONTROL_ACCESS,
                WellKnownGuids.MS_MCS_ADMPWD, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("ReadLAPSPassword",
                resolver.resolve(List.of(ace), "computers", true).get(0).get("RightName"));
        assertTrue(resolver.resolve(List.of(ace), "computers", false).isEmpty());
    }

    @Test
    void resolve_selfWrite_addSelf_onGroups() {
        var ace = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.ADS_RIGHT_DS_SELF,
                WellKnownGuids.MEMBER, "",
                "S-1-5-21-111-222-333-512");

        assertEquals("AddSelf",
                resolver.resolve(List.of(ace), "groups").get(0).get("RightName"));
        assertTrue(resolver.resolve(List.of(ace), "users").isEmpty());
    }

    @Test
    void resolve_objectAce_genericAll_requiresEmptyGuid() {
        var aceNoGuid = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.GENERIC_ALL, "", "",
                "S-1-5-21-111-222-333-512");

        var aceWithGuid = new AceRecord(AceRecord.ACCESS_ALLOWED_OBJECT, 0,
                AccessMask.GENERIC_ALL,
                WellKnownGuids.MEMBER, "",
                "S-1-5-21-111-222-333-512");

        assertFalse(resolver.resolve(List.of(aceNoGuid), "users").isEmpty());
        var withGuidResult = resolver.resolve(List.of(aceWithGuid), "groups");
        assertTrue(withGuidResult.stream()
                .noneMatch(m -> "GenericAll".equals(m.get("RightName"))));
    }
}
