package dev.devdoctor.core.model;

import java.util.Map;

public record EvidenceSource(String kind, String locator, Map<String, String> attributes) {
    public EvidenceSource {
        kind = ModelSupport.required(kind, "kind");
        locator = ModelSupport.required(locator, "locator");
        attributes = ModelSupport.map(attributes);
    }
}
