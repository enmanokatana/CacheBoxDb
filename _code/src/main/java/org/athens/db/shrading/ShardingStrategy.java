package org.athens.db.shrading;

public class ShardingStrategy {
    private final int numberOfShards;

    public ShardingStrategy(int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public int getShardForKey(String key) {
        return Math.abs(key.hashCode()) % numberOfShards;
    }
}