package com.faultcraft.weaver.ad.acl;

/** Access mask constants from MS-DTYP 2.4.3. */
public final class AccessMask {

    private AccessMask() {}

    public static final int GENERIC_ALL_RAW = 0x10000000;
    public static final int GENERIC_ALL = 0x000F01FF;
    public static final int GENERIC_WRITE = 0x00020028;

    public static final int WRITE_OWNER = 0x00080000;
    public static final int WRITE_DACL = 0x00040000;

    public static final int ADS_RIGHT_DS_CONTROL_ACCESS = 0x00000100;
    public static final int ADS_RIGHT_DS_CREATE_CHILD = 0x00000001;
    public static final int ADS_RIGHT_DS_DELETE_CHILD = 0x00000002;
    public static final int ADS_RIGHT_DS_READ_PROP = 0x00000010;
    public static final int ADS_RIGHT_DS_WRITE_PROP = 0x00000020;
    public static final int ADS_RIGHT_DS_SELF = 0x00000008;

    public static boolean hasGenericAll(int mask) {
        return (mask & GENERIC_ALL) == GENERIC_ALL || (mask & GENERIC_ALL_RAW) != 0;
    }

    public static boolean hasGenericWrite(int mask) {
        return (mask & GENERIC_WRITE) == GENERIC_WRITE;
    }

    public static boolean hasWriteOwner(int mask) {
        return (mask & WRITE_OWNER) != 0;
    }

    public static boolean hasWriteDacl(int mask) {
        return (mask & WRITE_DACL) != 0;
    }

    public static boolean hasExtendedRight(int mask) {
        return (mask & ADS_RIGHT_DS_CONTROL_ACCESS) != 0;
    }

    public static boolean hasWriteProperty(int mask) {
        return (mask & ADS_RIGHT_DS_WRITE_PROP) != 0;
    }

    public static boolean hasSelfRight(int mask) {
        return (mask & ADS_RIGHT_DS_SELF) != 0;
    }
}
