package com.faultcraft.weaver.util;

import java.util.Map;

/** Registry of well-known Windows Security Identifiers. */
public final class WellKnownSids {

    private WellKnownSids() {}

    private static final Map<String, String> SIDS = Map.ofEntries(
            Map.entry("S-1-0-0", "Nobody"),
            Map.entry("S-1-1-0", "Everyone"),
            Map.entry("S-1-2-0", "Local"),
            Map.entry("S-1-2-1", "Console Logon"),
            Map.entry("S-1-3-0", "Creator Owner"),
            Map.entry("S-1-3-1", "Creator Group"),
            Map.entry("S-1-3-4", "Owner Rights"),
            Map.entry("S-1-5-1", "Dialup"),
            Map.entry("S-1-5-2", "Network"),
            Map.entry("S-1-5-3", "Batch"),
            Map.entry("S-1-5-4", "Interactive"),
            Map.entry("S-1-5-6", "Service"),
            Map.entry("S-1-5-7", "Anonymous"),
            Map.entry("S-1-5-9", "Enterprise Domain Controllers"),
            Map.entry("S-1-5-10", "Principal Self"),
            Map.entry("S-1-5-11", "Authenticated Users"),
            Map.entry("S-1-5-13", "Terminal Server Users"),
            Map.entry("S-1-5-14", "Remote Interactive Logon"),
            Map.entry("S-1-5-17", "IUSR"),
            Map.entry("S-1-5-18", "Local System"),
            Map.entry("S-1-5-19", "NT Authority\\Local Service"),
            Map.entry("S-1-5-20", "NT Authority\\Network Service"),
            Map.entry("S-1-5-32-544", "Administrators"),
            Map.entry("S-1-5-32-545", "Users"),
            Map.entry("S-1-5-32-546", "Guests"),
            Map.entry("S-1-5-32-547", "Power Users"),
            Map.entry("S-1-5-32-548", "Account Operators"),
            Map.entry("S-1-5-32-549", "Server Operators"),
            Map.entry("S-1-5-32-550", "Print Operators"),
            Map.entry("S-1-5-32-551", "Backup Operators"),
            Map.entry("S-1-5-32-552", "Replicators"),
            Map.entry("S-1-5-32-554", "Pre-Windows 2000 Compatible Access"),
            Map.entry("S-1-5-32-555", "Remote Desktop Users"),
            Map.entry("S-1-5-32-556", "Network Configuration Operators"),
            Map.entry("S-1-5-32-557", "Incoming Forest Trust Builders"),
            Map.entry("S-1-5-32-558", "Performance Monitor Users"),
            Map.entry("S-1-5-32-559", "Performance Log Users"),
            Map.entry("S-1-5-32-560", "Windows Authorization Access Group"),
            Map.entry("S-1-5-32-561", "Terminal Server License Servers"),
            Map.entry("S-1-5-32-562", "Distributed COM Users"),
            Map.entry("S-1-5-32-568", "IIS_IUSRS"),
            Map.entry("S-1-5-32-569", "Cryptographic Operators"),
            Map.entry("S-1-5-32-573", "Event Log Readers"),
            Map.entry("S-1-5-32-574", "Certificate Service DCOM Access"),
            Map.entry("S-1-5-32-575", "RDS Remote Access Servers"),
            Map.entry("S-1-5-32-576", "RDS Endpoint Servers"),
            Map.entry("S-1-5-32-577", "RDS Management Servers"),
            Map.entry("S-1-5-32-578", "Hyper-V Administrators"),
            Map.entry("S-1-5-32-579", "Access Control Assistance Operators"),
            Map.entry("S-1-5-32-580", "Remote Management Users"),
            Map.entry("S-1-5-32-582", "Storage Replica Administrators")
    );

    /** Domain-relative RIDs for well-known domain groups. */
    private static final Map<String, String> DOMAIN_RIDS = Map.ofEntries(
            Map.entry("498", "Enterprise Read-only Domain Controllers"),
            Map.entry("500", "Administrator"),
            Map.entry("501", "Guest"),
            Map.entry("502", "KRBTGT"),
            Map.entry("512", "Domain Admins"),
            Map.entry("513", "Domain Users"),
            Map.entry("514", "Domain Guests"),
            Map.entry("515", "Domain Computers"),
            Map.entry("516", "Domain Controllers"),
            Map.entry("517", "Cert Publishers"),
            Map.entry("518", "Schema Admins"),
            Map.entry("519", "Enterprise Admins"),
            Map.entry("520", "Group Policy Creator Owners"),
            Map.entry("521", "Read-only Domain Controllers"),
            Map.entry("522", "Cloneable Domain Controllers"),
            Map.entry("525", "Protected Users"),
            Map.entry("526", "Key Admins"),
            Map.entry("527", "Enterprise Key Admins"),
            Map.entry("553", "RAS and IAS Servers"),
            Map.entry("571", "Allowed RODC Password Replication Group"),
            Map.entry("572", "Denied RODC Password Replication Group")
    );

    /** Returns the name of a well-known SID, or null if not well-known. */
    public static String getName(String sid) {
        return SIDS.get(sid);
    }

    /** Returns true if the SID is a well-known (non-domain) SID. */
    public static boolean isWellKnown(String sid) {
        return SIDS.containsKey(sid);
    }

    /** Returns the domain group name for a well-known RID, or null. */
    public static String getDomainGroupName(String rid) {
        return DOMAIN_RIDS.get(rid);
    }

    /** Extracts the RID (last sub-authority) from a SID string. */
    public static String extractRid(String sid) {
        if (sid == null || !sid.startsWith("S-")) {
            return "";
        }
        int lastDash = sid.lastIndexOf('-');
        if (lastDash < 0) {
            return "";
        }
        return sid.substring(lastDash + 1);
    }
}
