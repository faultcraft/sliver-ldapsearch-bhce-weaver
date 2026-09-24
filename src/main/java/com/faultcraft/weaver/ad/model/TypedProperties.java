package com.faultcraft.weaver.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Property map with Go-compatible typed defaults for missing keys.
 *
 * <p>BH-CE's Go API rejects null values. This map returns typed defaults
 * (false for bools, 0 for ints, "" for strings, [] for lists) instead of null
 * for known property keys.
 */
public final class TypedProperties extends HashMap<String, Object> {

    private static final Set<String> BOOL_KEYS = Set.of(
            "isdc", "isaclprotected", "highvalue", "haslaps", "enabled",
            "unconstraineddelegation", "dontreqpreauth", "passwordnotreqd",
            "trustedtoauth", "sensitive", "admincount", "hassidhistory",
            "isdeleted", "hasspn", "blocksinheritance", "collected",
            "pwdneverexpires", "trustsallclaims", "hasbasicconstraints",
            "enrolleesuppliessubject", "requiresmanagerapproval",
            "authenticationenabled", "isuserspecifiessanenabledcollected",
            "casecuritycollected", "enrollmentagentrestrictionscollected",
            "nosecurityextension", "subjectaltrequireupn",
            "subjectaltrequiredns", "subjectaltrequiredomaindns",
            "subjectaltrequireemail", "subjectaltrequirespn",
            "subjectrequireemail", "hascrosscertificatepair"
    );

    private static final Set<String> INT_KEYS = Set.of(
            "lastlogon", "lastlogontimestamp", "pwdlastset", "whencreated",
            "functionallevel", "basicconstraintpathlength", "schemaversion",
            "authorizedsignatures"
    );

    private static final Set<String> LIST_KEYS = Set.of(
            "serviceprincipalnames", "allowedtodelegate", "sidhistory",
            "ipaddresses", "certthumbprints", "certchain", "effectiveekus",
            "ekus", "certificateapplicationpolicy", "applicationpolicies",
            "issuancepolicies", "crosscertificatepair",
            "unresolvedpublishedtemplates"
    );

    /** Properties that should never appear in output. */
    public static final Set<String> NEVER_SHOW = Set.of(
            "ntsecuritydescriptor", "serviceprincipalname"
    );

    /** Returns the typed default for a known key, or empty string for unknown keys. */
    @Override
    public Object get(Object key) {
        Object val = super.get(key);
        if (val != null) {
            return val;
        }
        if (key instanceof String k) {
            return defaultFor(k);
        }
        return "";
    }

    /** Returns the value as a String, with empty string default. */
    public String getString(String key) {
        Object val = super.get(key);
        if (val == null) {
            return "";
        }
        return val.toString();
    }

    /** Returns the value as a boolean, with false default. */
    public boolean getBoolean(String key) {
        Object val = super.get(key);
        if (val instanceof Boolean b) {
            return b;
        }
        return false;
    }

    /** Returns the value as an int, with 0 default. */
    public int getInt(String key) {
        Object val = super.get(key);
        if (val instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    /** Returns the value as a long, with 0 default. */
    public long getLong(String key) {
        Object val = super.get(key);
        if (val instanceof Number n) {
            return n.longValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    /** Returns the value as a List of Strings, with empty list default. */
    public List<String> getStringList(String key) {
        Object val = super.get(key);
        if (val instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    /** Returns a copy of this map with NEVER_SHOW keys removed. */
    public Map<String, Object> toOutputMap() {
        Map<String, Object> out = new HashMap<>(this);
        for (String key : NEVER_SHOW) {
            out.remove(key);
        }
        return out;
    }

    private static Object defaultFor(String key) {
        if (BOOL_KEYS.contains(key)) {
            return false;
        }
        if (INT_KEYS.contains(key)) {
            return 0;
        }
        if (LIST_KEYS.contains(key)) {
            return List.of();
        }
        return "";
    }
}
