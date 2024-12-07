package org.athens.network;


import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerMetrics {
    private final long totalRequests;
    private final long failedRequests;
    private final CacheStats cacheStats;
    private final int activeThreads;
    private final int queuedTasks;
    private final int dataStoreSize;

    public ServerMetrics(long totalRequests, long failedRequests, CacheStats cacheStats,
                         ExecutorService executorService, int dataStoreSize) {
        this.totalRequests = totalRequests;
        this.failedRequests = failedRequests;
        this.cacheStats = cacheStats;
        this.activeThreads = ((ThreadPoolExecutor) executorService).getActiveCount();
        this.queuedTasks = ((ThreadPoolExecutor) executorService).getQueue().size();
        this.dataStoreSize = dataStoreSize;
    }

    // Getters
    public long getTotalRequests() { return totalRequests; }
    public long getFailedRequests() { return failedRequests; }
    public CacheStats getCacheStats() { return cacheStats; }
    public int getActiveThreads() { return activeThreads; }
    public int getQueuedTasks() { return queuedTasks; }
    public int getDataStoreSize() { return dataStoreSize; }
    public double getErrorRate() {
        return totalRequests > 0 ? (double) failedRequests / totalRequests : 0.0;
    }

    @Override
    public String toString() {
        return String.format(
                "ServerMetrics{total=%d, failed=%d, errorRate=%.2f%%, active threads=%d, queued=%d, store size=%d}",
                totalRequests, failedRequests, getErrorRate() * 100, activeThreads, queuedTasks, dataStoreSize
        );
    }
}