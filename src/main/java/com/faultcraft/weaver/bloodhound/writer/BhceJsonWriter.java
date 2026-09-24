package com.faultcraft.weaver.bloodhound.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.util.Log;

/**
 * Writes classified AD objects to BH-CE v6 JSON files, one per object type.
 *
 * <p>Each file contains:
 * <pre>
 * {
 *   "data": [ ... objects ... ],
 *   "meta": { "type": "users", "count": N, "methods": 0, "version": 6 }
 * }
 * </pre>
 */
public final class BhceJsonWriter {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, Integer> TYPE_VERSIONS = Map.ofEntries(
            Map.entry("domains", 5),
            Map.entry("gpos", 5),
            Map.entry("users", 6),
            Map.entry("computers", 6),
            Map.entry("groups", 6),
            Map.entry("ous", 6),
            Map.entry("containers", 6),
            Map.entry("certtemplates", 6),
            Map.entry("enterprisecas", 6),
            Map.entry("rootcas", 6),
            Map.entry("aiacas", 6),
            Map.entry("ntauthstores", 6),
            Map.entry("issuancepolicies", 6));

    private BhceJsonWriter() {}

    /**
     * Writes all objects to per-type JSON files in the output directory.
     *
     * @param objects all classified and resolved objects
     * @param outDir  output directory (created if absent)
     * @param zip     true to produce a single zip archive
     * @return list of written file paths
     */
    public static List<Path> write(List<BloodHoundObject> objects,
            Path outDir, boolean zip) throws IOException {
        validateOutputDir(outDir);
        Files.createDirectories(outDir);

        String timestamp = LocalDateTime.now().format(TS_FORMAT);
        Map<String, List<BloodHoundObject>> byType = groupByType(objects);

        List<Path> written = new ArrayList<>();
        for (var entry : byType.entrySet()) {
            Path file = writeTypeFile(entry.getKey(), entry.getValue(),
                    outDir, timestamp);
            written.add(file);
        }

        Log.info("Wrote %d JSON files (%d objects) to %s",
                written.size(), objects.size(), outDir);

        if (zip && !written.isEmpty()) {
            Path zipPath = outDir.resolve("bloodhound_" + timestamp + ".zip");
            zipFiles(written, zipPath);
            for (Path f : written) {
                Files.deleteIfExists(f);
            }
            written.clear();
            written.add(zipPath);
            Log.info("Compressed output to %s", zipPath);
        }

        return written;
    }

    private static Path writeTypeFile(String type, List<BloodHoundObject> objects,
            Path outDir, String timestamp) throws IOException {

        int version = TYPE_VERSIONS.getOrDefault(type, 6);
        int warnCount = 0;

        var data = new ArrayList<Map<String, Object>>(objects.size());
        for (int i = 0; i < objects.size(); i++) {
            Map<String, Object> json = objects.get(i).toJson();
            warnCount += validate(json, type, i);
            data.add(json);
        }

        var output = new LinkedHashMap<String, Object>();
        output.put("data", data);
        output.put("meta", Map.of(
                "type", type,
                "count", data.size(),
                "methods", 0,
                "version", version));

        Path file = outDir.resolve(type + "_" + timestamp + ".json");
        MAPPER.writeValue(file.toFile(), output);

        if (warnCount > 0) {
            Log.warn("%s: %d validation warnings", type, warnCount);
        }

        return file;
    }

    @SuppressWarnings("unchecked")
    static int validate(Map<String, Object> json, String type, int index) {
        int errors = 0;

        Object id = json.get("ObjectIdentifier");
        if (id == null || (id instanceof String s && s.isEmpty())) {
            String name = resolveName(json);
            Log.warn("%s %s: ObjectIdentifier is empty", type, name);
            errors++;
        }

        Object propsObj = json.get("Properties");
        if (propsObj instanceof Map<?, ?> props) {
            for (var entry : ((Map<String, Object>) props).entrySet()) {
                if (entry.getValue() == null) {
                    String name = resolveName(json);
                    Log.warn("%s %s: Properties[%s]=null", type, name, entry.getKey());
                    errors++;
                }
            }
        }

        return errors;
    }

    @SuppressWarnings("unchecked")
    private static String resolveName(Map<String, Object> json) {
        Object propsObj = json.get("Properties");
        if (propsObj instanceof Map<?, ?> props) {
            Object name = ((Map<String, Object>) props).get("name");
            if (name instanceof String s && !s.isEmpty()) {
                return s;
            }
            Object dn = ((Map<String, Object>) props).get("distinguishedname");
            if (dn instanceof String s && !s.isEmpty()) {
                return s;
            }
        }
        return "index=" + json.hashCode();
    }

    private static Map<String, List<BloodHoundObject>> groupByType(
            List<BloodHoundObject> objects) {
        var map = new LinkedHashMap<String, List<BloodHoundObject>>();
        for (BloodHoundObject obj : objects) {
            map.computeIfAbsent(obj.objectType(), k -> new ArrayList<>()).add(obj);
        }
        return map;
    }

    static void validateOutputDir(Path outDir) throws IOException {
        String raw = outDir.toString();
        if (raw.contains("..")) {
            throw new IOException("Path traversal detected: " + outDir);
        }
    }

    private static void zipFiles(List<Path> files, Path zipPath) throws IOException {
        try (OutputStream fos = Files.newOutputStream(zipPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (Path file : files) {
                zos.putNextEntry(new ZipEntry(file.getFileName().toString()));
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
    }
}
