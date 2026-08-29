package dev.devdoctor.agent;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import java.nio.file.Files;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;

class DevDoctorSpanProcessorTest {
    @Test
    void samplesOnlyDuringBoundedJfrWindow() {
        var sampler = new DevDoctorSampler();
        assertThat(sampler.shouldSample(Context.root(), "0123456789abcdef0123456789abcdef", "test",
                SpanKind.SERVER, io.opentelemetry.api.common.Attributes.empty(), java.util.List.of())
                .getDecision()).isEqualTo(SamplingDecision.DROP);
        try (var recording = new Recording()) {
            recording.enable("dev.devdoctor.Transaction");
            recording.start();
            assertThat(sampler.shouldSample(Context.root(), "0123456789abcdef0123456789abcdef", "test",
                    SpanKind.SERVER, io.opentelemetry.api.common.Attributes.empty(), java.util.List.of())
                    .getDecision()).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
        }
    }

    @Test
    void emitsOutcomeEventWithoutRequestContentOrExternalExporter() throws Exception {
        var target = Files.createTempFile("devdoctor-span-", ".jfr");
        try (var recording = new Recording();
             var provider = SdkTracerProvider.builder().addSpanProcessor(new DevDoctorSpanProcessor()).build()) {
            recording.enable("dev.devdoctor.Transaction");
            recording.start();
            var span = provider.tracerBuilder("test").build().spanBuilder("GET /orders/{id}")
                    .setSpanKind(SpanKind.SERVER).startSpan();
            span.setAttribute(AttributeKey.stringKey("http.request.method"), "GET");
            span.setAttribute(AttributeKey.stringKey("http.route"), "/orders/{id}");
            span.setAttribute(AttributeKey.longKey("http.response.status_code"), 500L);
            span.recordException(new IllegalStateException("password=do-not-store"));
            span.setStatus(StatusCode.ERROR);
            span.end();
            recording.stop();
            recording.dump(target);
        }
        try (var events = new RecordingFile(target)) {
            var event = events.readEvent();
            assertThat(event.getEventType().getName()).isEqualTo("dev.devdoctor.Transaction");
            assertThat(event.getBoolean("failed")).isTrue();
            assertThat(event.getLong("httpStatus")).isEqualTo(500);
            assertThat(event.getString("httpRoute")).isEqualTo("/orders/{id}");
            assertThat(event.getString("exceptionMessage")).doesNotContain("do-not-store").contains("[REDACTED]");
        } finally {
            Files.deleteIfExists(target);
        }
    }
}
