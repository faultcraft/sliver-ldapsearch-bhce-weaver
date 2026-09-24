package com.faultcraft.weaver.ad.model;

/** AD trust type flags from trustType attribute. */
public enum TrustType {

    PARENT_CHILD(0, "ParentChild"),
    CROSS_LINK(1, "CrossLink"),
    FOREST(2, "Forest"),
    EXTERNAL(3, "External"),
    UNKNOWN(4, "Unknown");

    private final int value;
    private final String label;

    TrustType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** Resolves a trust type from its integer value. */
    public static TrustType fromValue(int value) {
        return switch (value) {
            case 0 -> PARENT_CHILD;
            case 1 -> CROSS_LINK;
            case 2 -> FOREST;
            case 3 -> EXTERNAL;
            default -> UNKNOWN;
        };
    }
}
