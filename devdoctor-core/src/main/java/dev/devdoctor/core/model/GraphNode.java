package dev.devdoctor.core.model;

public record GraphNode(String id, GraphNodeType type, String label) {
    public GraphNode {
        id = ModelSupport.required(id, "id");
        if (type == null) throw new IllegalArgumentException("type is required");
        label = ModelSupport.required(label, "label");
    }
}
