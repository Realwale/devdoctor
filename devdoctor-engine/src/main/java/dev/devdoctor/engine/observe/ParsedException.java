package dev.devdoctor.engine.observe;

import java.util.List;

public record ParsedException(String exceptionClass, String message, List<StackFrame> frames, boolean suppressed) {
    public ParsedException { frames = frames == null ? List.of() : List.copyOf(frames); }
}
