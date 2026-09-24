package com.faultcraft.weaver.sliver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SliverLogParserTest {

    private static final String CMD_ENTRY =
            """
            {"level":"info","type":"command","msg":"sa-ldapsearch \\"(objectClass=user)\\"","time":"2026-01-15T10:00:00Z"}""";

    private static final String SUCCESS_ENTRY =
            """
            {"level":"info","msg":"Successfully executed sa-ldapsearch","time":"2026-01-15T10:00:01Z"}""";

    private static final String OUTPUT_ENTRY =
            """
            {"level":"info","msg":"Got output:\\n--------------------\\ndistinguishedName: CN=admin,DC=test,DC=local\\nsAMAccountName: admin\\n--------------------\\n[+] Retrieved 1 results","time":"2026-01-15T10:00:02Z"}""";

    @Test
    void parseLog_validSequence_extractsBlock(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("test.json");
        Files.writeString(log,
                CMD_ENTRY.strip() + "\n"
                + SUCCESS_ENTRY.strip() + "\n"
                + OUTPUT_ENTRY.strip() + "\n");

        List<String> blocks = SliverLogParser.parseLog(log, null);

        assertEquals(1, blocks.size(), "Should extract one block");
        assertTrue(blocks.get(0).contains("sAMAccountName: admin"),
                "Block should contain parsed attributes");
        assertTrue(blocks.get(0).contains("--------------------"),
                "Block should contain separators");
    }

    @Test
    void parseLog_multipleCommands_extractsAll(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("test.json");
        String secondCmd = """
                {"level":"info","type":"command","msg":"sa-ldapsearch \\"(objectClass=computer)\\"","time":"2026-01-15T10:01:00Z"}""";
        String secondSuccess = """
                {"level":"info","msg":"Successfully executed sa-ldapsearch","time":"2026-01-15T10:01:01Z"}""";
        String secondOutput = """
                {"level":"info","msg":"Got output:\\n--------------------\\ndistinguishedName: CN=DC01,DC=test,DC=local\\nsAMAccountName: DC01$\\n--------------------\\n[+] Retrieved 1 results","time":"2026-01-15T10:01:02Z"}""";

        Files.writeString(log,
                CMD_ENTRY.strip() + "\n"
                + SUCCESS_ENTRY.strip() + "\n"
                + OUTPUT_ENTRY.strip() + "\n"
                + secondCmd.strip() + "\n"
                + secondSuccess.strip() + "\n"
                + secondOutput.strip() + "\n");

        List<String> blocks = SliverLogParser.parseLog(log, null);
        assertEquals(2, blocks.size(), "Should extract two blocks");
    }

    @Test
    void parseLog_nonLdapCommand_ignored(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("test.json");
        String otherCmd = """
                {"level":"info","type":"command","msg":"whoami","time":"2026-01-15T10:00:00Z"}""";
        String otherSuccess = """
                {"level":"info","msg":"Successfully executed whoami","time":"2026-01-15T10:00:01Z"}""";
        String otherOutput = """
                {"level":"info","msg":"Got output:\\nNT AUTHORITY\\\\SYSTEM","time":"2026-01-15T10:00:02Z"}""";

        Files.writeString(log,
                otherCmd.strip() + "\n"
                + otherSuccess.strip() + "\n"
                + otherOutput.strip() + "\n");

        List<String> blocks = SliverLogParser.parseLog(log, null);
        assertTrue(blocks.isEmpty(), "Non-ldapsearch commands should not produce blocks");
    }

    @Test
    void parseLog_sinceFilter_skipsOldEntries(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("test.json");
        Instant cutoff = Instant.parse("2026-01-15T10:00:30Z");

        String oldCmd = """
                {"level":"info","type":"command","msg":"sa-ldapsearch \\"(objectClass=user)\\"","time":"2026-01-15T09:00:00Z"}""";
        String oldSuccess = """
                {"level":"info","msg":"Successfully executed sa-ldapsearch","time":"2026-01-15T09:00:01Z"}""";
        String oldOutput = """
                {"level":"info","msg":"Got output:\\n--------------------\\ndistinguishedName: CN=old,DC=test,DC=local\\n--------------------","time":"2026-01-15T09:00:02Z"}""";

        String newCmd = """
                {"level":"info","type":"command","msg":"sa-ldapsearch \\"(objectClass=group)\\"","time":"2026-01-15T11:00:00Z"}""";
        String newSuccess = """
                {"level":"info","msg":"Successfully executed sa-ldapsearch","time":"2026-01-15T11:00:01Z"}""";
        String newOutput = """
                {"level":"info","msg":"Got output:\\n--------------------\\ndistinguishedName: CN=new,DC=test,DC=local\\n--------------------","time":"2026-01-15T11:00:02Z"}""";

        Files.writeString(log,
                oldCmd.strip() + "\n" + oldSuccess.strip() + "\n" + oldOutput.strip() + "\n"
                + newCmd.strip() + "\n" + newSuccess.strip() + "\n" + newOutput.strip() + "\n");

        List<String> blocks = SliverLogParser.parseLog(log, cutoff);
        assertEquals(1, blocks.size(), "Should only extract the newer block");
        assertTrue(blocks.get(0).contains("CN=new"), "Should contain the newer entry");
    }

    @Test
    void parseLog_emptyFile_returnsEmptyList(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("empty.json");
        Files.writeString(log, "");

        List<String> blocks = SliverLogParser.parseLog(log, null);
        assertTrue(blocks.isEmpty(), "Empty file should return empty list");
    }

    @Test
    void parseLog_truncatedJson_returnsPartialResults(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("truncated.json");
        Files.writeString(log,
                CMD_ENTRY.strip() + "\n"
                + SUCCESS_ENTRY.strip() + "\n"
                + OUTPUT_ENTRY.strip() + "\n"
                + "{\"level\":\"info\",\"msg\":\"trun\n");

        List<String> blocks = SliverLogParser.parseLog(log, null);
        assertEquals(1, blocks.size(), "Should extract valid block despite truncated line");
    }

    @Test
    void parseLog_binaryGarbage_returnsEmptyList(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("garbage.json");
        Files.write(log, new byte[]{0x00, 0x01, (byte) 0xFF, (byte) 0xFE, 0x42, 0x43});

        List<String> blocks = SliverLogParser.parseLog(log, null);
        assertTrue(blocks.isEmpty(), "Binary garbage should return empty list");
    }

    @Test
    void parseLog_interleavedCommands_correctAssociation(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("interleaved.json");
        String cmd1 = """
                {"level":"info","type":"command","msg":"sa-ldapsearch \\"(objectClass=user)\\"","time":"2026-01-15T10:00:00Z"}""";
        String cmd2 = """
                {"level":"info","type":"command","msg":"whoami","time":"2026-01-15T10:00:01Z"}""";
        String success1 = """
                {"level":"info","msg":"Successfully executed sa-ldapsearch","time":"2026-01-15T10:00:02Z"}""";
        String output1 = """
                {"level":"info","msg":"Got output:\\n--------------------\\ndistinguishedName: CN=user1,DC=test,DC=local\\n--------------------","time":"2026-01-15T10:00:03Z"}""";
        String success2 = """
                {"level":"info","msg":"Successfully executed whoami","time":"2026-01-15T10:00:04Z"}""";
        String output2 = """
                {"level":"info","msg":"Got output:\\nNT AUTHORITY\\\\SYSTEM","time":"2026-01-15T10:00:05Z"}""";

        Files.writeString(log,
                cmd1.strip() + "\n" + cmd2.strip() + "\n"
                + success1.strip() + "\n" + output1.strip() + "\n"
                + success2.strip() + "\n" + output2.strip() + "\n");

        List<String> blocks = SliverLogParser.parseLog(log, null);
        assertEquals(1, blocks.size(), "Only sa-ldapsearch output should be captured");
        assertTrue(blocks.get(0).contains("user1"), "Should capture the ldapsearch output");
    }

    @Test
    void resolveLogPaths_singleFile_returnsSinglePath(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("test.json");
        Files.writeString(log, "{}");

        List<Path> paths = SliverLogParser.resolveLogPaths(log);
        assertEquals(1, paths.size());
        assertEquals(log, paths.get(0));
    }

    @Test
    void resolveLogPaths_directory_returnsJsonFiles(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.json"), "{}");
        Files.writeString(tempDir.resolve("b.log"), "{}");
        Files.writeString(tempDir.resolve("c.jsonl"), "{}");
        Files.writeString(tempDir.resolve("readme.txt"), "text");
        Files.writeString(tempDir.resolve("d.xml"), "<xml/>");

        List<Path> paths = SliverLogParser.resolveLogPaths(tempDir);
        assertEquals(3, paths.size(), "Should find .json, .log, and .jsonl files");
    }

    @Test
    void parseSince_dateOnly_returnsMidnightUtc() {
        Instant result = SliverLogParser.parseSince("2026-01-15");
        assertEquals(Instant.parse("2026-01-15T00:00:00Z"), result);
    }

    @Test
    void parseSince_isoDatetime_parsesCorrectly() {
        Instant result = SliverLogParser.parseSince("2026-01-15T10:30:00Z");
        assertEquals(Instant.parse("2026-01-15T10:30:00Z"), result);
    }

    @Test
    void parseLog_emptyOutput_skipped(@TempDir Path tempDir) throws IOException {
        Path log = tempDir.resolve("test.json");
        String emptyOutput = """
                {"level":"info","msg":"Got output:","time":"2026-01-15T10:00:02Z"}""";

        Files.writeString(log,
                CMD_ENTRY.strip() + "\n"
                + SUCCESS_ENTRY.strip() + "\n"
                + emptyOutput.strip() + "\n");

        List<String> blocks = SliverLogParser.parseLog(log, null);
        assertTrue(blocks.isEmpty(), "Empty output should not produce a block");
    }
}
