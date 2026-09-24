package com.faultcraft.weaver.ad.model;

/** AD trust direction flags from trustDirection attribute. */
public enum TrustDirection {

    DISABLED(0, "Disabled"),
    INBOUND(1, "Inbound"),
    OUTBOUND(2, "Outbound"),
    BIDIRECTIONAL(3, "Bidirectional");

    private final int value;
    private final String label;

    TrustDirection(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }

    /** Resolves a trust direction from its integer value. */
    public static TrustDirection fromValue(int value) {
        return switch (value) {
            case 1 -> INBOUND;
            case 2 -> OUTBOUND;
            case 3 -> BIDIRECTIONAL;
            default -> DISABLED;
        };
    }
}
