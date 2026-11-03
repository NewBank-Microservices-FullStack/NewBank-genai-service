package com.newbank.microservices.genai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * #14: metrics for model latency, estimated token usage, and tool-call
 * success/failure. Traces flow via the OTel bridge (see application.yml)
 * across the tool → downstream call chain.
 */
@Component
public class ModelMetrics {

    private final Timer latency;
    private final Counter toolSuccess;
    private final Counter toolFailure;
    private final Counter tokens;

    public ModelMetrics(MeterRegistry registry) {
        this.latency = Timer.builder("genai.model.latency").description("Chat model latency").register(registry);
        this.toolSuccess = Counter.builder("genai.tool.calls").tag("outcome", "success").register(registry);
        this.toolFailure = Counter.builder("genai.tool.calls").tag("outcome", "failure").register(registry);
        this.tokens = Counter.builder("genai.tokens").description("Estimated tokens").register(registry);
    }

    public Timer.Sample start() {
        return Timer.start();
    }

    public void stopLatency(Timer.Sample sample) {
        sample.stop(latency);
    }

    public void toolOk() {
        toolSuccess.increment();
    }

    public void toolFail() {
        toolFailure.increment();
    }

    public void addTokens(long n) {
        tokens.increment(n);
    }
}
