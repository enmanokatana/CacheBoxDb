package org.athens.network.core;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfig {
    @Min(1)
    private int port = 20029;

    @Min(1)
    private int bossThreads = 1;

    @Min(1)
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    @Min(1)
    private int connectionBacklog = 1024;

    @Min(1)
    private int maxRequestsPerSecond = 10000;

    @Min(1024)
    private int maxFrameLength = 1024 * 1024;

    @Min(1)
    private int readerIdleTime = 30;

    @Min(1)
    private int writerIdleTime = 30;

    @Min(1)
    private int allIdleTime = 60;

    @Min(1000)
    private int connectionTimeout = 5000;

    // Getters and setters
    // ... (implement all getters and setters)

    public void setPort(int port) {
        this.port = port;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public void setConnectionBacklog(int connectionBacklog) {
        this.connectionBacklog = connectionBacklog;
    }

    public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    public void setMaxFrameLength(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    public void setReaderIdleTime(int readerIdleTime) {
        this.readerIdleTime = readerIdleTime;
    }

    public void setWriterIdleTime(int writerIdleTime) {
        this.writerIdleTime = writerIdleTime;
    }

    public void setAllIdleTime(int allIdleTime) {
        this.allIdleTime = allIdleTime;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getPort() {
        return port;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public int getConnectionBacklog() {
        return connectionBacklog;
    }

    public int getMaxRequestsPerSecond() {
        return maxRequestsPerSecond;
    }

    public int getMaxFrameLength() {
        return maxFrameLength;
    }

    public int getReaderIdleTime() {
        return readerIdleTime;
    }

    public int getWriterIdleTime() {
        return writerIdleTime;
    }

    public int getAllIdleTime() {
        return allIdleTime;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }
}
