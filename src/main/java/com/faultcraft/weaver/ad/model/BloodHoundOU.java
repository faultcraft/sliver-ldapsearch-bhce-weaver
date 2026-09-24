package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BloodHoundOU extends BloodHoundObject {

    private final List<Map<String, Object>> childObjects = new ArrayList<>();
    private final List<Map<String, Object>> links = new ArrayList<>();
    private Map<String, Object> containedBy = Map.of();

    /* Mutable: raw GPLink DN+options pairs for resolution */
    private final List<String[]> gpLinks = new ArrayList<>();

    @Override
    public String objectType() {
        return "ous";
    }

    public static BloodHoundOU fromEntry(Map<String, List<String>> entry) {
        var ou = new BloodHoundOU();
        ou.populateCommon(entry);

        TypedProperties p = ou.getProperties();

        String dn = firstValue(entry, "distinguishedname");
        String domain = p.getString("domain");

        String ouName = firstValue(entry, "ou");
        if (ouName.isEmpty()) {
            ouName = firstValue(entry, "name");
        }
        if (ouName.isEmpty() && !dn.isEmpty()) {
            String firstRdn = dn.contains(",") ? dn.substring(0, dn.indexOf(',')) : dn;
            ouName = firstRdn.toUpperCase(Locale.ROOT).startsWith("OU=")
                    ? firstRdn.substring(3) : firstRdn;
        }
        p.put("name", (ouName + "@" + domain).toUpperCase(Locale.ROOT));

        p.put("blocksinheritance", "1".equals(firstValue(entry, "gpoptions")));
        p.put("highvalue", false);

        BloodHoundDomain.parseGpLinks(entry, ou.gpLinks);

        return ou;
    }

    public List<Map<String, Object>> getChildObjects() { return childObjects; }
    public List<Map<String, Object>> getLinks() { return links; }
    public List<String[]> getGpLinks() { return gpLinks; }
    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        json.put("ChildObjects", List.copyOf(childObjects));
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
}
