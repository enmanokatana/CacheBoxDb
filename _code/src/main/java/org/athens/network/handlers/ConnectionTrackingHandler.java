package org.athens.network.handlers;


import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.athens.network.support.MetricsCollector;

public class ConnectionTrackingHandler extends ChannelDuplexHandler {
    private final MetricsCollector metricsCollector;

    public ConnectionTrackingHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        metricsCollector.incrementActiveConnections();
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metricsCollector.decrementActiveConnections();
        ctx.fireChannelInactive();
    }
}