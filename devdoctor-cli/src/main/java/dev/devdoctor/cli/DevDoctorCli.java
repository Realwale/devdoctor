package dev.devdoctor.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "devdoctor", description = "Evidence-based root-cause diagnostics for software failures.",
        mixinStandardHelpOptions = true, version = "DevDoctor 0.1.0",
        subcommands = {DiagnoseCommand.class, InspectCommand.class, EvidenceCommand.class, HypothesisCommand.class, BenchmarkCommand.class, VersionCommand.class})
public final class DevDoctorCli implements Runnable {
    public static void main(String[] args) { System.exit(new CommandLine(new DevDoctorCli()).execute(args)); }
    @Override public void run() { new CommandLine(this).usage(System.out); }
}
