package com.faultcraft.weaver.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine;

class ConvertCommandTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void convert_sliverLog_producesJsonFiles(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("sliver.json");
        Path outputDir = tempDir.resolve("output");

        Files.writeString(inputFile, buildSliverLog());

        int exitCode = new CommandLine(new ConvertCommand())
                .execute("-i", inputFile.toString(), "-o", outputDir.toString());

        assertEquals(0, exitCode);
        assertTrue(Files.isDirectory(outputDir));

        List<Path> jsonFiles;
        try (var stream = Files.list(outputDir)) {
            jsonFiles = stream.filter(p -> p.toString().endsWith(".json")).toList();
        }

        assertFalse(jsonFiles.isEmpty());

        boolean hasUsers = jsonFiles.stream()
                .anyMatch(p -> p.getFileName().toString().startsWith("users_"));
        assertTrue(hasUsers, "Expected a users JSON file");

        Path usersFile = jsonFiles.stream()
                .filter(p -> p.getFileName().toString().startsWith("users_"))
                .findFirst().orElseThrow();
        JsonNode root = MAPPER.readTree(usersFile.toFile());
        assertEquals("users", root.get("meta").get("type").asText());
        assertTrue(root.get("data").size() > 0);
    }

    @Test
    void convert_emptyLog_producesNoFiles(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("empty.json");
        Path outputDir = tempDir.resolve("output");

        Files.writeString(inputFile, "{\"TaskID\": 1, \"Request\": {\"Args\": [\"--help\"]},"
                + " \"Response\": {\"Response\": \"\"}}\n");

        int exitCode = new CommandLine(new ConvertCommand())
                .execute("-i", inputFile.toString(), "-o", outputDir.toString());

        assertEquals(0, exitCode);
    }

    @Test
    void convert_missingInput_exits1(@TempDir Path tempDir) {
        int exitCode = new CommandLine(new ConvertCommand())
                .execute("-i", tempDir.resolve("nonexistent.json").toString());

        assertNotEquals(0, exitCode);
    }

    @Test
    void convert_invalidPropertiesLevel_exits1(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("test.json");
        Files.writeString(inputFile, "{}");

        int exitCode = new CommandLine(new ConvertCommand())
                .execute("-i", inputFile.toString(), "-p", "InvalidLevel");

        assertNotEquals(0, exitCode);
    }

    @Test
    void convert_withZip_producesSingleZip(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("sliver.json");
        Path outputDir = tempDir.resolve("output");

        Files.writeString(inputFile, buildSliverLog());

        int exitCode = new CommandLine(new ConvertCommand())
                .execute("-i", inputFile.toString(), "-o", outputDir.toString(), "--zip");

        assertEquals(0, exitCode);

        List<Path> zipFiles;
        try (var stream = Files.list(outputDir)) {
            zipFiles = stream.filter(p -> p.toString().endsWith(".zip")).toList();
        }

        assertEquals(1, zipFiles.size());
    }

    private static String buildSliverLog() {
        String ldapData = "--------------------\\n"
                + "distinguishedName: CN=testuser,CN=Users,DC=test,DC=local\\n"
                + "objectClass: top, person, organizationalPerson, user\\n"
                + "samAccountName: testuser\\n"
                + "objectSid: S-1-5-21-1234567890-1234567890-1234567890-1001\\n"
                + "samAccountType: 805306368\\n"
                + "userAccountControl: 512\\n"
                + "--------------------\\n"
                + "distinguishedName: CN=Domain Admins,CN=Users,DC=test,DC=local\\n"
                + "objectClass: top, group\\n"
                + "samAccountName: Domain Admins\\n"
                + "objectSid: S-1-5-21-1234567890-1234567890-1234567890-512\\n"
                + "samAccountType: 268435456\\n"
                + "--------------------\\n"
                + "distinguishedName: DC=test,DC=local\\n"
                + "objectClass: top, domainDNS\\n"
                + "objectSid: S-1-5-21-1234567890-1234567890-1234567890\\n"
                + "--------------------\\n"
                + "[+] Retrieved 3 results";

        return """
                {"level":"info","type":"command","msg":"sa-ldapsearch \\"(objectClass=*)\\"","time":"2026-01-15T10:00:00Z"}
                {"level":"info","msg":"Successfully executed sa-ldapsearch","time":"2026-01-15T10:00:01Z"}
                {"level":"info","msg":"Got output:\\n""" + ldapData + "\",\"time\":\"2026-01-15T10:00:02Z\"}\n";
    }
}
