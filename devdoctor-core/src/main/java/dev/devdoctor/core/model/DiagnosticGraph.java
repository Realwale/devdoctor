package dev.devdoctor.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record DiagnosticGraph(List<GraphNode> nodes, List<GraphEdge> edges) {
    public DiagnosticGraph {
        nodes = ModelSupport.list(nodes);
        edges = ModelSupport.list(edges);
        Set<String> ids = new HashSet<>();
        for (GraphNode node : nodes) {
            if (!ids.add(node.id())) throw new IllegalArgumentException("duplicate graph node: " + node.id());
        }
        for (GraphEdge edge : edges) {
            if (!ids.contains(edge.from()) || !ids.contains(edge.to())) {
                throw new IllegalArgumentException("dangling graph edge: " + edge.from() + " -> " + edge.to());
            }
        }
    }
}
