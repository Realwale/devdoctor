package dev.devdoctor.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "evidence", description = "Explain an evidence item from a saved session.", mixinStandardHelpOptions = true)
final class EvidenceCommand implements Callable<Integer> {
    @Parameters(index = "0") String evidenceId;
    @Option(names = "--session") String sessionId;
    @Option(names = "--project", defaultValue = ".") Path project;
    public Integer call() throws Exception {
        var session = new SessionStore().load(project.toAbsolutePath().normalize(), sessionId);
        var evidence = session.evidence().stream().filter(e -> e.id().equals(evidenceId)).findFirst();
        if (evidence.isEmpty()) { System.err.println("Evidence not found: " + evidenceId); return 1; }
        var e = evidence.get(); System.out.println("Evidence: " + e.id() + "\nType: " + e.type() + "\nSource: " + e.source().kind() + " / " + e.source().locator() + "\nObservation: " + e.summary() + "\nStrength: " + e.strength() + "\nSensitivity: " + e.sensitivity() + "\nCollected: " + e.collectedAt());
        System.out.println("Used by: " + session.graph().edges().stream().filter(edge -> edge.from().equals(e.id())).map(edge -> edge.relationship() + " " + edge.to()).toList()); return 0;
    }
}
