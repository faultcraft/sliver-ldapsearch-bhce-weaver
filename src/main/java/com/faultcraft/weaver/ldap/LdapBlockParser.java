package com.faultcraft.weaver.ldap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.faultcraft.weaver.util.Log;

/** Parses raw ldapsearch text output into structured attribute maps. */
public final class LdapBlockParser {

    private static final Pattern SEPARATOR = Pattern.compile("-{20,}");
    private static final Pattern RETRIEVED = Pattern.compile(
            "(?i)retr(?:ie|ei)ved \\d+ results?");
    private static final String[] STATUS_PREFIXES = {"[*]", "[+]", "[-]", "[!]", "Search returned"};

    private LdapBlockParser() {}

    /**
     * Parses raw ldapsearch text into a list of attribute maps.
     *
     * <p>Each block is delimited by 20+ hyphens. Returns a list of maps where
     * keys are lowercase attribute names and values are lists of attribute values.
     * Multi-valued attributes appear as multiple entries in the value list.
     *
     * @param text raw ldapsearch output text (separators, key:value lines, status lines)
     * @return list of parsed LDAP entries, each as a map of attribute name to values
     */
    public static List<Map<String, List<String>>> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<Map<String, List<String>>> entries = new ArrayList<>();
        String[] blocks = SEPARATOR.split(text);

        for (String block : blocks) {
            block = block.strip();
            if (block.isEmpty()) {
                continue;
            }
            if (RETRIEVED.matcher(block).matches()) {
                continue;
            }

            Map<String, List<String>> attrs = parseBlock(block);
            if (!attrs.isEmpty()) {
                entries.add(attrs);
            }
        }

        return List.copyOf(entries);
    }

    private static Map<String, List<String>> parseBlock(String block) {
        Map<String, List<String>> attrs = new LinkedHashMap<>();

        for (String line : block.split("\n")) {
            line = line.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (RETRIEVED.matcher(line).matches()) {
                continue;
            }
            if (isStatusLine(line)) {
                continue;
            }

            int colonPos = line.indexOf(':');
            if (colonPos < 0) {
                continue;
            }

            String key = line.substring(0, colonPos).strip().toLowerCase(Locale.ROOT);
            String value = line.substring(colonPos + 1).strip();

            if (key.isEmpty()) {
                continue;
            }

            attrs.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return attrs;
    }

    private static boolean isStatusLine(String line) {
        for (String prefix : STATUS_PREFIXES) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
