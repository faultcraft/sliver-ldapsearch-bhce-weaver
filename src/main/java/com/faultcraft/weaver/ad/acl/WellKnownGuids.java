package com.faultcraft.weaver.ad.acl;

import java.util.Map;

/** Maps well-known AD schema GUIDs to BH-CE edge right names. */
public final class WellKnownGuids {

    private WellKnownGuids() {}

    // Extended rights GUIDs
    public static final String FORCE_CHANGE_PASSWORD =
            "00299570-246d-11d0-a768-00aa006e0529";
    public static final String GET_CHANGES =
            "1131f6aa-9c07-11d1-f79f-00c04fc2dcd2";
    public static final String GET_CHANGES_ALL =
            "1131f6ad-9c07-11d1-f79f-00c04fc2dcd2";
    public static final String GET_CHANGES_IN_FILTERED_SET =
            "89e95b76-444d-4c62-991a-0facbeda640c";
    public static final String ENROLL =
            "0e10c968-78fb-11d2-90d4-00c04f79dc55";

    // Property attribute GUIDs
    public static final String MEMBER =
            "bf9679c0-0de6-11d0-a285-00aa003049e2";
    public static final String GPLINK =
            "f30e3bbe-9ff0-11d1-b603-0000f80367c1";
    public static final String ALLOWED_TO_ACT =
            "3f78c3e5-f79a-46bd-a0b8-9d18116ddc79";
    public static final String KEY_CREDENTIAL_LINK =
            "5b47d60f-6090-40b2-9f37-2a4de88f3063";
    public static final String SERVICE_PRINCIPAL_NAME =
            "f3a64788-5306-11d1-a9c5-0000f80367c1";
    public static final String PKI_NAME_FLAG =
            "ea1dddc4-60ff-416e-8cc0-17cee534bce7";
    public static final String PKI_ENROLLMENT_FLAG =
            "d15ef7d8-f226-46db-ae79-b34e560bd12c";

    // Property set GUIDs
    public static final String USER_ACCOUNT_RESTRICTIONS_SET =
            "4c164200-20c0-11d0-a768-00aa006e0529";
    public static final String MEMBERSHIP_PROPERTY_SET =
            "bc0ac240-79a9-11d0-9020-00c04fc2d4cf";

    // LAPS attribute GUIDs (looked up from schema at runtime in SharpHound,
    // but we hardcode the well-known ones since we lack schema enumeration)
    public static final String MS_MCS_ADMPWD =
            "27e31206-3a08-4d40-8612-ce5a1b270a32";
    public static final String MS_LAPS_PASSWORD =
            "d41e4d9e-8c02-4ef6-893f-b45ab987b01d";
    public static final String MS_LAPS_ENCRYPTED_PASSWORD =
            "b4aec542-b11b-47ce-89a3-98f10178b733";

    public static final String EMPTY_GUID =
            "00000000-0000-0000-0000-000000000000";

    /** Extended rights GUIDs -> BH-CE right name. */
    public static final Map<String, String> EXTENDED_RIGHTS = Map.of(
            FORCE_CHANGE_PASSWORD, "ForceChangePassword",
            GET_CHANGES, "GetChanges",
            GET_CHANGES_ALL, "GetChangesAll",
            GET_CHANGES_IN_FILTERED_SET, "GetChangesInFilteredSet",
            ENROLL, "Enroll"
    );

    /** Write property GUIDs -> BH-CE right name. */
    public static final Map<String, String> WRITE_PROPERTY_RIGHTS = Map.ofEntries(
            Map.entry(MEMBER, "AddMember"),
            Map.entry(GPLINK, "WriteGPLink"),
            Map.entry(ALLOWED_TO_ACT, "AddAllowedToAct"),
            Map.entry(KEY_CREDENTIAL_LINK, "AddKeyCredentialLink"),
            Map.entry(SERVICE_PRINCIPAL_NAME, "WriteSPN"),
            Map.entry(PKI_NAME_FLAG, "WritePKINameFlag"),
            Map.entry(PKI_ENROLLMENT_FLAG, "WritePKIEnrollmentFlag"),
            Map.entry(USER_ACCOUNT_RESTRICTIONS_SET, "WriteAccountRestrictions"),
            Map.entry(MEMBERSHIP_PROPERTY_SET, "AddMember")
    );

    /** Validated write (Self) GUIDs -> BH-CE right name. */
    public static final Map<String, String> SELF_WRITE_GUIDS = Map.of(
            MEMBER, "AddSelf",
            MEMBERSHIP_PROPERTY_SET, "AddSelf"
    );

    /** LAPS attribute GUIDs for ReadLAPSPassword right. */
    public static final java.util.Set<String> LAPS_GUIDS = java.util.Set.of(
            MS_MCS_ADMPWD,
            MS_LAPS_PASSWORD,
            MS_LAPS_ENCRYPTED_PASSWORD
    );

    /** Returns the extended right name for a GUID, or null if unknown. */
    public static String getExtendedRight(String guid) {
        return EXTENDED_RIGHTS.get(normalizeGuid(guid));
    }

    /** Returns the write property name for a GUID, or null if unknown. */
    public static String getWriteProperty(String guid) {
        return WRITE_PROPERTY_RIGHTS.get(normalizeGuid(guid));
    }

    /** Returns the self-write right name for a GUID, or null if unknown. */
    public static String getSelfWrite(String guid) {
        return SELF_WRITE_GUIDS.get(normalizeGuid(guid));
    }

    /** Returns true if the GUID is a LAPS attribute. */
    public static boolean isLapsGuid(String guid) {
        return LAPS_GUIDS.contains(normalizeGuid(guid));
    }

    /** Returns true if the GUID represents "all" objects. */
    public static boolean isEmptyGuid(String guid) {
        return guid == null || guid.isEmpty() || EMPTY_GUID.equals(normalizeGuid(guid));
    }

    static String normalizeGuid(String guid) {
        if (guid == null) {
            return "";
        }
        return guid.strip().toLowerCase().replaceAll("[{}]", "");
    }
}
