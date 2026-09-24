package com.faultcraft.weaver.ad.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BloodHoundGPO extends BloodHoundObject {

    private Map<String, Object> containedBy = Map.of();

    @Override
    public String objectType() {
        return "gpos";
    }

    public static BloodHoundGPO fromEntry(Map<String, List<String>> entry) {
        var gpo = new BloodHoundGPO();
        gpo.populateCommon(entry);

        TypedProperties p = gpo.getProperties();

        String displayName = firstValue(entry, "displayname");
        if (!displayName.isEmpty()) {
            String domain = p.getString("domain");
            p.put("name", (displayName + "@" + domain).toUpperCase(Locale.ROOT));
        }

        p.put("gpcpath", firstValue(entry, "gpcfilesyspath"));
        p.put("highvalue", false);

        return gpo;
    }

    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        return json;
    }
}
