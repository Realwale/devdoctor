package dev.devdoctor.agent;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.HashMap;
import java.util.Map;
import jdk.jfr.FlightRecorder;

/** Installs the bounded, local-only DevDoctor span processor in the OpenTelemetry Java agent. */
public final class DevDoctorAutoConfiguration implements AutoConfigurationCustomizerProvider {
    @Override
    public void customize(AutoConfigurationCustomizer configuration) {
        System.setProperty("devdoctor.agent.active", "true");
        FlightRecorder.register(DevDoctorTransactionEvent.class);
        new DevDoctorTransactionEvent().commit();
        configuration.addTracerProviderCustomizer((provider, ignored) ->
                        provider.addSpanProcessor(new DevDoctorSpanProcessor()))
                .addSamplerCustomizer((ignored, configurationProperties) -> new DevDoctorSampler())
                .addPropertiesSupplier(this::safeDefaults);
    }

    private Map<String, String> safeDefaults() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("otel.traces.exporter", "none");
        defaults.put("otel.metrics.exporter", "none");
        defaults.put("otel.logs.exporter", "none");
        defaults.put("otel.propagators", "none");
        return defaults;
    }
}
