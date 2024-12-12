package org.athens.network.handlers;


import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import com.google.common.util.concurrent.RateLimiter;
import org.athens.network.protocol.CBSPResponse;

public class RateLimitHandler extends ChannelDuplexHandler {
    private final RateLimiter rateLimiter;

    public RateLimitHandler(int maxRequestsPerSecond) {
        this.rateLimiter = RateLimiter.create(maxRequestsPerSecond);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!rateLimiter.tryAcquire()) {
            ctx.writeAndFlush(new CBSPResponse(
                    RequestParser.encodeError("Too many requests")));
            return;
        }
        ctx.fireChannelRead(msg);
    }
}