package org.athens.network.support;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

@Component
public class MetricsCollector {
    private final Counter activeConnections;
    private final Counter totalRequests;
    private final Timer requestLatency;
    private final Counter errorCount;
    private final MeterRegistry registry;

    public MetricsCollector(MeterRegistry registry) {
        this.registry = registry;
        this.activeConnections = registry.counter("server.connections.active");
        this.totalRequests = registry.counter("server.requests.total");
        this.requestLatency = registry.timer("server.request.latency");
        this.errorCount = registry.counter("server.errors.total");
    }

    public void start() {
        // Initialize any metrics that need setup
    }

    public void stop() {
        // Cleanup any metrics resources
    }

    public void incrementActiveConnections() {
        activeConnections.increment();
    }

    public void decrementActiveConnections() {
        activeConnections.increment(-1);
    }

    public void recordRequest() {
        totalRequests.increment();
    }

    public Timer.Sample startRequest() {
        return Timer.start(registry);
    }

    public void endRequest(Timer.Sample sample) {
        sample.stop(requestLatency);
    }

    public void recordError() {
        errorCount.increment();
    }
}
