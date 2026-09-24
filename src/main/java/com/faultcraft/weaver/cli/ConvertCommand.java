package com.faultcraft.weaver.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.faultcraft.weaver.ad.classify.ObjectClassifier;
import com.faultcraft.weaver.ad.resolve.RelationshipResolver;
import com.faultcraft.weaver.bloodhound.upload.BhceUploadClient;
import com.faultcraft.weaver.bloodhound.writer.BhceJsonWriter;
import com.faultcraft.weaver.ldap.LdapBlockParser;
import com.faultcraft.weaver.merge.DnMerger;
import com.faultcraft.weaver.sliver.SliverLogParser;
import com.faultcraft.weaver.util.Log;

/** Converts Sliver sa-ldapsearch output to BloodHound CE v6 JSON. */
@Command(
    name = "convert",
    description = "Convert Sliver sa-ldapsearch output to BloodHound CE v6 JSON."
)
public final class ConvertCommand implements Runnable {

    @Option(names = {"-i", "--input"}, required = true,
            description = "Sliver JSON console log file or directory of log files.")
    private Path input;

    @Option(names = {"-o", "--output"}, defaultValue = "./output",
            description = "Output directory for BloodHound JSON files (default: ./output).")
    private Path output;

    @Option(names = {"-p", "--properties-level"}, defaultValue = "All",
            description = "Properties detail level: Standard, Member, All (default: All).")
    private String propertiesLevel;

    @Option(names = {"--since"},
            description = "Only process log entries on or after this date (YYYY-MM-DD or ISO datetime).")
    private String since;

    @Option(names = {"--domain"},
            description = "Only process entries belonging to this domain (DNS name or DN suffix).")
    private String domain;

    @Option(names = {"--debug"},
            description = "Enable debug logging.")
    private boolean debug;

    @Option(names = {"--zip"},
            description = "Compress output into a single zip file.")
    private boolean zip;

    @Option(names = {"--bh-url"},
            description = "BloodHound CE server URL (e.g. http://localhost:8080).")
    private String bhUrl;

    @Option(names = {"--bh-token-id"},
            description = "BH-CE API token ID.")
    private String bhTokenId;

    @Option(names = {"--bh-token-key"},
            description = "BH-CE API token key (base64).")
    private String bhTokenKey;

    @Override
    public void run() {
        if (debug) {
            Log.setDebug(true);
        }

        if (!Files.exists(input)) {
            Log.error("Input path does not exist: %s", input);
            throw new CommandLine.ExecutionException(
                new CommandLine(this), "Input path does not exist: " + input);
        }

        if (!validatePropertiesLevel()) {
            throw new CommandLine.ExecutionException(
                new CommandLine(this),
                "Invalid properties level: " + propertiesLevel
                    + ". Must be Standard, Member, or All.");
        }

        if (!validateBhceArgs()) {
            throw new CommandLine.ExecutionException(
                new CommandLine(this),
                "--bh-url, --bh-token-id, and --bh-token-key must all be provided together.");
        }

        Log.info("Input: %s", input);
        Log.info("Output: %s", output);

        try {
            List<Path> outputFiles = runPipeline();
            if (bhUrl != null) {
                uploadResults(outputFiles);
            }
        } catch (Exception e) {
            Log.error("Pipeline failed: %s", e.getMessage());
            throw new CommandLine.ExecutionException(new CommandLine(this), e.getMessage(), e);
        }
    }

    private List<Path> runPipeline() throws Exception {
        Instant sinceInstant = since != null ? SliverLogParser.parseSince(since) : null;

        List<Path> logPaths = SliverLogParser.resolveLogPaths(input);
        Log.info("Found %d log file(s)", logPaths.size());

        List<String> rawBlocks = new ArrayList<>();
        for (Path logPath : logPaths) {
            rawBlocks.addAll(SliverLogParser.parseLog(logPath, sinceInstant));
        }
        Log.info("Extracted %d sa-ldapsearch result blocks", rawBlocks.size());

        if (rawBlocks.isEmpty()) {
            Log.info("No LDAP data found, writing empty output");
            return BhceJsonWriter.write(List.of(), output, zip);
        }

        List<Map<String, List<String>>> entries = new ArrayList<>();
        for (String block : rawBlocks) {
            entries.addAll(LdapBlockParser.parse(block));
        }
        Log.info("Parsed %d LDAP entries", entries.size());

        List<Map<String, List<String>>> merged = DnMerger.merge(entries, domain);
        Log.info("Merged to %d unique objects", merged.size());

        var classifier = new ObjectClassifier();
        var classified = classifier.classify(merged);

        var resolver = new RelationshipResolver();
        resolver.resolve(classified);

        return BhceJsonWriter.write(classified.objects(), output, zip);
    }

    private void uploadResults(List<Path> files) throws Exception {
        var client = new BhceUploadClient(bhUrl, bhTokenId, bhTokenKey);
        client.uploadFiles(files);
    }

    public Path getInput() { return input; }
    public Path getOutput() { return output; }
    public String getPropertiesLevel() { return propertiesLevel; }
    public String getSince() { return since; }
    public String getDomain() { return domain; }
    public boolean isDebug() { return debug; }

    private boolean validatePropertiesLevel() {
        return "Standard".equalsIgnoreCase(propertiesLevel)
            || "Member".equalsIgnoreCase(propertiesLevel)
            || "All".equalsIgnoreCase(propertiesLevel);
    }

    private boolean validateBhceArgs() {
        boolean anySet = bhUrl != null || bhTokenId != null || bhTokenKey != null;
        boolean allSet = bhUrl != null && bhTokenId != null && bhTokenKey != null;
        return !anySet || allSet;
    }
}
