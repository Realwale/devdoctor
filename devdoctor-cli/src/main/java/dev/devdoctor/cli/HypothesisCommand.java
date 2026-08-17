package dev.devdoctor.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "hypothesis", description = "Explain a tested hypothesis from a saved session.", mixinStandardHelpOptions = true)
final class HypothesisCommand implements Callable<Integer> {
    @Parameters(index = "0") String hypothesisId;
    @Option(names = "--session") String sessionId;
    @Option(names = "--project", defaultValue = ".") Path project;
    public Integer call() throws Exception {
        var session = new SessionStore().load(project.toAbsolutePath().normalize(), sessionId);
        var item = session.hypotheses().stream().filter(h -> h.id().equals(hypothesisId)).findFirst();
        if (item.isEmpty()) { System.err.println("Hypothesis not found: " + hypothesisId); return 1; }
        var h = item.get(); System.out.println(h.id() + "\n\n" + h.title() + "\nSTATUS: " + h.status() + "\nCONFIDENCE: " + h.confidence() + "\nSUPPORTING: " + h.supportingEvidence() + "\nCONTRADICTING: " + h.contradictingEvidence() + "\nTESTED BY: " + h.availableProbes() + "\nFACTORS: " + h.confidenceFactors()); return 0;
    }
}
