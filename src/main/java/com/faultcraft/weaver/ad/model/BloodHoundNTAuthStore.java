package com.faultcraft.weaver.ad.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.util.Log;

public final class BloodHoundNTAuthStore extends BloodHoundObject {

    private Map<String, Object> containedBy = Map.of();

    @Override
    public String objectType() {
        return "ntauthstores";
    }

    public static BloodHoundNTAuthStore fromEntry(Map<String, List<String>> entry) {
        var store = new BloodHoundNTAuthStore();
        store.populateCommon(entry);

        TypedProperties p = store.getProperties();

        String name = firstValue(entry, "name");
        String domain = p.getString("domain");
        if (!name.isEmpty() && !domain.isEmpty()) {
            p.put("name", (name + "@" + domain).toUpperCase(Locale.ROOT));
        }

        parseCertThumbprints(entry, p);

        return store;
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

    private static void parseCertThumbprints(Map<String, List<String>> entry,
            TypedProperties p) {
        String raw = firstValue(entry, "cacertificate");
        if (raw.isEmpty()) {
            return;
        }
        List<String> thumbprints = new ArrayList<>();
        for (String cert : raw.split(", ")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(cert.strip());
                String hex = CertificateParser.sha1Hex(bytes);
                if (!hex.isEmpty()) {
                    thumbprints.add(hex.toUpperCase(Locale.ROOT));
                }
            } catch (IllegalArgumentException e) {
                Log.warn("Skipping malformed cACertificate");
            }
        }
        p.put("certthumbprints", thumbprints);
    }
}
