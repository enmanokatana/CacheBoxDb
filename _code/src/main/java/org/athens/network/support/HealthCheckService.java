package org.athens.network.support;


import org.athens.network.core.IServer;
import org.athens.network.core.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class HealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    public void startMonitoring(IServer server) {
        if (monitoring.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    performHealthCheck(server);
                } catch (Exception e) {
                    logger.error("Health check failed", e);
                }
            }, 0, 30, TimeUnit.SECONDS);
        }
    }

    public void stopMonitoring() {
        if (monitoring.compareAndSet(true, false)) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
    }

    private void performHealthCheck(IServer server) {
        ServerStatus status = server.getStatus();
        if (status != ServerStatus.RUNNING) {
            logger.warn("Server health check failed. Current status: {}", status);
            // Implement additional health check logic here
        }
    }
}
