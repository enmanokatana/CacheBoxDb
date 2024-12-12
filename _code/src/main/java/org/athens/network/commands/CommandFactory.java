package org.athens.network.commands;
import org.athens.network.CacheCommand;

import java.util.HashMap;
import java.util.Map;

public class CommandFactory {
    private static final Map<String, CacheCommand> commandMap = new HashMap<>();

    static {
        commandMap.put("DELETE", new DeleteCommand());
        commandMap.put("PUT", new PutCommand());
        commandMap.put("GET", new GetCommand());
    }

    public static CacheCommand getCommand(String action) {
        return commandMap.getOrDefault(action.toUpperCase(), null);
    }
}
