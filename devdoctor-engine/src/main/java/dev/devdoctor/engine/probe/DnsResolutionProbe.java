package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class DnsResolutionProbe implements DiagnosticProbe {
    public String id() { return "dns-resolution"; }
    public boolean supports(DiagnosticContext context) { return ProbeSupport.host(context).isPresent(); }
    public ProbeSafety safety() { return ProbeSafety.SAFE_ACTIVE; }
    public ProbeResult execute(DiagnosticContext context) {
        Instant start = Instant.now(); String host = ProbeSupport.host(context).orElseThrow();
        try {
            List<String> addresses = Arrays.stream(InetAddress.getAllByName(host)).map(InetAddress::getHostAddress).toList();
            Evidence e = new Evidence("E-PROBE-DNS", EvidenceType.DNS, new EvidenceSource("DNS", host, Map.of()),
                    "Hostname resolved successfully: " + host, EvidenceStrength.HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("host", host, "addresses", addresses));
            return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), Map.of("host", host, "addresses", addresses));
        } catch (Exception failure) {
            Evidence e = new Evidence("E-PROBE-DNS", EvidenceType.DNS, new EvidenceSource("DNS", host, Map.of()),
                    "Hostname resolution failed: " + host, EvidenceStrength.VERY_HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("host", host, "resolved", false));
            return new ProbeResult(id(), safety(), ProbeStatus.FAILED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), Map.of("host", host));
        }
    }
}
