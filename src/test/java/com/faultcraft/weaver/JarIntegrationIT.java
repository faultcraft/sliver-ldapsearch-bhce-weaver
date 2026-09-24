package com.faultcraft.weaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests that launch the packaged JAR as a subprocess.
 *
 * <p>Both the standard and ProGuard-obfuscated JARs are tested with
 * identical assertions. These tests catch packaging errors, obfuscation
 * breakage, manifest issues, and resource loading failures that unit
 * tests cannot detect.</p>
 */
class JarIntegrationIT {

    private static final String STANDARD_JAR = "target/bhce-weaver-0.1.1.jar";
    private static final String OBFUSCATED_JAR = "target/bhce-weaver-0.1.1-obfuscated.jar";

    @BeforeAll
    static void ensureJarsExist() {
        assertTrue(Files.exists(Path.of(STANDARD_JAR)),
                "Standard JAR not found -- run mvn package first");
        assertTrue(Files.exists(Path.of(OBFUSCATED_JAR)),
                "Obfuscated JAR not found -- run mvn package first");
    }

    @ParameterizedTest(name = "help [{0}]")
    @ValueSource(strings = {STANDARD_JAR, OBFUSCATED_JAR})
    void help_noArgs_showsUsage(String jar) throws Exception {
        ProcessResult result = run(jar, "--help");
        assertEquals(0, result.exitCode(), "exit code for --help");
        assertTrue(result.stdout().contains("weaver"),
                "help output should mention 'weaver'");
        assertTrue(result.stdout().contains("convert"),
                "help output should list convert subcommand");
        assertTrue(result.stdout().contains("upload"),
                "help output should list upload subcommand");
    }

    @ParameterizedTest(name = "version [{0}]")
    @ValueSource(strings = {STANDARD_JAR, OBFUSCATED_JAR})
    void version_flag_printsVersion(String jar) throws Exception {
        ProcessResult result = run(jar, "--version");
        assertEquals(0, result.exitCode(), "exit code for --version");
        assertTrue(result.stdout().contains("0.1.1"),
                "version output should contain 0.1.1");
    }

    @ParameterizedTest(name = "convert help [{0}]")
    @ValueSource(strings = {STANDARD_JAR, OBFUSCATED_JAR})
    void convertHelp_showsFlags(String jar) throws Exception {
        ProcessResult result = run(jar, "convert", "--help");
        String output = result.stdout() + result.stderr();
        assertTrue(output.contains("--input"),
                "convert help should show --input flag");
        assertTrue(output.contains("--output"),
                "convert help should show --output flag");
        assertTrue(output.contains("--zip"),
                "convert help should show --zip flag");
    }

    @ParameterizedTest(name = "convert missing input [{0}]")
    @ValueSource(strings = {STANDARD_JAR, OBFUSCATED_JAR})
    void convert_missingInput_nonZeroExit(String jar) throws Exception {
        ProcessResult result = run(jar, "convert",
                "-i", "/nonexistent/path/no-such-file.json");
        assertTrue(result.exitCode() != 0,
                "should fail with nonexistent input");
    }

    @ParameterizedTest(name = "convert real data [{0}]")
    @ValueSource(strings = {STANDARD_JAR, OBFUSCATED_JAR})
    void convert_sliverFixture_producesJson(String jar,
            @TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("sliver.json");
        Path outputDir = tempDir.resolve("output");
        Files.writeString(inputFile, buildSliverLog());

        ProcessResult result = run(jar, "convert",
                "-i", inputFile.toString(),
                "-o", outputDir.toString());

        assertEquals(0, result.exitCode(),
                "convert should succeed; stderr: " + result.stderr());
        assertTrue(Files.isDirectory(outputDir),
                "output directory should exist");

        List<Path> jsonFiles;
        try (var stream = Files.list(outputDir)) {
            jsonFiles = stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();
        }
        assertFalse(jsonFiles.isEmpty(),
                "should produce at least one JSON file");

        boolean hasUsers = jsonFiles.stream()
                .anyMatch(p -> p.getFileName().toString().startsWith("users_"));
        assertTrue(hasUsers, "should produce a users JSON file");
    }

    @ParameterizedTest(name = "convert zip [{0}]")
    @ValueSource(strings = {STANDARD_JAR, OBFUSCATED_JAR})
    void convert_withZip_producesSingleZip(String jar,
            @TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("sliver.json");
        Path outputDir = tempDir.resolve("output");
        Files.writeString(inputFile, buildSliverLog());

        ProcessResult result = run(jar, "convert",
                "-i", inputFile.toString(),
                "-o", outputDir.toString(),
                "--zip");

        assertEquals(0, result.exitCode(),
                "convert --zip should succeed; stderr: " + result.stderr());

        List<Path> zipFiles;
        try (var stream = Files.list(outputDir)) {
            zipFiles = stream
                    .filter(p -> p.toString().endsWith(".zip"))
                    .toList();
        }
        assertEquals(1, zipFiles.size(),
                "should produce exactly one zip file");
    }

    @ParameterizedTest(name = "upload help [{0}]")
    @ValueSource(strings = {STANDARD_JAR, OBFUSCATED_JAR})
    void uploadHelp_showsFlags(String jar) throws Exception {
        ProcessResult result = run(jar, "upload", "--help");
        String output = result.stdout() + result.stderr();
        assertTrue(output.contains("--bh-url"),
                "upload help should show --bh-url flag");
    }

    @Test
    void standardAndObfuscated_sameJsonOutput(@TempDir Path tempDir)
            throws Exception {
        Path inputFile = tempDir.resolve("sliver.json");
        Files.writeString(inputFile, buildSliverLog());

        Path stdOut = tempDir.resolve("std-output");
        Path obfOut = tempDir.resolve("obf-output");

        ProcessResult stdResult = run(STANDARD_JAR, "convert",
                "-i", inputFile.toString(), "-o", stdOut.toString());
        ProcessResult obfResult = run(OBFUSCATED_JAR, "convert",
                "-i", inputFile.toString(), "-o", obfOut.toString());

        assertEquals(0, stdResult.exitCode(), "standard JAR should succeed");
        assertEquals(0, obfResult.exitCode(), "obfuscated JAR should succeed");

        List<String> stdFiles = listSorted(stdOut);
        List<String> obfFiles = listSorted(obfOut);

        assertEquals(stdFiles.size(), obfFiles.size(),
                "both JARs should produce same number of output files");

        for (int i = 0; i < stdFiles.size(); i++) {
            String stdPrefix = stdFiles.get(i).replaceAll("_\\d{8}_\\d{6}", "");
            String obfPrefix = obfFiles.get(i).replaceAll("_\\d{8}_\\d{6}", "");
            assertEquals(stdPrefix, obfPrefix,
                    "output file types should match");
        }
    }

    private static List<String> listSorted(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static ProcessResult run(String jar, String... args)
            throws Exception {
        String[] cmd = new String[args.length + 3];
        cmd[0] = "java";
        cmd[1] = "-jar";
        cmd[2] = jar;
        System.arraycopy(args, 0, cmd, 3, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(Path.of(".").toAbsolutePath().toFile());
        Process proc = pb.start();

        String stdout = new String(proc.getInputStream().readAllBytes());
        String stderr = new String(proc.getErrorStream().readAllBytes());
        boolean finished = proc.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            proc.destroyForcibly();
            throw new AssertionError("Process timed out after 30s");
        }
        return new ProcessResult(proc.exitValue(), stdout, stderr);
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}

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
                {"level":"info","msg":"Got output:\\n""" + ldapData
                + "\",\"time\":\"2026-01-15T10:00:02Z\"}\n";
    }
}
