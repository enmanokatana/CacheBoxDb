package org.athens.network.commands;

import org.athens.db.shrading.ShardedCacheBox;
import org.athens.network.CacheCommand;
import org.athens.network.RequestParser;
import org.athens.network.Server;
import org.athens.utils.CacheValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.athens.network.Server.logger;

import java.util.Arrays;
import java.util.List;

public class PutCommand implements CacheCommand {

    @Override
    public String execute(List<String> args, ShardedCacheBox cacheBox) throws Exception {
        if (args.size() < 3) {
            logger.warn("PUT command has insufficient arguments");
            return RequestParser.encodeError("PUT requires type, key, and value");
        }

        cacheBox.beginTransaction();
        if (!cacheBox.isTransactionActive()) {
            logger.warn("No transaction active for PUT command");
            return RequestParser.encodeError("No transaction active. Start with 'begin'.");
        }

        String inputType = args.get(0).toLowerCase();
        String inputKey = args.get(1);
        String inputValue = args.get(2);
        logger.debug("PUT command details: type={}, key={}, value={}", inputType, inputKey, inputValue);

        switch (inputType) {
            case "string":
                cacheBox.put(inputKey, CacheValue.of(0, inputValue));
                break;
            case "int":
                cacheBox.put(inputKey, CacheValue.of(0, Integer.parseInt(inputValue)));
                break;
            case "bool":
                cacheBox.put(inputKey, CacheValue.of(0, Boolean.parseBoolean(inputValue)));
                break;
            case "list":
                cacheBox.put(inputKey, CacheValue.of(0, Arrays.asList(inputValue.split(","))));
                break;
            default:
                logger.warn("Unsupported type for PUT command: {}", inputType);
                return RequestParser.encodeError("Unknown type. Supported types: string, int, bool, list");
        }

        cacheBox.commit();
        logger.info("PUT command successful for key: {}", inputKey);
        return RequestParser.encodeSimpleString("OK");
    }
}
