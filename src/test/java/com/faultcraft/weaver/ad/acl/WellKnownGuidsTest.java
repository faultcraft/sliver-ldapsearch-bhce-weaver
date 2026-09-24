package com.faultcraft.weaver.ad.acl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WellKnownGuidsTest {

    @Test
    void getExtendedRight_knownGuids() {
        assertEquals("ForceChangePassword",
                WellKnownGuids.getExtendedRight(WellKnownGuids.FORCE_CHANGE_PASSWORD));
        assertEquals("GetChanges",
                WellKnownGuids.getExtendedRight(WellKnownGuids.GET_CHANGES));
        assertEquals("GetChangesAll",
                WellKnownGuids.getExtendedRight(WellKnownGuids.GET_CHANGES_ALL));
        assertEquals("GetChangesInFilteredSet",
                WellKnownGuids.getExtendedRight(WellKnownGuids.GET_CHANGES_IN_FILTERED_SET));
        assertEquals("Enroll",
                WellKnownGuids.getExtendedRight(WellKnownGuids.ENROLL));
    }

    @Test
    void getExtendedRight_unknownGuid_returnsNull() {
        assertNull(WellKnownGuids.getExtendedRight("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    }

    @Test
    void getWriteProperty_knownGuids() {
        assertEquals("AddMember",
                WellKnownGuids.getWriteProperty(WellKnownGuids.MEMBER));
        assertEquals("WriteGPLink",
                WellKnownGuids.getWriteProperty(WellKnownGuids.GPLINK));
        assertEquals("AddAllowedToAct",
                WellKnownGuids.getWriteProperty(WellKnownGuids.ALLOWED_TO_ACT));
        assertEquals("AddKeyCredentialLink",
                WellKnownGuids.getWriteProperty(WellKnownGuids.KEY_CREDENTIAL_LINK));
        assertEquals("WriteSPN",
                WellKnownGuids.getWriteProperty(WellKnownGuids.SERVICE_PRINCIPAL_NAME));
        assertEquals("WritePKINameFlag",
                WellKnownGuids.getWriteProperty(WellKnownGuids.PKI_NAME_FLAG));
        assertEquals("WritePKIEnrollmentFlag",
                WellKnownGuids.getWriteProperty(WellKnownGuids.PKI_ENROLLMENT_FLAG));
        assertEquals("WriteAccountRestrictions",
                WellKnownGuids.getWriteProperty(WellKnownGuids.USER_ACCOUNT_RESTRICTIONS_SET));
    }

    @Test
    void getWriteProperty_membershipPropertySet_resolvesToAddMember() {
        assertEquals("AddMember",
                WellKnownGuids.getWriteProperty(WellKnownGuids.MEMBERSHIP_PROPERTY_SET));
    }

    @Test
    void getSelfWrite_memberGuid() {
        assertEquals("AddSelf",
                WellKnownGuids.getSelfWrite(WellKnownGuids.MEMBER));
    }

    @Test
    void getSelfWrite_membershipPropertySet() {
        assertEquals("AddSelf",
                WellKnownGuids.getSelfWrite(WellKnownGuids.MEMBERSHIP_PROPERTY_SET));
    }

    @Test
    void isEmptyGuid_variations() {
        assertTrue(WellKnownGuids.isEmptyGuid(null));
        assertTrue(WellKnownGuids.isEmptyGuid(""));
        assertTrue(WellKnownGuids.isEmptyGuid("00000000-0000-0000-0000-000000000000"));
        assertTrue(WellKnownGuids.isEmptyGuid("{00000000-0000-0000-0000-000000000000}"));
        assertFalse(WellKnownGuids.isEmptyGuid("00299570-246d-11d0-a768-00aa006e0529"));
    }

    @Test
    void normalizeGuid_caseInsensitive() {
        assertNotNull(WellKnownGuids.getExtendedRight(
                "00299570-246D-11D0-A768-00AA006E0529"));
    }

    @Test
    void normalizeGuid_stripsBraces() {
        assertNotNull(WellKnownGuids.getExtendedRight(
                "{00299570-246d-11d0-a768-00aa006e0529}"));
    }

    @Test
    void isLapsGuid_knownGuids() {
        assertTrue(WellKnownGuids.isLapsGuid(WellKnownGuids.MS_MCS_ADMPWD));
        assertTrue(WellKnownGuids.isLapsGuid(WellKnownGuids.MS_LAPS_PASSWORD));
        assertTrue(WellKnownGuids.isLapsGuid(WellKnownGuids.MS_LAPS_ENCRYPTED_PASSWORD));
        assertFalse(WellKnownGuids.isLapsGuid(WellKnownGuids.MEMBER));
    }
}
