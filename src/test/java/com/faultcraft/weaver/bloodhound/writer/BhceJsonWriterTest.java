package com.faultcraft.weaver.bloodhound.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.faultcraft.weaver.ad.model.BloodHoundDomain;
import com.faultcraft.weaver.ad.model.BloodHoundGroup;
import com.faultcraft.weaver.ad.model.BloodHoundObject;
import com.faultcraft.weaver.ad.model.BloodHoundUser;

class BhceJsonWriterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void write_singleType_correctFormat(@TempDir Path tempDir) throws IOException {
        var user = makeUser("S-1-5-21-1-2-3-1001");

        List<Path> files = BhceJsonWriter.write(List.of(user), tempDir, false);

        assertEquals(1, files.size());
        assertTrue(files.get(0).getFileName().toString().startsWith("users_"));

        var root = readJson(files.get(0));
        assertEquals("users", root.get("meta").get("type").asText());
        assertEquals(1, root.get("meta").get("count").asInt());
        assertEquals(6, root.get("meta").get("version").asInt());
        assertEquals(0, root.get("meta").get("methods").asInt());
        assertEquals(1, root.get("data").size());
    }

    @Test
    void write_multipleTypes_separateFiles(@TempDir Path tempDir) throws IOException {
        var user = makeUser("S-1-5-21-1-2-3-1001");
        var group = makeGroup("S-1-5-21-1-2-3-513");
        var domain = makeDomain("S-1-5-21-1-2-3");

        List<Path> files = BhceJsonWriter.write(
                List.of(user, group, domain), tempDir, false);

        assertEquals(3, files.size());
    }

    @Test
    void write_domainVersion5(@TempDir Path tempDir) throws IOException {
        var domain = makeDomain("S-1-5-21-1-2-3");

        List<Path> files = BhceJsonWriter.write(List.of(domain), tempDir, false);

        var root = readJson(files.get(0));
        assertEquals(5, root.get("meta").get("version").asInt());
        assertEquals("domains", root.get("meta").get("type").asText());
    }

    @Test
    void write_emptyList_noFiles(@TempDir Path tempDir) throws IOException {
        List<Path> files = BhceJsonWriter.write(List.of(), tempDir, false);
        assertTrue(files.isEmpty());
    }

    @Test
    void write_zipMode_singleZipFile(@TempDir Path tempDir) throws IOException {
        var user = makeUser("S-1-5-21-1-2-3-1001");

        List<Path> files = BhceJsonWriter.write(List.of(user), tempDir, true);

        assertEquals(1, files.size());
        assertTrue(files.get(0).getFileName().toString().endsWith(".zip"));
        assertTrue(Files.exists(files.get(0)));
    }

    @Test
    void write_objectIdentifierPresent(@TempDir Path tempDir) throws IOException {
        var user = makeUser("S-1-5-21-1-2-3-1001");

        List<Path> files = BhceJsonWriter.write(List.of(user), tempDir, false);

        var root = readJson(files.get(0));
        var obj = root.get("data").get(0);
        assertEquals("S-1-5-21-1-2-3-1001", obj.get("ObjectIdentifier").asText());
    }

    @Test
    void write_outputDirectoryCreated(@TempDir Path tempDir) throws IOException {
        Path nested = tempDir.resolve("sub").resolve("dir");
        var user = makeUser("S-1-5-21-1-2-3-1001");

        List<Path> files = BhceJsonWriter.write(List.of(user), nested, false);

        assertFalse(files.isEmpty());
        assertTrue(Files.exists(nested));
    }

    @Test
    void validate_nullProperty_countsWarning() {
        var json = Map.<String, Object>of(
                "ObjectIdentifier", "S-1-5-21-1-2-3-1001",
                "Properties", new java.util.HashMap<>(Map.of("name", "test")));
        @SuppressWarnings("unchecked")
        var props = (Map<String, Object>) json.get("Properties");
        props.put("badkey", null);

        int warnings = BhceJsonWriter.validate(json, "users", 0);

        assertEquals(1, warnings);
    }

    @Test
    void validate_emptyObjectIdentifier_countsWarning() {
        var json = Map.<String, Object>of(
                "ObjectIdentifier", "",
                "Properties", Map.of("name", "test"));

        int warnings = BhceJsonWriter.validate(json, "users", 0);

        assertEquals(1, warnings);
    }

    @Test
    void validate_validObject_noWarnings() {
        var json = Map.<String, Object>of(
                "ObjectIdentifier", "S-1-5-21-1-2-3-1001",
                "Properties", Map.of("name", "test", "domain", "TEST.LOCAL"));

        int warnings = BhceJsonWriter.validate(json, "users", 0);

        assertEquals(0, warnings);
    }

    @Test
    void validateOutputDir_pathTraversal_throws() {
        assertThrows(IOException.class,
                () -> BhceJsonWriter.validateOutputDir(Path.of("/tmp/../etc/shadow")));
    }

    private static BloodHoundUser makeUser(String sid) {
        return BloodHoundUser.fromEntry(Map.of(
                "distinguishedname", List.of("CN=jdoe,DC=test,DC=local"),
                "objectsid", List.of(sid),
                "samaccountname", List.of("jdoe")));
    }

    private static BloodHoundGroup makeGroup(String sid) {
        return BloodHoundGroup.fromEntry(Map.of(
                "distinguishedname", List.of("CN=Group1,DC=test,DC=local"),
                "objectsid", List.of(sid),
                "samaccountname", List.of("Group1")));
    }

    private static BloodHoundDomain makeDomain(String sid) {
        return BloodHoundDomain.fromEntry(Map.of(
                "distinguishedname", List.of("DC=test,DC=local"),
                "objectsid", List.of(sid)));
    }

    private static com.fasterxml.jackson.databind.JsonNode readJson(Path file) throws IOException {
        return MAPPER.readTree(file.toFile());
    }
}
