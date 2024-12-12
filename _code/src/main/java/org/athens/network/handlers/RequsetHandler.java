package org.athens.network.handlers;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import org.athens.db.shrading.ShardedCacheBox;
import org.athens.network.CacheCommand;
import org.athens.network.RequestParser;
import org.athens.network.commands.CommandFactory;
import org.athens.network.protocol.CBSPRequest;
import org.athens.network.protocol.CBSPResponse;
import org.athens.network.support.MetricsCollector;
import io.micrometer.core.instrument.Timer;

import java.util.List;

public class RequestHandler extends SimpleChannelInboundHandler<CBSPRequest> {
    private final CommandFactory commandFactory;
    private final ShardedCacheBox cacheBox;
    private final MetricsCollector metricsCollector;

    public RequestHandler(CommandFactory commandFactory, ShardedCacheBox cacheBox, MetricsCollector metricsCollector) {
        this.commandFactory = commandFactory;
        this.cacheBox = cacheBox;
        this.metricsCollector = metricsCollector;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CBSPRequest request) {
        Timer.Sample sample = metricsCollector.startRequest();
        try {
            String commandName = request.getCommand();
            List<String> args = request.getArguments();
            CacheCommand command = commandFactory.getCommand(commandName);
            if (command == null) {
                ctx.writeAndFlush(new CBSPResponse(RequestParser.encodeError("Unknown command '" + commandName + "'")));
                return;
            }
            String response = command.execute(args, cacheBox);
            ctx.writeAndFlush(new CBSPResponse(response));
        } catch (Exception e) {
            metricsCollector.recordError();
            ctx.writeAndFlush(new CBSPResponse(RequestParser.encodeError(e.getMessage())));
        } finally {
            metricsCollector.endRequest(sample);
        }
    }
}