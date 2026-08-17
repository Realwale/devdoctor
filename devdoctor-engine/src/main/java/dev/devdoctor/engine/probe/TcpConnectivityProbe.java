package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.DiagnosticContext;
import dev.devdoctor.core.spi.DiagnosticProbe;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TcpConnectivityProbe implements DiagnosticProbe {
    public String id() { return "tcp-connectivity"; }
    public boolean supports(DiagnosticContext context) { return ProbeSupport.host(context).isPresent() && ProbeSupport.port(context).isPresent(); }
    public ProbeSafety safety() { return ProbeSafety.SAFE_ACTIVE; }
    public ProbeResult execute(DiagnosticContext context) {
        Instant start = Instant.now(); String host = ProbeSupport.host(context).orElseThrow(); int port = ProbeSupport.port(context).orElseThrow();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 800);
            Evidence e = evidence(host, port, true, "TCP endpoint accepted a connection");
            return new ProbeResult(id(), safety(), ProbeStatus.SUCCEEDED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), Map.of("host", host, "port", port, "connected", true));
        } catch (Exception failure) {
            Evidence e = evidence(host, port, false, "TCP endpoint did not accept a connection");
            return new ProbeResult(id(), safety(), ProbeStatus.FAILED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), Map.of("host", host, "port", port, "connected", false));
        }
    }
    protected Evidence evidence(String host, int port, boolean connected, String summary) {
        return new Evidence("E-PROBE-TCP", EvidenceType.TCP, new EvidenceSource("TCP", host + ":" + port, Map.of()),
                summary + ": " + host + ":" + port, connected ? EvidenceStrength.HIGH : EvidenceStrength.VERY_HIGH,
                Sensitivity.INTERNAL, Instant.now(), Map.of("host", host, "port", port, "connected", connected));
    }
}
