package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.ad.model.pki.PkiCaFlags;

public final class BloodHoundEnterpriseCA extends BloodHoundObject {

    private Map<String, Object> containedBy = Map.of();
    private final List<String> rawCertTemplateNames = new ArrayList<>();
    private final List<Map<String, Object>> enabledCertTemplates = new ArrayList<>();
    private String hostingComputer = "";

    @Override
    public String objectType() {
        return "enterprisecas";
    }

    public static BloodHoundEnterpriseCA fromEntry(Map<String, List<String>> entry) {
        var ca = new BloodHoundEnterpriseCA();
        ca.populateCommon(entry);

        TypedProperties p = ca.getProperties();

        p.put("casecuritycollected", false);
        p.put("enrollmentagentrestrictionscollected", false);
        p.put("isuserspecifiessanenabledcollected", false);
        p.put("unresolvedpublishedtemplates", new ArrayList<>());

        String caName = firstValue(entry, "name");
        p.put("caname", caName);
        String domain = p.getString("domain");
        if (!caName.isEmpty() && !domain.isEmpty()) {
            p.put("name", (caName + "@" + domain).toUpperCase(Locale.ROOT));
        }

        String dnsHost = firstValue(entry, "dnshostname");
        if (!dnsHost.isEmpty()) {
            p.put("dnshostname", dnsHost);
        }

        String flagStr = firstValue(entry, "flags");
        if (!flagStr.isEmpty()) {
            try {
                int flags = Integer.parseInt(flagStr.strip());
                p.put("flags", String.join(", ", PkiCaFlags.decompose(flags)));
            } catch (NumberFormatException ignored) {}
        }

        CertificateParser.parseCaCertificate(entry, p);

        String templates = firstValue(entry, "certificatetemplates");
        if (!templates.isEmpty()) {
            for (String t : templates.split(", ")) {
                String trimmed = t.strip();
                if (!trimmed.isEmpty()) {
                    ca.rawCertTemplateNames.add(trimmed);
                }
            }
            p.put("certificatetemplates", List.copyOf(ca.rawCertTemplateNames));
        }

        return ca;
    }

    public List<String> getRawCertTemplateNames() { return rawCertTemplateNames; }
    public List<Map<String, Object>> getEnabledCertTemplates() { return enabledCertTemplates; }
    public void setHostingComputer(String sid) { this.hostingComputer = sid; }
    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        json.put("HostingComputer", hostingComputer);
        json.put("CARegistryData", null);
        json.put("EnabledCertTemplates", List.copyOf(enabledCertTemplates));
        return json;
    }
}
