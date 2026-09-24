package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.util.SidParser;
import com.faultcraft.weaver.util.TimestampParser;

public final class BloodHoundUser extends BloodHoundObject {

    private String primaryGroupSid = "";
    private Map<String, Object> containedBy = Map.of();
    private List<Map<String, Object>> resolvedDelegate = null;

    /* Mutable: populated during relationship resolution */
    private final List<String> memberOfDns = new ArrayList<>();

    @Override
    public String objectType() {
        return "users";
    }

    public static BloodHoundUser fromEntry(Map<String, List<String>> entry) {
        var user = new BloodHoundUser();
        user.populateCommon(entry);

        TypedProperties p = user.getProperties();

        String sam = firstValue(entry, "samaccountname");
        p.put("samaccountname", sam);
        if (!sam.isEmpty()) {
            String domain = p.getString("domain");
            p.put("name", (sam + "@" + domain).toUpperCase(Locale.ROOT));
        }

        parseUac(entry, p);
        parseTimestamps(entry, p);
        parseSpns(entry, p);
        parseSidHistory(entry, p);
        parseDelegation(entry, p);
        parseOptionalStrings(entry, p);

        p.put("admincount", "1".equals(firstValue(entry, "admincount")));

        String pgid = firstValue(entry, "primarygroupid");
        if (!pgid.isEmpty()) {
            String sid = user.getObjectIdentifier();
            int lastDash = sid.lastIndexOf('-');
            if (lastDash > 0) {
                user.primaryGroupSid = sid.substring(0, lastDash + 1) + pgid;
            }
        }

        parseMemberOfDns(entry, user.memberOfDns);

        return user;
    }

    @Override
    public Map<String, Object> toJson() {
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        json.put("PrimaryGroupSID", primaryGroupSid);
        json.put("AllowedToDelegate", resolvedDelegate != null
                ? resolvedDelegate : getProperties().getStringList("allowedtodelegate"));
        json.put("SPNTargets", List.of());
        json.put("HasSIDHistory", getProperties().getStringList("sidhistory"));
        return json;
    }

    public String getPrimaryGroupSid() { return primaryGroupSid; }
    public List<String> getMemberOfDns() { return memberOfDns; }
    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }
    public void setAllowedToDelegate(List<Map<String, Object>> val) { this.resolvedDelegate = val; }

    private static void parseUac(Map<String, List<String>> entry, TypedProperties p) {
        int uac = UacFlags.parse(firstValue(entry, "useraccountcontrol"));
        p.put("enabled", UacFlags.isEnabled(uac));
        p.put("unconstraineddelegation", (uac & UacFlags.UNCONSTRAINED_DELEGATION) != 0);
        p.put("passwordnotreqd", (uac & UacFlags.PASSWORD_NOT_REQUIRED) != 0);
        p.put("dontreqpreauth", (uac & UacFlags.DONT_REQUIRE_PREAUTH) != 0);
        p.put("sensitive", (uac & UacFlags.SENSITIVE) != 0);
        p.put("trustedtoauth", (uac & UacFlags.TRUSTED_TO_AUTH) != 0);
        p.put("pwdneverexpires", (uac & UacFlags.PASSWORD_NEVER_EXPIRES) != 0);
    }

    private static void parseTimestamps(Map<String, List<String>> entry, TypedProperties p) {
        p.put("lastlogon", TimestampParser.fromFiletime(firstValue(entry, "lastlogon")));
        p.put("lastlogontimestamp",
                TimestampParser.fromFiletime(firstValue(entry, "lastlogontimestamp")));
        p.put("pwdlastset", TimestampParser.fromFiletime(firstValue(entry, "pwdlastset")));
    }

    private static void parseSpns(Map<String, List<String>> entry, TypedProperties p) {
        List<String> spns = allValues(entry, "serviceprincipalname");
        if (spns.size() == 1 && spns.get(0).contains(", ")) {
            spns = List.of(spns.get(0).split(", "));
        }
        p.put("serviceprincipalnames", new ArrayList<>(spns));
        p.put("hasspn", !spns.isEmpty());
    }

    private static void parseSidHistory(Map<String, List<String>> entry, TypedProperties p) {
        List<String> raw = allValues(entry, "sidhistory");
        List<String> parsed = raw.stream()
                .map(SidParser::parseAny).filter(s -> !s.isEmpty()).toList();
        p.put("sidhistory", new ArrayList<>(parsed));
        p.put("hassidhistory", !parsed.isEmpty());
    }

    private static void parseDelegation(Map<String, List<String>> entry, TypedProperties p) {
        List<String> vals = allValues(entry, "msds-allowedtodelegateto");
        if (vals.size() == 1 && vals.get(0).contains(", ")) {
            vals = List.of(vals.get(0).split(", "));
        }
        p.put("allowedtodelegate", new ArrayList<>(vals));
    }

    private static void parseOptionalStrings(Map<String, List<String>> entry, TypedProperties p) {
        putIfPresent(entry, p, "displayname", "displayname");
        putIfPresent(entry, p, "mail", "email");
        putIfPresent(entry, p, "title", "title");
        putIfPresent(entry, p, "homedirectory", "homedirectory");
        putIfPresent(entry, p, "userpassword", "userpassword");
    }

    static void parseMemberOfDns(Map<String, List<String>> entry, List<String> target) {
        String raw = firstValue(entry, "memberof");
        if (raw.isEmpty()) {
            return;
        }
        for (String part : raw.split(", CN=")) {
            String dn = part.startsWith("CN=") ? part : "CN=" + part;
            target.add(dn.toUpperCase(Locale.ROOT));
        }
    }

    private static void putIfPresent(Map<String, List<String>> entry,
            TypedProperties p, String ldapKey, String propKey) {
        String val = firstValue(entry, ldapKey);
        if (!val.isEmpty()) {
            p.put(propKey, val);
        }
    }
}
