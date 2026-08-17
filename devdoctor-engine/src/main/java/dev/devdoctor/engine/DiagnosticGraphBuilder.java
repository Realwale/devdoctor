package dev.devdoctor.engine;

import dev.devdoctor.core.model.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DiagnosticGraphBuilder {
    DiagnosticGraph build(Failure failure, List<Evidence> evidence, List<Hypothesis> hypotheses, List<ProbeResult> probes,
                          List<RootCauseCandidate> roots, List<Remediation> remediations, List<VerificationStep> verification) {
        Map<String,GraphNode> nodes = new LinkedHashMap<>(); List<GraphEdge> edges = new ArrayList<>();
        add(nodes, new GraphNode(failure.id(), GraphNodeType.FAILURE, failure.summary()));
        for (Evidence item : evidence) {
            add(nodes, new GraphNode(item.id(), GraphNodeType.EVIDENCE, item.summary()));
            String sourceId = "SRC:" + item.id();
            add(nodes, new GraphNode(sourceId, GraphNodeType.SOURCE, item.source().kind() + ": " + item.source().locator()));
            edges.add(new GraphEdge(item.id(), sourceId, RelationshipType.DERIVED_FROM, "Evidence provenance"));
        }
        for (ProbeResult probe : probes) add(nodes, new GraphNode("P:" + probe.probeId(), GraphNodeType.PROBE, probe.summary()));
        for (Hypothesis h : hypotheses) {
            add(nodes, new GraphNode(h.id(), GraphNodeType.HYPOTHESIS, h.title()));
            h.supportingEvidence().stream().filter(nodes::containsKey).forEach(e -> edges.add(new GraphEdge(e, h.id(), RelationshipType.SUPPORTS, "Observed evidence supports hypothesis")));
            h.contradictingEvidence().stream().filter(nodes::containsKey).forEach(e -> edges.add(new GraphEdge(e, h.id(), RelationshipType.CONTRADICTS, "Observed evidence contradicts hypothesis")));
            h.availableProbes().stream().map(id -> "P:" + id).filter(nodes::containsKey).forEach(p -> edges.add(new GraphEdge(h.id(), p, RelationshipType.TESTED_BY, "Planner selected discriminating probe")));
        }
        for (RootCauseCandidate root : roots) edges.add(new GraphEdge(root.hypothesisId(), failure.id(), RelationshipType.CAUSES, "Highest-supported root-cause candidate"));
        for (Remediation remediation : remediations) {
            add(nodes, new GraphNode(remediation.id(), GraphNodeType.REMEDIATION, remediation.description()));
            edges.add(new GraphEdge(remediation.id(), remediation.hypothesisId(), RelationshipType.DERIVED_FROM, "Recommendation derives from supported hypothesis"));
        }
        for (VerificationStep step : verification) {
            add(nodes, new GraphNode(step.id(), GraphNodeType.VERIFICATION, step.description()));
            edges.add(new GraphEdge(step.hypothesisId(), step.id(), RelationshipType.VERIFIED_BY, "Verification plan for hypothesis"));
        }
        return new DiagnosticGraph(List.copyOf(nodes.values()), edges);
    }
    private void add(Map<String,GraphNode> nodes, GraphNode node) { nodes.putIfAbsent(node.id(), node); }
}
