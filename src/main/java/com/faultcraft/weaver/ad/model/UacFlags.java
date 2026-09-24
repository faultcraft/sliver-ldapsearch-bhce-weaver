package com.faultcraft.weaver.ad.model;

public final class UacFlags {

    public static final int DISABLED = 0x00000002;
    public static final int PASSWORD_NOT_REQUIRED = 0x00000020;
    public static final int SERVER_TRUST_ACCOUNT = 0x00002000;
    public static final int PASSWORD_NEVER_EXPIRES = 0x00010000;
    public static final int UNCONSTRAINED_DELEGATION = 0x00080000;
    public static final int SENSITIVE = 0x00100000;
    public static final int DONT_REQUIRE_PREAUTH = 0x00400000;
    public static final int TRUSTED_TO_AUTH = 0x01000000;

    private UacFlags() {}

    public static int parse(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isEnabled(int uac) {
        return (uac & DISABLED) == 0;
    }

    public static boolean isDomainController(int uac) {
        return (uac & SERVER_TRUST_ACCOUNT) == SERVER_TRUST_ACCOUNT;
    }
}
