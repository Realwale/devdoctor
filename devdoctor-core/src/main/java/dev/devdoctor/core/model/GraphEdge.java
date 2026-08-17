package dev.devdoctor.core.model;

public record GraphEdge(String from, String to, RelationshipType relationship, String rationale) {
    public GraphEdge {
        from = ModelSupport.required(from, "from");
        to = ModelSupport.required(to, "to");
        if (relationship == null) throw new IllegalArgumentException("relationship is required");
        rationale = ModelSupport.required(rationale, "rationale");
    }
}
