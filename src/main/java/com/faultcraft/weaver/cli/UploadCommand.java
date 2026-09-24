package com.faultcraft.weaver.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.faultcraft.weaver.bloodhound.upload.BhceUploadClient;
import com.faultcraft.weaver.util.Log;

/** Uploads previously generated BH-CE JSON files to a BloodHound CE server. */
@Command(
    name = "upload",
    description = "Upload BH-CE JSON output to a BloodHound CE server."
)
public final class UploadCommand implements Runnable {

    @Option(names = {"-o", "--output"}, required = true,
            description = "Directory containing BH-CE JSON files to upload.")
    private Path output;

    @Option(names = {"--bh-url"}, required = true,
            description = "BloodHound CE server URL (e.g. http://localhost:8080).")
    private String bhUrl;

    @Option(names = {"--bh-token-id"}, required = true,
            description = "BH-CE API token ID.")
    private String bhTokenId;

    @Option(names = {"--bh-token-key"}, required = true,
            description = "BH-CE API token key (base64).")
    private String bhTokenKey;

    @Option(names = {"--debug"},
            description = "Enable debug logging.")
    private boolean debug;

    @Override
    public void run() {
        if (debug) {
            Log.setDebug(true);
        }

        if (!Files.isDirectory(output)) {
            Log.error("Output directory does not exist: %s", output);
            throw new CommandLine.ExecutionException(
                new CommandLine(this), "Output directory does not exist: " + output);
        }

        Log.info("Uploading from: %s", output);
        Log.info("BH-CE URL: %s", bhUrl);

        try {
            List<Path> files;
            try (Stream<Path> stream = Files.list(output)) {
                files = stream
                        .filter(p -> p.toString().endsWith(".json")
                                || p.toString().endsWith(".zip"))
                        .sorted()
                        .toList();
            }

            if (files.isEmpty()) {
                Log.warn("No JSON or zip files found in %s", output);
                return;
            }

            var client = new BhceUploadClient(bhUrl, bhTokenId, bhTokenKey);
            client.uploadFiles(files);

        } catch (Exception e) {
            Log.error("Upload failed: %s", e.getMessage());
            throw new CommandLine.ExecutionException(new CommandLine(this), e.getMessage(), e);
        }
    }

    public Path getOutput() { return output; }
}
