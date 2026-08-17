package dev.devdoctor.engine.probe;

import dev.devdoctor.core.model.*;
import dev.devdoctor.core.spi.*;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class RedisReachabilityProbe implements DiagnosticProbe {
    public String id() { return "redis-reachability"; }
    public boolean supports(DiagnosticContext c) { return c.project().technologies().contains("REDIS") && ProbeSupport.host(c).isPresent(); }
    public ProbeSafety safety() { return ProbeSafety.SAFE_ACTIVE; }
    public ProbeResult execute(DiagnosticContext c) {
        String host = ProbeSupport.host(c).orElse("localhost"); int port = ProbeSupport.port(c).orElse(6379); Instant start = Instant.now(); boolean pong = false;
        try (Socket socket = new Socket()) { socket.connect(new InetSocketAddress(host, port), 800); socket.setSoTimeout(800); OutputStream out = socket.getOutputStream(); out.write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.US_ASCII)); out.flush(); byte[] response = socket.getInputStream().readNBytes(7); pong = new String(response, StandardCharsets.US_ASCII).startsWith("+PONG"); } catch (Exception ignored) { }
        Evidence e = new Evidence("E-PROBE-REDIS", EvidenceType.REDIS, new EvidenceSource("REDIS", host + ":" + port, Map.of()), pong ? "Redis responded to PING" : "Redis did not respond to PING", EvidenceStrength.VERY_HIGH, Sensitivity.INTERNAL, Instant.now(), Map.of("host", host, "port", port, "pong", pong));
        return new ProbeResult(id(), safety(), pong ? ProbeStatus.SUCCEEDED : ProbeStatus.FAILED, e.summary(), start, Duration.between(start, Instant.now()), List.of(e), e.metadata());
    }
}
