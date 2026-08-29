package dev.devdoctor.agent;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

/** Records spans only while a DevDoctor JFR observation window is active. */
final class DevDoctorSampler implements Sampler {
    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
                                       Attributes attributes, List<LinkData> parentLinks) {
        return new DevDoctorTransactionEvent().isEnabled()
                ? SamplingResult.recordAndSample() : SamplingResult.drop();
    }

    @Override public String getDescription() { return "DevDoctorJfrWindowSampler"; }
}
