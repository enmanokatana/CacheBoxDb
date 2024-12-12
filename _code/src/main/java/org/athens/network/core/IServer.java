package org.athens.network.core;

public interface IServer {
    void start();
    void shutdown();
    boolean isRunning();
    ServerStatus getStatus();
}
