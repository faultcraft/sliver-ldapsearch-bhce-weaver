package com.faultcraft.weaver;

import picocli.CommandLine;

import com.faultcraft.weaver.cli.ConvertCommand;
import com.faultcraft.weaver.cli.UploadCommand;

/** Entry point for BHCE Weaver -- converts Sliver sa-ldapsearch output to BloodHound CE v6 JSON. */
@CommandLine.Command(
    name = "weaver",
    mixinStandardHelpOptions = true,
    version = "bhce-weaver 0.1.1",
    description = "Converts multi-pass Sliver sa-ldapsearch BOF output into BloodHound CE v6 JSON.",
    subcommands = {ConvertCommand.class, UploadCommand.class}
)
public final class Main implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
