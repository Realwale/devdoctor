package dev.devdoctor.engine.observe;

public record StackFrame(String className, String method, String file, Integer line, boolean applicationFrame) {}
