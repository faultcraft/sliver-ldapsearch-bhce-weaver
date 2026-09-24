package com.faultcraft.weaver.ad.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BloodHoundIssuancePolicy extends BloodHoundObject {

    private Map<String, Object> containedBy = Map.of();
    private String groupLink = "";
    private String resolvedGroupLink = "";

    @Override
    public String objectType() {
        return "issuancepolicies";
    }

    public static BloodHoundIssuancePolicy fromEntry(Map<String, List<String>> entry) {
        var policy = new BloodHoundIssuancePolicy();
        policy.populateCommon(entry);

        TypedProperties p = policy.getProperties();

        String displayName = firstValue(entry, "displayname");
        if (!displayName.isEmpty()) {
            p.put("displayname", displayName);
            String domain = p.getString("domain");
            if (!domain.isEmpty()) {
                p.put("name", (displayName + "@" + domain).toUpperCase(Locale.ROOT));
            }
        }

        String oid = firstValue(entry, "mspki-cert-template-oid");
        if (!oid.isEmpty()) {
            p.put("certtemplateoid", oid);
        }

        String link = firstValue(entry, "msds-oidtogrouplink");
        if (!link.isEmpty()) {
            policy.groupLink = link.toUpperCase(Locale.ROOT);
        }

        return policy;
    }

    public String getGroupLink() { return groupLink; }
    public void setResolvedGroupLink(String sid) { this.resolvedGroupLink = sid; }
    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        String linkId = !resolvedGroupLink.isEmpty() ? resolvedGroupLink
                : (groupLink.isEmpty() ? null : groupLink);
        json.put("GroupLink", linkId);
        return json;
    }
}
