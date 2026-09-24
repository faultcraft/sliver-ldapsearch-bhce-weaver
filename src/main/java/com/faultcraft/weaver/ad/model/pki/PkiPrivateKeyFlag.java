package com.faultcraft.weaver.ad.model.pki;

public final class PkiPrivateKeyFlag {

    public static final int REQUIRE_PRIVATE_KEY_ARCHIVAL = 0x00000001;
    public static final int EXPORTABLE_KEY = 0x00000010;
    public static final int STRONG_KEY_PROTECTION_REQUIRED = 0x00000020;
    public static final int REQUIRE_ALTERNATE_SIGNATURE_ALGORITHM = 0x00000040;
    public static final int REQUIRE_SAME_KEY_RENEWAL = 0x00000080;
    public static final int USE_LEGACY_PROVIDER = 0x00000100;
    public static final int ATTEST_REQUIRED = 0x00002000;
    public static final int ATTEST_PREFERRED = 0x00001000;
    public static final int ATTESTATION_WITHOUT_POLICY = 0x00004000;
    public static final int EK_TRUST_ON_USE = 0x00000200;
    public static final int EK_VALIDATE_CERT = 0x00000400;
    public static final int EK_VALIDATE_KEY = 0x00000800;
    public static final int HELLO_LOGON_KEY = 0x00200000;

    private PkiPrivateKeyFlag() {}

    public static boolean hasFlag(int value, int flag) {
        return (value & flag) == flag;
    }
}
