package com.faultcraft.weaver.ad.model.pki;

import java.util.ArrayList;
import java.util.List;

public final class PkiEnrollmentFlag {

    public static final int NONE = 0x00000000;
    public static final int INCLUDE_SYMMETRIC_ALGORITHMS = 0x00000001;
    public static final int PEND_ALL_REQUESTS = 0x00000002;
    public static final int PUBLISH_TO_KRA_CONTAINER = 0x00000004;
    public static final int PUBLISH_TO_DS = 0x00000008;
    public static final int AUTO_ENROLLMENT_CHECK_USER_DS_CERTIFICATE = 0x00000010;
    public static final int AUTO_ENROLLMENT = 0x00000020;
    public static final int CT_FLAG_DOMAIN_AUTHENTICATION_NOT_REQUIRED = 0x80;
    public static final int PREVIOUS_APPROVAL_VALIDATE_REENROLLMENT = 0x00000040;
    public static final int USER_INTERACTION_REQUIRED = 0x00000100;
    public static final int ADD_TEMPLATE_NAME = 0x200;
    public static final int REMOVE_INVALID_CERTIFICATE_FROM_PERSONAL_STORE = 0x00000400;
    public static final int ALLOW_ENROLL_ON_BEHALF_OF = 0x00000800;
    public static final int ADD_OCSP_NOCHECK = 0x00001000;
    public static final int ENABLE_KEY_REUSE_ON_NT_TOKEN_KEYSET_STORAGE_FULL = 0x00002000;
    public static final int NOREVOCATIONINFOINISSUEDCERTS = 0x00004000;
    public static final int INCLUDE_BASIC_CONSTRAINTS_FOR_EE_CERTS = 0x00008000;
    public static final int ALLOW_PREVIOUS_APPROVAL_KEYBASEDRENEWAL = 0x00010000;
    public static final int ISSUANCE_POLICIES_FROM_REQUEST = 0x00020000;
    public static final int SKIP_AUTO_RENEWAL = 0x00040000;
    public static final int NO_SECURITY_EXTENSION = 0x00080000;

    private PkiEnrollmentFlag() {}

    public static boolean hasFlag(int value, int flag) {
        return (value & flag) == flag;
    }

    public static List<String> decompose(int value) {
        List<String> flags = new ArrayList<>();
        addIf(flags, value, INCLUDE_SYMMETRIC_ALGORITHMS, "INCLUDE_SYMMETRIC_ALGORITHMS");
        addIf(flags, value, PEND_ALL_REQUESTS, "PEND_ALL_REQUESTS");
        addIf(flags, value, PUBLISH_TO_KRA_CONTAINER, "PUBLISH_TO_KRA_CONTAINER");
        addIf(flags, value, PUBLISH_TO_DS, "PUBLISH_TO_DS");
        addIf(flags, value, AUTO_ENROLLMENT_CHECK_USER_DS_CERTIFICATE, "AUTO_ENROLLMENT_CHECK_USER_DS_CERTIFICATE");
        addIf(flags, value, AUTO_ENROLLMENT, "AUTO_ENROLLMENT");
        addIf(flags, value, PREVIOUS_APPROVAL_VALIDATE_REENROLLMENT, "PREVIOUS_APPROVAL_VALIDATE_REENROLLMENT");
        addIf(flags, value, CT_FLAG_DOMAIN_AUTHENTICATION_NOT_REQUIRED, "CT_FLAG_DOMAIN_AUTHENTICATION_NOT_REQUIRED");
        addIf(flags, value, USER_INTERACTION_REQUIRED, "USER_INTERACTION_REQUIRED");
        addIf(flags, value, ADD_TEMPLATE_NAME, "ADD_TEMPLATE_NAME");
        addIf(flags, value, REMOVE_INVALID_CERTIFICATE_FROM_PERSONAL_STORE, "REMOVE_INVALID_CERTIFICATE_FROM_PERSONAL_STORE");
        addIf(flags, value, ALLOW_ENROLL_ON_BEHALF_OF, "ALLOW_ENROLL_ON_BEHALF_OF");
        addIf(flags, value, ADD_OCSP_NOCHECK, "ADD_OCSP_NOCHECK");
        addIf(flags, value, ENABLE_KEY_REUSE_ON_NT_TOKEN_KEYSET_STORAGE_FULL, "ENABLE_KEY_REUSE_ON_NT_TOKEN_KEYSET_STORAGE_FULL");
        addIf(flags, value, NOREVOCATIONINFOINISSUEDCERTS, "NOREVOCATIONINFOINISSUEDCERTS");
        addIf(flags, value, INCLUDE_BASIC_CONSTRAINTS_FOR_EE_CERTS, "INCLUDE_BASIC_CONSTRAINTS_FOR_EE_CERTS");
        addIf(flags, value, ALLOW_PREVIOUS_APPROVAL_KEYBASEDRENEWAL, "ALLOW_PREVIOUS_APPROVAL_KEYBASEDRENEWAL");
        addIf(flags, value, ISSUANCE_POLICIES_FROM_REQUEST, "ISSUANCE_POLICIES_FROM_REQUEST");
        addIf(flags, value, SKIP_AUTO_RENEWAL, "SKIP_AUTO_RENEWAL");
        addIf(flags, value, NO_SECURITY_EXTENSION, "NO_SECURITY_EXTENSION");
        if (flags.isEmpty()) {
            flags.add("NONE");
        }
        return flags;
    }

    private static void addIf(List<String> flags, int value, int flag, String name) {
        if (hasFlag(value, flag)) {
            flags.add(name);
        }
    }
}
