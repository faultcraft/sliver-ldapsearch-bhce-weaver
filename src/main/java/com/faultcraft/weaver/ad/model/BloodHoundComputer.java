package com.faultcraft.weaver.ad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.faultcraft.weaver.util.SidParser;
import com.faultcraft.weaver.util.TimestampParser;

public final class BloodHoundComputer extends BloodHoundObject {

    private static final Map<String, Object> NOT_COLLECTED = Map.of(
            "Results", List.of(), "Collected", false, "FailureReason", "");

    private String primaryGroupSid = "";
    private Map<String, Object> containedBy = Map.of();
    private List<Map<String, Object>> resolvedDelegate = null;

    /* Mutable: populated during relationship resolution */
    private final List<String> memberOfDns = new ArrayList<>();

    @Override
    public String objectType() {
        return "computers";
    }

    public static BloodHoundComputer fromEntry(Map<String, List<String>> entry) {
        var comp = new BloodHoundComputer();
        comp.populateCommon(entry);

        TypedProperties p = comp.getProperties();

        parseName(entry, p);
        parseUac(entry, p);
        parseTimestamps(entry, p);
        parseLaps(entry, p);
        parseOs(entry, p);
        parseSpns(entry, p);
        parseSidHistory(entry, p);
        parseDelegation(entry, p);
        parsePrimaryGroup(entry, comp);
        BloodHoundUser.parseMemberOfDns(entry, comp.memberOfDns);

        putIfPresent(entry, p, "email", "email");

        return comp;
    }

    @Override
    public Map<String, Object> toJson() {
        TypedProperties p = getProperties();
        var json = baseJson();
        json.put("ContainedBy", containedBy);
        json.put("PrimaryGroupSID", primaryGroupSid);
        json.put("AllowedToDelegate", resolvedDelegate != null
                ? resolvedDelegate : p.getStringList("allowedtodelegate"));
        json.put("AllowedToAct", List.of());
        json.put("HasSIDHistory", p.getStringList("sidhistory"));
        json.put("DumpSMSAPassword", List.of());
        json.put("Sessions", NOT_COLLECTED);
        json.put("PrivilegedSessions", NOT_COLLECTED);
        json.put("RegistrySessions", NOT_COLLECTED);
        json.put("LocalGroups", List.of());
        json.put("UserRights", List.of());
        json.put("Status", null);
        json.put("IsDC", p.getBoolean("isdc"));
        return json;
    }

    public String getPrimaryGroupSid() { return primaryGroupSid; }
    public List<String> getMemberOfDns() { return memberOfDns; }
    public void setContainedBy(Map<String, Object> val) { this.containedBy = val; }
    public void setAllowedToDelegate(List<Map<String, Object>> val) { this.resolvedDelegate = val; }

    private static void parseName(Map<String, List<String>> entry, TypedProperties p) {
        String hostname = firstValue(entry, "dnshostname");
        if (!hostname.isEmpty()) {
            p.put("dnshostname", hostname);
            p.put("name", hostname.toUpperCase(Locale.ROOT));
            return;
        }
        String sam = firstValue(entry, "samaccountname");
        if (!sam.isEmpty()) {
            String domain = p.getString("domain");
            String name = sam.endsWith("$") ? sam.substring(0, sam.length() - 1) : sam;
            p.put("name", (name + "." + domain).toUpperCase(Locale.ROOT));
        }
    }

    private static void parseUac(Map<String, List<String>> entry, TypedProperties p) {
        int uac = UacFlags.parse(firstValue(entry, "useraccountcontrol"));
        p.put("enabled", UacFlags.isEnabled(uac));
        p.put("unconstraineddelegation", (uac & UacFlags.UNCONSTRAINED_DELEGATION) != 0);
        p.put("trustedtoauth", (uac & UacFlags.TRUSTED_TO_AUTH) != 0);
        p.put("isdc", UacFlags.isDomainController(uac));
    }

    private static void parseTimestamps(Map<String, List<String>> entry, TypedProperties p) {
        p.put("lastlogon", TimestampParser.fromFiletime(firstValue(entry, "lastlogon")));
        p.put("lastlogontimestamp",
                TimestampParser.fromFiletime(firstValue(entry, "lastlogontimestamp")));
        p.put("pwdlastset", TimestampParser.fromFiletime(firstValue(entry, "pwdlastset")));
    }

    private static void parseLaps(Map<String, List<String>> entry, TypedProperties p) {
        boolean hasLaps = !firstValue(entry, "ms-mcs-admpwdexpirationtime").isEmpty()
                || !firstValue(entry, "mslaps-passwordexpirationtime").isEmpty();
        p.put("haslaps", hasLaps);
    }

    private static void parseOs(Map<String, List<String>> entry, TypedProperties p) {
        String os = firstValue(entry, "operatingsystem");
        String sp = firstValue(entry, "operatingsystemservicepack");
        if (!os.isEmpty()) {
            p.put("operatingsystem", sp.isEmpty() ? os : os + " " + sp);
        }
    }

    private static void parseSpns(Map<String, List<String>> entry, TypedProperties p) {
        List<String> spns = allValues(entry, "serviceprincipalname");
        if (spns.size() == 1 && spns.get(0).contains(", ")) {
            spns = List.of(spns.get(0).split(", "));
        }
        p.put("serviceprincipalnames", new ArrayList<>(spns));
    }

    private static void parseSidHistory(Map<String, List<String>> entry, TypedProperties p) {
        List<String> raw = allValues(entry, "sidhistory");
        List<String> parsed = raw.stream()
                .map(SidParser::parseAny).filter(s -> !s.isEmpty()).toList();
        p.put("sidhistory", new ArrayList<>(parsed));
    }

    private static void parseDelegation(Map<String, List<String>> entry, TypedProperties p) {
        List<String> vals = allValues(entry, "msds-allowedtodelegateto");
        if (vals.size() == 1 && vals.get(0).contains(", ")) {
            vals = List.of(vals.get(0).split(", "));
        }
        p.put("allowedtodelegate", new ArrayList<>(vals));
    }

    private static void parsePrimaryGroup(Map<String, List<String>> entry,
            BloodHoundComputer comp) {
        String pgid = firstValue(entry, "primarygroupid");
        if (!pgid.isEmpty()) {
            String sid = comp.getObjectIdentifier();
            int lastDash = sid.lastIndexOf('-');
            if (lastDash > 0) {
                comp.primaryGroupSid = sid.substring(0, lastDash + 1) + pgid;
            }
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
