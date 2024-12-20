package org.athens.network.commands;

import org.athens.db.shrading.ShardedCacheBox;
import org.athens.network.CacheCommand;
import org.athens.network.RequestParser;

import java.util.List;

import static org.athens.network.Server.logger;

public class DeleteCommand implements CacheCommand {
    @Override
    public String execute(List<String> args, ShardedCacheBox cacheBox) throws Exception {
        if (args.isEmpty()) {
            return RequestParser.encodeError("DELETE command has insufficient arguments");
        }

        cacheBox.beginTransaction();
        if (!cacheBox.isTransactionActive()) {
            logger.warn("No transaction active for DELETE command");
            return RequestParser.encodeError("No transaction active. Start with 'begin'.");
        }

        cacheBox.delete(args.get(0));
        cacheBox.commit();
        return RequestParser.encodeSimpleString("OK");
    }
}