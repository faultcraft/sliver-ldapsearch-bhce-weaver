package com.faultcraft.weaver.sliver;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.faultcraft.weaver.util.Log;

/** Parses Sliver C2 NDJSON console logs and extracts sa-ldapsearch BOF output blocks. */
public final class SliverLogParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SUCCESS_PATTERN =
            Pattern.compile("Successfully executed (\\S+)");

    private SliverLogParser() {}

    /**
     * Extracts raw LDAP text blocks from a Sliver NDJSON console log.
     *
     * <p>Each returned string contains the raw ldapsearch output for one sa-ldapsearch
     * command (entry separators, key:value lines, and result trailer included).
     *
     * @param path  path to a Sliver JSON console log file
     * @param since if non-null, entries with timestamps before this instant are skipped
     * @return list of raw LDAP text blocks
     */
    public static List<String> parseLog(Path path, Instant since) {
        Map<String, Deque<PendingCommand>> pending = new HashMap<>();
        PendingCommand activeCmd = null;
        List<String> results = new ArrayList<>();
        int skipped = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                rawLine = rawLine.strip();
                if (rawLine.isEmpty()) {
                    continue;
                }

                JsonNode entry;
                try {
                    entry = MAPPER.readTree(rawLine);
                } catch (Exception e) {
                    continue;
                }

                if (!entry.isObject()) {
                    continue;
                }

                if (since != null) {
                    Instant ts = parseTimestamp(entry.path("time").asText(""));
                    if (ts != null && ts.isBefore(since)) {
                        skipped++;
                        continue;
                    }
                }

                String msg = entry.path("msg").asText("");
                String entryType = entry.path("type").asText("");

                if ("command".equals(entryType)) {
                    String cmdText = msg.strip();
                    String cmdName = extractCommandName(cmdText);
                    String cmdLower = cmdName.toLowerCase(java.util.Locale.ROOT);
                    pending.computeIfAbsent(cmdLower, k -> new ArrayDeque<>())
                           .addLast(new PendingCommand(cmdText));
                    continue;
                }

                Matcher successMatch = SUCCESS_PATTERN.matcher(msg);
                if (successMatch.find()) {
                    String cmdName = successMatch.group(1).toLowerCase(java.util.Locale.ROOT);
                    Deque<PendingCommand> queue = pending.get(cmdName);
                    if (queue != null && !queue.isEmpty()) {
                        activeCmd = queue.pollFirst();
                        if (queue.isEmpty()) {
                            pending.remove(cmdName);
                        }
                    }
                    continue;
                }

                if (msg.startsWith("Got output:")) {
                    String body = unescape(msg.substring("Got output:".length())).strip();
                    if (body.isEmpty()) {
                        activeCmd = null;
                        continue;
                    }
                    if (activeCmd != null
                            && activeCmd.text().toLowerCase(java.util.Locale.ROOT)
                                        .contains("sa-ldapsearch")) {
                        results.add(body);
                    }
                    activeCmd = null;
                }
            }
        } catch (IOException e) {
            Log.warn("Failed to read log file %s: %s", path, e.getMessage());
            return List.copyOf(results);
        }

        var msgParts = new StringBuilder();
        msgParts.append(String.format("Extracted %d sa-ldapsearch result blocks from %s",
                results.size(), path.getFileName()));
        if (skipped > 0) {
            msgParts.append(String.format(" (%d entries skipped, before --since cutoff)", skipped));
        }
        Log.info(msgParts.toString());
        return List.copyOf(results);
    }

    /**
     * Yields Sliver JSON log file paths from a file or directory.
     *
     * <p>If the path is a file, returns a single-element list. If a directory,
     * returns all files with .json, .log, .jsonl, or no extension, sorted by name.
     */
    public static List<Path> resolveLogPaths(Path inputPath) {
        if (Files.isRegularFile(inputPath)) {
            return List.of(inputPath);
        }
        if (!Files.isDirectory(inputPath)) {
            Log.warn("Input path is neither a file nor a directory: %s", inputPath);
            return List.of();
        }
        List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.endsWith(".json") || name.endsWith(".log")
                        || name.endsWith(".jsonl") || !name.contains(".")) {
                    paths.add(p);
                }
            }
        } catch (IOException e) {
            Log.warn("Failed to list directory %s: %s", inputPath, e.getMessage());
        }
        paths.sort(Path::compareTo);
        return List.copyOf(paths);
    }

    /**
     * Parses a --since value into an Instant.
     *
     * <p>Accepts YYYY-MM-DD (interpreted as midnight UTC) or ISO-8601 datetime.
     */
    public static Instant parseSince(String value) {
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return Instant.parse(value.contains("T") && !value.endsWith("Z") && !value.contains("+")
                ? value + "Z" : value);
    }

    private static Instant parseTimestamp(String tsStr) {
        if (tsStr == null || tsStr.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(tsStr.contains("T") && !tsStr.endsWith("Z")
                    && !tsStr.contains("+") && tsStr.indexOf('-', 10) < 0
                    ? tsStr + "Z" : tsStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String extractCommandName(String cmdText) {
        if (cmdText.isEmpty()) {
            return "";
        }
        int space = cmdText.indexOf(' ');
        return space < 0 ? cmdText : cmdText.substring(0, space);
    }

    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        var sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private record PendingCommand(String text) {}
}
