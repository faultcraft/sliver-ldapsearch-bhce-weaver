package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.ad.model.pki.FiletimeSpan;
import com.faultcraft.weaver.ad.model.pki.OidRegistry;
import com.faultcraft.weaver.ad.model.pki.PkiCertificateNameFlag;
import com.faultcraft.weaver.ad.model.pki.PkiEnrollmentFlag;
import com.faultcraft.weaver.ad.model.pki.PkiPrivateKeyFlag;

public final class BloodHoundCertTemplate extends BloodHoundObject {

    private Map<String, Object> containedBy = Map.of();

    @Override
    public String objectType() {
        return "certtemplates";
    }

    public static BloodHoundCertTemplate fromEntry(Map<String, List<String>> entry) {
        var tpl = new BloodHoundCertTemplate();
        tpl.populateCommon(entry);

        TypedProperties p = tpl.getProperties();

        String dn = firstValue(entry, "distinguishedname");
        String domain = p.getString("domain");
        String cn = extractCnFromDn(dn);
        p.put("name", (cn + "@" + domain).toUpperCase(Locale.ROOT));

        parsePeriods(entry, p);
        parseSchemaVersion(entry, p);
        parseDisplayAndOid(entry, p);
        parseEnrollmentFlags(entry, p);
        parseCertNameFlags(entry, p);
        parseEkus(entry, p);
        parseApplicationPolicies(entry, p);
        parseIssuancePolicies(entry, p);
        parseSignatures(entry, p);
        computeEffectiveEkus(p);
        computeAuthEnabled(p);

        return tpl;
    }

    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        return json;
    }

    private static void parsePeriods(Map<String, List<String>> entry, TypedProperties p) {
        String validity = firstValue(entry, "pkiexpirationperiod");
        if (!validity.isEmpty()) {
            long secs = FiletimeSpan.toSecondsFromBase64(validity);
            p.put("validityperiod", FiletimeSpan.toDisplayString(secs));
        }
        String renewal = firstValue(entry, "pkioverlapperiod");
        if (!renewal.isEmpty()) {
            long secs = FiletimeSpan.toSecondsFromBase64(renewal);
            p.put("renewalperiod", FiletimeSpan.toDisplayString(secs));
        }
    }

    private static void parseSchemaVersion(Map<String, List<String>> entry,
            TypedProperties p) {
        String sv = firstValue(entry, "mspki-template-schema-version");
        if (!sv.isEmpty()) {
            try {
                p.put("schemaversion", Integer.parseInt(sv.strip()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private static void parseDisplayAndOid(Map<String, List<String>> entry,
            TypedProperties p) {
        String dn = firstValue(entry, "displayname");
        if (!dn.isEmpty()) { p.put("displayname", dn); }
        String oid = firstValue(entry, "mspki-cert-template-oid");
        if (!oid.isEmpty()) { p.put("oid", oid); }
    }

    private static void parseEnrollmentFlags(Map<String, List<String>> entry,
            TypedProperties p) {
        int flag = parseIntSafe(firstValue(entry, "mspki-enrollment-flag"));
        p.put("enrollmentflag", String.join(", ", PkiEnrollmentFlag.decompose(flag)));
        p.put("requiresmanagerapproval",
                PkiEnrollmentFlag.hasFlag(flag, PkiEnrollmentFlag.PEND_ALL_REQUESTS));
        p.put("nosecurityextension",
                PkiEnrollmentFlag.hasFlag(flag, PkiEnrollmentFlag.NO_SECURITY_EXTENSION));
    }

    private static void parseCertNameFlags(Map<String, List<String>> entry,
            TypedProperties p) {
        int flag = parseIntSafe(firstValue(entry, "mspki-certificate-name-flag"));
        p.put("certificatenameflag",
                String.join(", ", PkiCertificateNameFlag.decompose(flag)));
        p.put("enrolleesuppliessubject", PkiCertificateNameFlag.hasFlag(flag,
                PkiCertificateNameFlag.ENROLLEE_SUPPLIES_SUBJECT));
        p.put("subjectaltrequireupn", PkiCertificateNameFlag.hasFlag(flag,
                PkiCertificateNameFlag.SUBJECT_ALT_REQUIRE_UPN));
        p.put("subjectaltrequiredns", PkiCertificateNameFlag.hasFlag(flag,
                PkiCertificateNameFlag.SUBJECT_ALT_REQUIRE_DNS));
        p.put("subjectaltrequiredomaindns", PkiCertificateNameFlag.hasFlag(flag,
                PkiCertificateNameFlag.SUBJECT_ALT_REQUIRE_DOMAIN_DNS));
        p.put("subjectaltrequireemail", PkiCertificateNameFlag.hasFlag(flag,
                PkiCertificateNameFlag.SUBJECT_ALT_REQUIRE_EMAIL));
        p.put("subjectaltrequirespn", PkiCertificateNameFlag.hasFlag(flag,
                PkiCertificateNameFlag.SUBJECT_ALT_REQUIRE_SPN));
        p.put("subjectrequireemail", PkiCertificateNameFlag.hasFlag(flag,
                PkiCertificateNameFlag.SUBJECT_REQUIRE_EMAIL));
    }

    private static void parseEkus(Map<String, List<String>> entry, TypedProperties p) {
        String raw = firstValue(entry, "pkiextendedkeyusage");
        List<String> ekus = raw.isEmpty() ? List.of() : List.of(raw.split(", "));
        p.put("ekus", new ArrayList<>(ekus));

        String cap = firstValue(entry, "mspki-certificate-application-policy");
        List<String> caps = cap.isEmpty() ? List.of() : List.of(cap.split(", "));
        p.put("certificateapplicationpolicy", new ArrayList<>(caps));
    }

    private static void parseApplicationPolicies(Map<String, List<String>> entry,
            TypedProperties p) {
        String raw = firstValue(entry, "mspki-ra-application-policies");
        if (raw.isEmpty()) {
            return;
        }
        int sv = p.getInt("schemaversion");
        int pkFlag = parseIntSafe(firstValue(entry, "mspki-private-key-flag"));
        boolean useLegacy = PkiPrivateKeyFlag.hasFlag(pkFlag,
                PkiPrivateKeyFlag.USE_LEGACY_PROVIDER);

        if (sv <= 2 || (sv == 4 && useLegacy)) {
            p.put("applicationpolicies", new ArrayList<>(List.of(raw.split(", "))));
            return;
        }
        List<String> parts = List.of(raw.split(", "));
        List<String> result = new ArrayList<>();
        for (int i = 0; i + 2 < parts.size(); i += 3) {
            if ("mspki-ra-application-policies".equalsIgnoreCase(parts.get(i))) {
                result.add(parts.get(i + 2));
            }
        }
        p.put("applicationpolicies", result);
    }

    private static void parseIssuancePolicies(Map<String, List<String>> entry,
            TypedProperties p) {
        String raw = firstValue(entry, "mspki-ra-policies");
        if (!raw.isEmpty()) {
            p.put("issuancepolicies", new ArrayList<>(List.of(raw.split(", "))));
            return;
        }
        raw = firstValue(entry, "mspki-certificate-policy");
        if (!raw.isEmpty()) {
            p.put("issuancepolicies", new ArrayList<>(List.of(raw.split(", "))));
        }
    }

    private static void parseSignatures(Map<String, List<String>> entry,
            TypedProperties p) {
        String raw = firstValue(entry, "mspki-ra-signature");
        if (!raw.isEmpty()) {
            p.put("authorizedsignatures", parseIntSafe(raw));
        }
    }

    private static void computeEffectiveEkus(TypedProperties p) {
        int sv = p.getInt("schemaversion");
        List<String> ekus = p.getStringList("ekus");
        List<String> caps = p.getStringList("certificateapplicationpolicy");
        if (sv == 1 && !ekus.isEmpty()) {
            p.put("effectiveekus", new ArrayList<>(ekus));
        } else if (!caps.isEmpty()) {
            p.put("effectiveekus", new ArrayList<>(caps));
        }
    }

    private static void computeAuthEnabled(TypedProperties p) {
        List<String> effective = p.getStringList("effectiveekus");
        if (effective.isEmpty()) {
            p.put("authenticationenabled", true);
            return;
        }
        boolean hasAuth = effective.stream()
                .anyMatch(OidRegistry.AUTHENTICATION_OIDS::contains);
        p.put("authenticationenabled", hasAuth);
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.isBlank()) { return 0; }
        try { return Integer.parseInt(value.strip()); }
        catch (NumberFormatException e) { return 0; }
    }
}
