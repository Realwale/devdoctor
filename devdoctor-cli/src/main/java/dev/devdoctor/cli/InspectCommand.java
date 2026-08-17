package dev.devdoctor.cli;

import dev.devdoctor.core.json.DiagnosticJson;
import dev.devdoctor.engine.observe.ProjectProfiler;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "inspect", description = "Inspect project metadata without diagnosing.", mixinStandardHelpOptions = true)
final class InspectCommand implements Callable<Integer> {
    @Parameters(index = "0", defaultValue = "project", description = "project, dependencies, environment, or git") String subject;
    @Option(names = "--project", defaultValue = ".") Path project;
    public Integer call() {
        var profile = new ProjectProfiler().profile(project);
        switch (subject) {
            case "project" -> System.out.println(profile);
            case "dependencies" -> System.out.println("Build tool: " + profile.buildTool() + "; technologies: " + profile.technologies());
            case "environment" -> System.out.println("Environment values are inspected only when referenced by project configuration; values are never printed.");
            case "git" -> System.out.println(profile.technologies().contains("GIT") ? "Git repository detected; diagnosis collects read-only correlation evidence." : "No Git repository detected.");
            default -> { System.err.println("Unknown inspection subject: " + subject); return 1; }
        }
        return 0;
    }
}
