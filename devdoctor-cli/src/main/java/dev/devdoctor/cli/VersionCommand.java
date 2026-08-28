package dev.devdoctor.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

@Command(name = "version", description = "Print DevDoctor version.")
final class VersionCommand implements Callable<Integer> { public Integer call() { System.out.println("DevDoctor 0.1.1"); return 0; } }
