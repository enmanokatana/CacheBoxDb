package org.athens.network;

import org.athens.db.shrading.ShardedCacheBox;

import java.util.List;

public interface CacheCommand {
    String execute(List<String> args, ShardedCacheBox cacheBox) throws Exception;
}
