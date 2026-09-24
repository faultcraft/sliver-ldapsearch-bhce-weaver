package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BloodHoundDomain extends BloodHoundObject {

    private final List<Map<String, Object>> childObjects = new ArrayList<>();
    private final List<Map<String, Object>> trusts = new ArrayList<>();
    private final List<Map<String, Object>> links = new ArrayList<>();

    /* Mutable: raw GPLink DN+options pairs for resolution */
    private final List<String[]> gpLinks = new ArrayList<>();

    @Override
    public String objectType() {
        return "domains";
    }

    public static BloodHoundDomain fromEntry(Map<String, List<String>> entry) {
        var domain = new BloodHoundDomain();
        domain.populateCommon(entry);

        TypedProperties p = domain.getProperties();

        String dn = firstValue(entry, "distinguishedname");
        if (!dn.isEmpty()) {
            String domainName = dnToDomain(dn);
            p.put("name", domainName);
            p.put("domain", domainName);
        }

        String sid = firstValue(entry, "objectsid");
        if (!sid.isEmpty()) {
            String parsedSid = com.faultcraft.weaver.util.SidParser.parseAny(sid);
            p.put("domainsid", parsedSid);
        }

        p.put("highvalue", true);
        p.put("collected", true);

        String funcLevel = firstValue(entry, "msds-behavior-version");
        if (!funcLevel.isEmpty()) {
            p.put("functionallevel", resolveFunctionalLevel(funcLevel));
        }

        parseGpLinks(entry, domain.gpLinks);

        return domain;
    }

    public List<Map<String, Object>> getChildObjects() { return childObjects; }
    public List<Map<String, Object>> getTrusts() { return trusts; }
    public List<Map<String, Object>> getLinks() { return links; }
    public List<String[]> getGpLinks() { return gpLinks; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", (Object) null);
        json.put("ChildObjects", List.copyOf(childObjects));
        json.put("Trusts", List.copyOf(trusts));
        json.put("Links", List.copyOf(links));
        json.put("GPOChanges", Map.of(
                "AffectedComputers", List.of(),
                "AffectedUsers", List.of(),
                "DcomUsers", List.of(),
                "LocalAdmins", List.of(),
                "PSRemoteUsers", List.of(),
                "RemoteDesktopUsers", List.of()));
        return json;
    }

    static void parseGpLinks(Map<String, List<String>> entry, List<String[]> target) {
        String raw = firstValue(entry, "gplink");
        if (raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split("\\[LDAP://");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String trimmed = part.strip();
            if (trimmed.endsWith("]")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            String[] dnAndOptions = trimmed.split(";", 2);
            if (dnAndOptions.length == 2) {
                target.add(new String[]{
                        dnAndOptions[0].toUpperCase(Locale.ROOT),
                        dnAndOptions[1]
                });
            }
        }
    }

    private static String resolveFunctionalLevel(String value) {
        try {
            int level = Integer.parseInt(value.strip());
            return switch (level) {
                case 0 -> "2000 Mixed/Native";
                case 1 -> "2003 Interim";
                case 2 -> "2003";
                case 3 -> "2008";
                case 4 -> "2008 R2";
                case 5 -> "2012";
                case 6 -> "2012 R2";
                case 7 -> "2016";
                default -> "Unknown";
            };
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }
}
