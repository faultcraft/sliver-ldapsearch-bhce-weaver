package com.faultcraft.weaver.ad.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BloodHoundRootCA extends BloodHoundObject {

    private Map<String, Object> containedBy = Map.of();

    @Override
    public String objectType() {
        return "rootcas";
    }

    public static BloodHoundRootCA fromEntry(Map<String, List<String>> entry) {
        var ca = new BloodHoundRootCA();
        ca.populateCommon(entry);

        TypedProperties p = ca.getProperties();

        String name = firstValue(entry, "name");
        String domain = p.getString("domain");
        if (!name.isEmpty() && !domain.isEmpty()) {
            p.put("name", (name + "@" + domain).toUpperCase(Locale.ROOT));
        }

        CertificateParser.parseCaCertificate(entry, p);

        String thumbprint = p.getString("certthumbprint");
        if (!thumbprint.isEmpty()) {
            p.put("certchain", List.of(thumbprint));
        }

        return ca;
    }

    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        String domainSid = getProperties().getString("domainsid");
        if (!domainSid.isEmpty()) {
            json.put("DomainSID", domainSid);
        }
        return json;
    }
}
