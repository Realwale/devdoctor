package dev.devdoctor.core.model;

import java.util.List;
import java.util.Map;

public record ProjectProfile(String root, String language, String javaVersion, String buildTool,
                             String framework, String frameworkVersion, String webStack,
                             List<String> technologies, Map<String, String> attributes) {
    public ProjectProfile {
        root = ModelSupport.required(root, "root");
        language = language == null ? "UNKNOWN" : language;
        javaVersion = javaVersion == null ? "UNKNOWN" : javaVersion;
        buildTool = buildTool == null ? "UNKNOWN" : buildTool;
        framework = framework == null ? "NONE" : framework;
        frameworkVersion = frameworkVersion == null ? "UNKNOWN" : frameworkVersion;
        webStack = webStack == null ? "UNKNOWN" : webStack;
        technologies = ModelSupport.list(technologies);
        attributes = ModelSupport.map(attributes);
    }
}
