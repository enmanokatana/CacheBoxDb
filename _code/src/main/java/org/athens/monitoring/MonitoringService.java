package org.athens.monitoring;

import org.athens.db.core.CacheBox;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MonitoringService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Integer, CacheBox> shards;
    private boolean isLiveMonitoringActive = false;
    private Thread liveMonitoringThread;

    public MonitoringService(Map<Integer, CacheBox> shards) {
        this.shards = shards;
    }

    public void startLiveMonitoring() {
        if (isLiveMonitoringActive) {
            System.out.println("Live monitoring is already active.");
            return;
        }
        isLiveMonitoringActive = true;
        liveMonitoringThread = new Thread(() -> {
            while (isLiveMonitoringActive) {
                collectAndPrintMetrics();
                try {
                    Thread.sleep(1000); // Update every second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        liveMonitoringThread.start();
    }

    public void stopLiveMonitoring() {
        if (!isLiveMonitoringActive) {
            System.out.println("Live monitoring is not active.");
            return;
        }
        isLiveMonitoringActive = false;
        liveMonitoringThread.interrupt();
        System.out.println("Live monitoring stopped.");
    }

    public String getSnapshotMetrics() {
        StringBuilder metrics = new StringBuilder();
        for (CacheBox shard : shards.values()) {
            metrics.append(monitorShard(shard));
        }
        return metrics.toString();
    }

    private void collectAndPrintMetrics() {
        System.out.println("Live monitoring metrics:");
        for (CacheBox shard : shards.values()) {
            System.out.print(monitorShard(shard));
        }
        System.out.println("----------------------------------------");
    }

    private String monitorShard(CacheBox shard) {
        StringBuilder shardMetrics = new StringBuilder();

        // Log memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        shardMetrics.append(String.format("Shard: %s - Memory Usage: %d bytes%n", shard.getDbFile(), usedMemory));

        // Log disk usage (assuming the storage is a file)
        File dbFile = new File(shard.getDbFile());
        if (dbFile.exists()) {
            long diskUsage = dbFile.length();
            shardMetrics.append(String.format("Shard: %s - Disk Usage: %d bytes%n", shard.getDbFile(), diskUsage));
        }

        // Log other relevant metrics (e.g., number of entries, cache hits, etc.)
        int numberOfEntries = shard.getGlobalStore().size();
        shardMetrics.append(String.format("Shard: %s - Number of Entries: %d%n", shard.getDbFile(), numberOfEntries));

        return shardMetrics.toString();
    }

    public boolean isLiveMonitoringActive() {
        return isLiveMonitoringActive;
    }

    public void setLiveMonitoringActive(boolean liveMonitoringActive) {
        isLiveMonitoringActive = liveMonitoringActive;
    }
}