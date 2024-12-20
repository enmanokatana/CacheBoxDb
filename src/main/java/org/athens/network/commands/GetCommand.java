package org.athens.network.commands;

import org.athens.db.shrading.ShardedCacheBox;
import org.athens.network.CacheCommand;
import org.athens.network.RequestParser;
import org.athens.network.Server;
import org.athens.utils.CacheValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.athens.network.Server.logger;

import java.util.List;

public class GetCommand implements CacheCommand {

    @Override
    public String execute(List<String> args, ShardedCacheBox cacheBox) throws Exception {
        if (args.size() < 1) {
            logger.warn("GET command has insufficient arguments");
            return RequestParser.encodeError("GET requires a key");
        }

        cacheBox.beginTransaction();
        if (!cacheBox.isTransactionActive()) {
            logger.warn("No transaction active for GET command");
            return RequestParser.encodeError("No transaction active. Start with 'begin'.");
        }

        String key = args.get(0);
        logger.debug("GET command for key: {}", key);

        CacheValue getValue = cacheBox.get(key);
        cacheBox.commit();

        if (getValue != null) {
            logger.info("GET command successful. Key: {}, Value: {}", key, getValue.asString());
            return RequestParser.encodeBulkString(getValue.asString());
        } else {
            logger.info("GET command failed. Key not found: {}", key);
            return RequestParser.encodeSimpleString("NULL");
        }
    }
}
