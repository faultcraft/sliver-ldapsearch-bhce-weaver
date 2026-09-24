package com.faultcraft.weaver.merge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.faultcraft.weaver.util.Log;

/** Merges LDAP entries by distinguished name across multiple query passes. */
public final class DnMerger {

    private DnMerger() {}

    /**
     * Merges entries by distinguishedName (case-insensitive).
     *
     * <p>Attributes from later entries are unioned with existing values.
     * Multi-valued attributes accumulate unique values across passes.
     *
     * @param entries list of parsed LDAP entries (from LdapBlockParser)
     * @param domain  optional domain filter (DNS form like "example.com" or DN form like
     *                "DC=example,DC=com"); null to accept all entries
     * @return list of merged entries, one per unique DN
     */
    public static List<Map<String, List<String>>> merge(
            List<Map<String, List<String>>> entries, String domain) {

        Map<String, Map<String, List<String>>> dnMap = new LinkedHashMap<>();
        int domainFiltered = 0;
        int noDn = 0;

        for (Map<String, List<String>> entry : entries) {
            List<String> dnValues = entry.get("distinguishedname");
            if (dnValues == null || dnValues.isEmpty() || dnValues.get(0).isBlank()) {
                noDn++;
                continue;
            }

            String dn = dnValues.get(0);

            if (domain != null && !dnMatchesDomain(dn, domain)) {
                domainFiltered++;
                continue;
            }

            String dnKey = dn.toLowerCase(Locale.ROOT);

            if (!dnMap.containsKey(dnKey)) {
                dnMap.put(dnKey, deepCopy(entry));
            } else {
                mergeInto(dnMap.get(dnKey), entry);
            }
        }

        List<Map<String, List<String>>> merged = new ArrayList<>(dnMap.values());

        int classifiable = countClassifiable(merged);
        int withAcl = countWithAttribute(merged, "ntsecuritydescriptor");
        int missingName = countMissingAttribute(merged, "name");

        Log.info("Merged %d entries into %d unique objects (%d classifiable, %d with ACLs, %d missing name)",
                entries.size(), merged.size(), classifiable, withAcl, missingName);

        if (noDn > 0) {
            Log.warn("%d entries skipped (no distinguishedName)", noDn);
        }
        if (domainFiltered > 0) {
            Log.info("%d entries filtered out by --domain", domainFiltered);
        }

        return List.copyOf(merged);
    }

    /** Checks if a DN belongs to the given domain suffix. */
    static boolean dnMatchesDomain(String dn, String domainSuffix) {
        String dnUpper = dn.toUpperCase(Locale.ROOT).strip();
        String suffixUpper = domainSuffix.toUpperCase(Locale.ROOT).strip();

        String target;
        if (suffixUpper.startsWith("DC=")) {
            target = suffixUpper.replaceAll("\\s*,\\s*", ",");
        } else {
            var parts = suffixUpper.split("\\.");
            var sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append("DC=").append(parts[i]);
            }
            target = sb.toString();
        }

        return dnUpper.endsWith(target);
    }

    private static void mergeInto(
            Map<String, List<String>> existing, Map<String, List<String>> incoming) {
        for (Map.Entry<String, List<String>> e : incoming.entrySet()) {
            String key = e.getKey();
            List<String> newValues = e.getValue();

            if (!existing.containsKey(key)) {
                existing.put(key, new ArrayList<>(newValues));
            } else if (!"distinguishedname".equals(key)) {
                List<String> current = existing.get(key);
                Set<String> seen = new LinkedHashSet<>(current);
                for (String v : newValues) {
                    if (!v.isEmpty() && seen.add(v)) {
                        current.add(v);
                    }
                }
            }
        }
    }

    private static Map<String, List<String>> deepCopy(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : source.entrySet()) {
            copy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return copy;
    }

    private static int countClassifiable(List<Map<String, List<String>>> entries) {
        int count = 0;
        for (Map<String, List<String>> e : entries) {
            boolean hasId = hasNonEmpty(e, "objectsid") || hasNonEmpty(e, "objectguid");
            boolean hasType = hasNonEmpty(e, "samaccounttype") || hasNonEmpty(e, "objectclass");
            if (hasId && hasType) {
                count++;
            }
        }
        return count;
    }

    private static int countWithAttribute(List<Map<String, List<String>>> entries, String attr) {
        int count = 0;
        for (Map<String, List<String>> e : entries) {
            if (hasNonEmpty(e, attr)) {
                count++;
            }
        }
        return count;
    }

    private static int countMissingAttribute(List<Map<String, List<String>>> entries, String attr) {
        int count = 0;
        for (Map<String, List<String>> e : entries) {
            if (!hasNonEmpty(e, attr)) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasNonEmpty(Map<String, List<String>> entry, String key) {
        List<String> vals = entry.get(key);
        return vals != null && !vals.isEmpty() && !vals.get(0).isBlank();
    }
}
