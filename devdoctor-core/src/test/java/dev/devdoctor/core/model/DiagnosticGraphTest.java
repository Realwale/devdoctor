package dev.devdoctor.core.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticGraphTest {
    @Test void rejectsDanglingEdges() {
        var nodes = List.of(new GraphNode("E-1", GraphNodeType.EVIDENCE, "safe evidence"));
        var edge = new GraphEdge("E-1", "H-404", RelationshipType.SUPPORTS, "supports");
        assertThatThrownBy(() -> new DiagnosticGraph(nodes, List.of(edge)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dangling");
    }
}
