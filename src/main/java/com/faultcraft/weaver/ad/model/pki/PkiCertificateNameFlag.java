package com.faultcraft.weaver.ad.model.pki;

import java.util.ArrayList;
import java.util.List;

public final class PkiCertificateNameFlag {

    public static final int NONE = 0;
    public static final int ENROLLEE_SUPPLIES_SUBJECT = 1;
    public static final int ADD_EMAIL = 0x00000002;
    public static final int ADD_OBJ_GUID = 0x00000004;
    public static final int OLD_CERT_SUPPLIES_SUBJECT_AND_ALT_NAME = 0x00000008;
    public static final int ADD_DIRECTORY_PATH = 0x00000100;
    public static final int ENROLLEE_SUPPLIES_SUBJECT_ALT_NAME = 0x00010000;
    public static final int SUBJECT_ALT_REQUIRE_DOMAIN_DNS = 0x00400000;
    public static final int SUBJECT_ALT_REQUIRE_SPN = 0x00800000;
    public static final int SUBJECT_ALT_REQUIRE_DIRECTORY_GUID = 0x01000000;
    public static final int SUBJECT_ALT_REQUIRE_UPN = 0x02000000;
    public static final int SUBJECT_ALT_REQUIRE_EMAIL = 0x04000000;
    public static final int SUBJECT_ALT_REQUIRE_DNS = 0x08000000;
    public static final int SUBJECT_REQUIRE_DNS_AS_CN = 0x10000000;
    public static final int SUBJECT_REQUIRE_EMAIL = 0x20000000;
    public static final int SUBJECT_REQUIRE_COMMON_NAME = 0x40000000;
    public static final int SUBJECT_REQUIRE_DIRECTORY_PATH = 0x80000000;

    private PkiCertificateNameFlag() {}

    public static boolean hasFlag(int value, int flag) {
        return (value & flag) == flag;
    }

    public static List<String> decompose(int value) {
        List<String> flags = new ArrayList<>();
        addIf(flags, value, ENROLLEE_SUPPLIES_SUBJECT, "ENROLLEE_SUPPLIES_SUBJECT");
        addIf(flags, value, ADD_EMAIL, "ADD_EMAIL");
        addIf(flags, value, ADD_OBJ_GUID, "ADD_OBJ_GUID");
        addIf(flags, value, OLD_CERT_SUPPLIES_SUBJECT_AND_ALT_NAME, "OLD_CERT_SUPPLIES_SUBJECT_AND_ALT_NAME");
        addIf(flags, value, ADD_DIRECTORY_PATH, "ADD_DIRECTORY_PATH");
        addIf(flags, value, ENROLLEE_SUPPLIES_SUBJECT_ALT_NAME, "ENROLLEE_SUPPLIES_SUBJECT_ALT_NAME");
        addIf(flags, value, SUBJECT_ALT_REQUIRE_DOMAIN_DNS, "SUBJECT_ALT_REQUIRE_DOMAIN_DNS");
        addIf(flags, value, SUBJECT_ALT_REQUIRE_SPN, "SUBJECT_ALT_REQUIRE_SPN");
        addIf(flags, value, SUBJECT_ALT_REQUIRE_DIRECTORY_GUID, "SUBJECT_ALT_REQUIRE_DIRECTORY_GUID");
        addIf(flags, value, SUBJECT_ALT_REQUIRE_UPN, "SUBJECT_ALT_REQUIRE_UPN");
        addIf(flags, value, SUBJECT_ALT_REQUIRE_EMAIL, "SUBJECT_ALT_REQUIRE_EMAIL");
        addIf(flags, value, SUBJECT_ALT_REQUIRE_DNS, "SUBJECT_ALT_REQUIRE_DNS");
        addIf(flags, value, SUBJECT_REQUIRE_DNS_AS_CN, "SUBJECT_REQUIRE_DNS_AS_CN");
        addIf(flags, value, SUBJECT_REQUIRE_EMAIL, "SUBJECT_REQUIRE_EMAIL");
        addIf(flags, value, SUBJECT_REQUIRE_COMMON_NAME, "SUBJECT_REQUIRE_COMMON_NAME");
        addIf(flags, value, SUBJECT_REQUIRE_DIRECTORY_PATH, "SUBJECT_REQUIRE_DIRECTORY_PATH");
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
