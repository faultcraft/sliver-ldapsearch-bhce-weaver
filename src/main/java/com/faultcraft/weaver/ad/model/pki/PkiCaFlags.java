package com.faultcraft.weaver.ad.model.pki;

import java.util.ArrayList;
import java.util.List;

public final class PkiCaFlags {

    public static final int NO_TEMPLATE_SUPPORT = 1;
    public static final int SUPPORTS_NT_AUTHENTICATION = 2;
    public static final int CA_SUPPORTS_MANUAL_AUTHENTICATION = 4;
    public static final int CA_SERVERTYPE_ADVANCED = 8;

    private PkiCaFlags() {}

    public static List<String> decompose(int value) {
        List<String> flags = new ArrayList<>();
        if ((value & NO_TEMPLATE_SUPPORT) != 0) {
            flags.add("NO_TEMPLATE_SUPPORT");
        }
        if ((value & SUPPORTS_NT_AUTHENTICATION) != 0) {
            flags.add("SUPPORTS_NT_AUTHENTICATION");
        }
        if ((value & CA_SUPPORTS_MANUAL_AUTHENTICATION) != 0) {
            flags.add("CA_SUPPORTS_MANUAL_AUTHENTICATION");
        }
        if ((value & CA_SERVERTYPE_ADVANCED) != 0) {
            flags.add("CA_SERVERTYPE_ADVANCED");
        }
        return flags;
    }
}
