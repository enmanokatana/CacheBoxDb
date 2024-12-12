package org.athens.network.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.athens.network.commands.CommandFactory;
import org.athens.network.handlers.ConnectionTrackingHandler;
import org.athens.network.handlers.RateLimitHandler;
import org.athens.network.protocol.CBSPDecoder;
import org.athens.network.protocol.CBSPEncoder;
import org.athens.network.support.HealthCheckService;
import org.athens.network.support.MetricsCollector;
import org.athens.db.shrading.ShardedCacheBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CacheBoxServer implements IServer {
    private static final Logger logger = LoggerFactory.getLogger(CacheBoxServer.class);

    private final ServerConfig config;
    private final ShardedCacheBox cacheBox;
    private final MetricsCollector metricsCollector;
    private final CommandFactory commandFactory;
    private final HealthCheckService healthCheckService;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.STOPPED);

    public CacheBoxServer(ServerConfig config, ShardedCacheBox cacheBox, MetricsCollector metricsCollector, CommandFactory commandFactory, HealthCheckService healthCheckService) {
        this.config = config;
        this.cacheBox = cacheBox;
        this.metricsCollector = metricsCollector;
        this.commandFactory = commandFactory;
        this.healthCheckService = healthCheckService;
    }

    @Override
    public void start() {
        if (!status.compareAndSet(ServerStatus.STOPPED, ServerStatus.STARTING)) {
            throw new IllegalStateException("Server is already running or starting");
        }

        try {
            initializeServer();
            startMetrics();
            startHealthCheck();
            status.set(ServerStatus.RUNNING);
            logger.info("Server successfully started on port {}", config.getPort());
        } catch (Exception e) {
            status.set(ServerStatus.ERROR);
            logger.error("Failed to start server", e);
            shutdown();
            throw new ServerStartupException("Failed to start server", e);
        }
    }

    private void initializeServer() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(config.getBossThreads());
        workerGroup = new NioEventLoopGroup(config.getWorkerThreads());

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, config.getConnectionBacklog())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeout())
                .childHandler(new ServerInitializer(config, commandFactory, metricsCollector, cacheBox));

        serverChannel = bootstrap.bind(config.getPort()).sync().channel();
    }

    @Override
    public void shutdown() {
        if (!status.compareAndSet(ServerStatus.RUNNING, ServerStatus.SHUTTING_DOWN)) {
            return;
        }

        logger.info("Initiating server shutdown...");

        try {
            serverChannel.close().sync();
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            stopMetrics();
            stopHealthCheck();
            status.set(ServerStatus.STOPPED);
            logger.info("Server shutdown complete");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Server shutdown interrupted", e);
            status.set(ServerStatus.ERROR);
        } catch (Exception e) {
            logger.error("Error during server shutdown", e);
            status.set(ServerStatus.ERROR);
        }
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public ServerStatus getStatus() {
        return null;
    }

    private void startMetrics() {
        metricsCollector.start();
    }

    private void stopMetrics() {
        metricsCollector.stop();
    }

    private void startHealthCheck() {
        healthCheckService.startMonitoring(this);
    }

    private void stopHealthCheck() {
        healthCheckService.stopMonitoring();
    }
}