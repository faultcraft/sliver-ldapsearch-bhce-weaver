package com.faultcraft.weaver.ad.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.util.SidParser;

public final class DomainTrust {

    private final String localDomainDn;
    private String targetDomainName = "";
    private String targetDomainSid = "";
    private boolean isTransitive = false;
    private TrustDirection trustDirection = TrustDirection.DISABLED;
    private TrustType trustType = TrustType.UNKNOWN;
    private boolean sidFilteringEnabled = true;

    private DomainTrust(String localDomainDn) {
        this.localDomainDn = localDomainDn;
    }

    public static DomainTrust fromEntry(Map<String, List<String>> entry) {
        String dn = firstValue(entry, "distinguishedname");
        String localDn = BloodHoundObject.dnToDomain(dn);
        var trust = new DomainTrust(localDn);

        String partner = firstValue(entry, "trustpartner");
        if (partner.isEmpty()) {
            partner = firstValue(entry, "name");
        }
        if (partner.isEmpty()) {
            partner = firstValue(entry, "cn");
        }
        trust.targetDomainName = partner.toUpperCase(Locale.ROOT);

        String sid = firstValue(entry, "securityidentifier");
        if (sid.isEmpty()) {
            sid = firstValue(entry, "objectsid");
        }
        trust.targetDomainSid = SidParser.parseAny(sid);

        String dirStr = firstValue(entry, "trustdirection");
        if (!dirStr.isEmpty()) {
            try {
                trust.trustDirection = TrustDirection.fromValue(
                        Integer.parseInt(dirStr.strip()));
            } catch (NumberFormatException ignored) {}
        }

        String typeStr = firstValue(entry, "trusttype");
        if (!typeStr.isEmpty()) {
            try {
                trust.trustType = TrustType.fromValue(Integer.parseInt(typeStr.strip()));
            } catch (NumberFormatException ignored) {}
        }

        String attrsStr = firstValue(entry, "trustattributes");
        if (!attrsStr.isEmpty()) {
            try {
                int attrs = Integer.parseInt(attrsStr.strip());
                trust.isTransitive = (attrs & 0x00000001) == 0;
                trust.sidFilteringEnabled = (attrs & 0x00000004) != 0;
            } catch (NumberFormatException ignored) {}
        }

        return trust;
    }

    public Map<String, Object> toJson() {
        var json = new LinkedHashMap<String, Object>();
        json.put("TargetDomainName", targetDomainName);
        json.put("TargetDomainSid", targetDomainSid);
        json.put("IsTransitive", isTransitive);
        json.put("TrustDirection", trustDirection.label());
        json.put("TrustType", trustType.label());
        json.put("SidFilteringEnabled", sidFilteringEnabled);
        return json;
    }

    public String getLocalDomainDn() { return localDomainDn; }
    public String getTargetDomainName() { return targetDomainName; }
    public String getTargetDomainSid() { return targetDomainSid; }

    private static String firstValue(Map<String, List<String>> entry, String key) {
        List<String> vals = entry.get(key);
        if (vals == null || vals.isEmpty()) {
            return "";
        }
        return vals.get(0);
    }
}
