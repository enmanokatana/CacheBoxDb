package org.athens.network.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.athens.network.commands.CommandFactory;
import org.athens.network.handlers.*;
import org.athens.network.protocol.CBSPDecoder;
import org.athens.network.protocol.CBSPEncoder;
import org.athens.network.support.MetricsCollector;
import org.athens.db.shrading.ShardedCacheBox;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private final ServerConfig config;
    private final CommandFactory commandFactory;
    private final MetricsCollector metricsCollector;
    private final ShardedCacheBox cacheBox;

    public ServerInitializer(ServerConfig config, CommandFactory commandFactory, MetricsCollector metricsCollector, ShardedCacheBox cacheBox) {
        this.config = config;
        this.commandFactory = commandFactory;
        this.metricsCollector = metricsCollector;
        this.cacheBox = cacheBox;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("connectionTracker", new ConnectionTrackingHandler(metricsCollector));
        pipeline.addLast("idleStateHandler", new IdleStateHandler(config.getReaderIdleTime(), config.getWriterIdleTime(), config.getAllIdleTime()));
        pipeline.addLast("frameDecoder", new DelimiterBasedFrameDecoder(config.getMaxFrameLength(), true, config.getDelimiters()));
        pipeline.addLast("protocolDecoder", new CBSPDecoder());
        pipeline.addLast("protocolEncoder", new CBSPEncoder());
        pipeline.addLast("rateLimiter", new RateLimitHandler(config.getMaxRequestsPerSecond()));
        pipeline.addLast("requestHandler", new RequestHandler(commandFactory, cacheBox, metricsCollector));
        pipeline.addLast("exceptionHandler", new ExceptionHandler(metricsCollector));
    }
}