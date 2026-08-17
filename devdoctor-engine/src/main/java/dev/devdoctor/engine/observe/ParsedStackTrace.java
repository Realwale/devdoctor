package dev.devdoctor.engine.observe;

import java.util.List;

public record ParsedStackTrace(String thread, List<ParsedException> causeChain) {
    public ParsedStackTrace { thread = thread == null ? "" : thread; causeChain = List.copyOf(causeChain); }
    public ParsedException rootCause() { return causeChain.isEmpty() ? null : causeChain.get(causeChain.size() - 1); }
    public List<StackFrame> applicationFrames() {
        return causeChain.stream().flatMap(e -> e.frames().stream()).filter(StackFrame::applicationFrame).toList();
    }
}
