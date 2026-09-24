package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BloodHoundGroup extends BloodHoundObject {

    private final List<Map<String, Object>> members = new ArrayList<>();
    private Map<String, Object> containedBy = Map.of();

    /* Mutable: populated during relationship resolution */
    private final List<String> memberDns = new ArrayList<>();
    private final List<String> memberOfDns = new ArrayList<>();

    @Override
    public String objectType() {
        return "groups";
    }

    public static BloodHoundGroup fromEntry(Map<String, List<String>> entry) {
        var group = new BloodHoundGroup();
        group.populateCommon(entry);

        TypedProperties p = group.getProperties();
        String sam = firstValue(entry, "samaccountname");
        p.put("samaccountname", sam);
        if (!sam.isEmpty()) {
            String domain = p.getString("domain");
            p.put("name", (sam + "@" + domain).toUpperCase(Locale.ROOT));
        }

        String sid = group.getObjectIdentifier();
        if (sid.startsWith("S-") && sid.contains("-")) {
            int lastDash = sid.lastIndexOf('-');
            if (lastDash > 0) {
                p.put("domainsid", sid.substring(0, lastDash));
            }
        }

        p.put("admincount", "1".equals(firstValue(entry, "admincount")));

        parseMemberDns(entry, group.memberDns);
        BloodHoundUser.parseMemberOfDns(entry, group.memberOfDns);

        return group;
    }

    public List<Map<String, Object>> getMembers() { return members; }
    public List<String> getMemberDns() { return memberDns; }
    public List<String> getMemberOfDns() { return memberOfDns; }
    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        json.put("Members", List.copyOf(members));
        return json;
    }

    static void parseMemberDns(Map<String, List<String>> entry, List<String> target) {
        String raw = firstValue(entry, "member");
        if (raw.isEmpty()) {
            return;
        }
        for (String part : raw.split(", CN=")) {
            String dn = part.startsWith("CN=") ? part : "CN=" + part;
            target.add(dn.toUpperCase(Locale.ROOT));
        }
    }
}
