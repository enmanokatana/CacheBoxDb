package org.athens.network.handlers;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.athens.network.protocol.CBSPResponse;
import org.athens.network.RequestParser;
import org.athens.network.support.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    private final MetricsCollector metricsCollector;

    public ExceptionHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Channel error", cause);
        metricsCollector.recordError();
        ctx.writeAndFlush(new CBSPResponse(RequestParser.encodeError("Internal server error")));
        ctx.close();
    }
}