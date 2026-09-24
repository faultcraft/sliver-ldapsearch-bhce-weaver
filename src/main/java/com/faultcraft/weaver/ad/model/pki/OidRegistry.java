package com.faultcraft.weaver.ad.model.pki;

import java.util.Map;
import java.util.Set;

public final class OidRegistry {

    private OidRegistry() {}

    public static final Set<String> AUTHENTICATION_OIDS = Set.of(
            "1.3.6.1.5.5.7.3.2",
            "1.3.6.1.5.2.3.4",
            "1.3.6.1.4.1.311.20.2.2",
            "2.5.29.37.0"
    );

    private static final Map<String, String> OID_TO_NAME = Map.ofEntries(
            Map.entry("1.3.6.1.4.1.311.76.6.1", "Windows Update"),
            Map.entry("1.3.6.1.4.1.311.10.3.11", "Key Recovery"),
            Map.entry("1.3.6.1.4.1.311.10.3.25", "Windows Third Party Application Component"),
            Map.entry("1.3.6.1.4.1.311.21.6", "Key Recovery Agent"),
            Map.entry("1.3.6.1.4.1.311.10.3.6", "Windows System Component Verification"),
            Map.entry("1.3.6.1.4.1.311.61.4.1", "Early Launch Antimalware Drive"),
            Map.entry("1.3.6.1.4.1.311.10.3.23", "Windows TCB Component"),
            Map.entry("1.3.6.1.4.1.311.61.1.1", "Kernel Mode Code Signing"),
            Map.entry("1.3.6.1.4.1.311.10.3.26", "Windows Software Extension Verification"),
            Map.entry("2.23.133.8.3", "Attestation Identity Key Certificate"),
            Map.entry("1.3.6.1.4.1.311.76.3.1", "Windows Store"),
            Map.entry("1.3.6.1.4.1.311.10.6.1", "Key Pack Licenses"),
            Map.entry("1.3.6.1.4.1.311.20.2.2", "Smart Card Logon"),
            Map.entry("1.3.6.1.5.2.3.5", "KDC Authentication"),
            Map.entry("1.3.6.1.5.5.7.3.7", "IP security use"),
            Map.entry("1.3.6.1.4.1.311.10.3.8", "Embedded Windows System Component Verification"),
            Map.entry("1.3.6.1.4.1.311.10.3.20", "Windows Kits Component"),
            Map.entry("1.3.6.1.5.5.7.3.6", "IP security tunnel termination"),
            Map.entry("1.3.6.1.4.1.311.10.3.5", "Windows Hardware Driver Verification"),
            Map.entry("1.3.6.1.5.5.8.2.2", "IP security IKE intermediate"),
            Map.entry("1.3.6.1.4.1.311.10.3.39", "Windows Hardware Driver Extended Verification"),
            Map.entry("1.3.6.1.4.1.311.10.6.2", "License Server Verification"),
            Map.entry("1.3.6.1.4.1.311.10.3.5.1", "Windows Hardware Driver Attested Verification"),
            Map.entry("1.3.6.1.4.1.311.76.5.1", "Dynamic Code Generator"),
            Map.entry("1.3.6.1.5.5.7.3.8", "Time Stamping"),
            Map.entry("1.3.6.1.4.1.311.10.3.4.1", "File Recovery"),
            Map.entry("1.3.6.1.4.1.311.2.6.1", "SpcRelaxedPEMarkerCheck"),
            Map.entry("2.23.133.8.1", "Endorsement Key Certificate"),
            Map.entry("1.3.6.1.4.1.311.2.6.2", "SpcEncryptedDigestRetryCount"),
            Map.entry("1.3.6.1.4.1.311.10.3.4", "Encrypting File System"),
            Map.entry("1.3.6.1.5.5.7.3.1", "Server Authentication"),
            Map.entry("1.3.6.1.4.1.311.61.5.1", "HAL Extension"),
            Map.entry("1.3.6.1.5.5.7.3.4", "Secure Email"),
            Map.entry("1.3.6.1.5.5.7.3.5", "IP security end system"),
            Map.entry("1.3.6.1.4.1.311.10.3.9", "Root List Signer"),
            Map.entry("1.3.6.1.4.1.311.10.3.30", "Disallowed List"),
            Map.entry("1.3.6.1.4.1.311.10.3.19", "Revoked List Signer"),
            Map.entry("1.3.6.1.4.1.311.10.3.21", "Windows RT Verification"),
            Map.entry("1.3.6.1.4.1.311.10.3.10", "Qualified Subordination"),
            Map.entry("1.3.6.1.4.1.311.10.3.12", "Document Signing"),
            Map.entry("1.3.6.1.4.1.311.10.3.24", "Protected Process Verification"),
            Map.entry("1.3.6.1.4.1.311.80.1", "Document Encryption"),
            Map.entry("1.3.6.1.4.1.311.10.3.22", "Protected Process Light Verification"),
            Map.entry("1.3.6.1.4.1.311.21.19", "Directory Service Email Replication"),
            Map.entry("1.3.6.1.4.1.311.21.5", "Private Key Archival"),
            Map.entry("1.3.6.1.4.1.311.10.5.1", "Digital Rights"),
            Map.entry("1.3.6.1.4.1.311.10.3.27", "Preview Build Signing"),
            Map.entry("1.3.6.1.4.1.311.20.2.1", "Certificate Request Agent"),
            Map.entry("2.23.133.8.2", "Platform Certificate"),
            Map.entry("1.3.6.1.4.1.311.20.1", "CTL Usage"),
            Map.entry("1.3.6.1.5.5.7.3.9", "OCSP Signing"),
            Map.entry("1.3.6.1.5.5.7.3.3", "Code Signing"),
            Map.entry("1.3.6.1.4.1.311.10.3.1", "Microsoft Trust List Signing"),
            Map.entry("1.3.6.1.4.1.311.10.3.2", "Microsoft Time Stamping"),
            Map.entry("1.3.6.1.4.1.311.76.8.1", "Microsoft Publisher"),
            Map.entry("1.3.6.1.5.5.7.3.2", "Client Authentication"),
            Map.entry("1.3.6.1.5.2.3.4", "PKINIT Client Authentication"),
            Map.entry("1.3.6.1.4.1.311.10.3.13", "Lifetime Signing"),
            Map.entry("2.5.29.37.0", "Any Purpose"),
            Map.entry("1.3.6.1.4.1.311.64.1.1", "Server Trust"),
            Map.entry("1.3.6.1.4.1.311.10.3.7", "OEM Windows System Component Verification")
    );

    public static String resolveName(String oid) {
        return OID_TO_NAME.getOrDefault(oid, oid);
    }
}
