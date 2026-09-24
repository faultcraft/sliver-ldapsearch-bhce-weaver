package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BloodHoundAIACA extends BloodHoundObject {

    private Map<String, Object> containedBy = Map.of();

    @Override
    public String objectType() {
        return "aiacas";
    }

    public static BloodHoundAIACA fromEntry(Map<String, List<String>> entry) {
        var ca = new BloodHoundAIACA();
        ca.populateCommon(entry);

        TypedProperties p = ca.getProperties();

        String name = firstValue(entry, "name");
        String domain = p.getString("domain");
        if (!name.isEmpty() && !domain.isEmpty()) {
            p.put("name", (name + "@" + domain).toUpperCase(Locale.ROOT));
        }

        String crossCert = firstValue(entry, "crosscertificatepair");
        if (!crossCert.isEmpty()) {
            p.put("crosscertificatepair", new ArrayList<>(List.of(crossCert.split(", "))));
            p.put("hascrosscertificatepair", true);
        }

        CertificateParser.parseCaCertificate(entry, p);

        return ca;
    }

    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        return json;
    }
}
