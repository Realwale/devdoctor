package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DatabaseReachabilityProbe implements DiagnosticProbe {
    public String id() { return "database-reachability"; }
    public boolean supports(DiagnosticContext c) { return (c.project().technologies().contains("POSTGRESQL") || c.project().technologies().contains("MYSQL")) && ProbeSupport.host(c).isPresent() && ProbeSupport.port(c).isPresent(); }
    public ProbeSafety safety() { return ProbeSafety.SAFE_ACTIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        String host = ProbeSupport.host(c).orElseThrow(); int port = ProbeSupport.port(c).orElseThrow(); Instant start = Instant.now(); boolean reachable = false;
        try (Socket socket = new Socket()) { socket.connect(new InetSocketAddress(host, port), 800); reachable = true; } catch (Exception ignored) { }
        Evidence e = new Evidence("E-PROBE-DATABASE", EvidenceType.DATABASE, new EvidenceSource("DATABASE_TCP", host + ":" + port, Map.of()), reachable ? "Database TCP endpoint is reachable" : "Database TCP endpoint is unreachable", EvidenceStrength.HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("host", host, "port", port, "reachable", reachable, "scope", "TCP reachability only; no authentication or SQL executed"));
        return new ProbeResult(id(), safety(), reachable ? ProbeStatus.SUCCEEDED : ProbeStatus.FAILED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), e.metadata());
    }
}
