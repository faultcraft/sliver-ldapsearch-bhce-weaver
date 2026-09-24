package com.faultcraft.weaver.ad.model;

/** Controls which properties to include in BH-CE JSON output. */
public enum PropertiesLevel {

    /** Only standard BH-CE properties. */
    STANDARD,

    /** Standard plus group membership attributes. */
    MEMBER,

    /** All available properties. */
    ALL;

    /** Parses a properties level string, case-insensitive. Returns ALL if unrecognized. */
    public static PropertiesLevel parse(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.strip().toUpperCase()) {
            case "STANDARD" -> STANDARD;
            case "MEMBER" -> MEMBER;
            default -> ALL;
        };
    }
}
