package org.athens;

import org.athens.utils.EncryptionStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShardedCacheBox {
    private final Map<Integer, CacheBox> shards;
    private final ShardingStrategy shardingStrategy;

    public ShardedCacheBox(int numberOfShards, String dbFilePrefix) {
        this.shards = new ConcurrentHashMap<>();
        this.shardingStrategy = new ShardingStrategy(numberOfShards);

        for (int i = 0; i < numberOfShards; i++) {
            shards.put(i, new CacheBox(dbFilePrefix + i + ".cbx"));
        }
    }
    public ShardedCacheBox(int numberOfShards, String dbFilePrefix, EncryptionStrategy encryptionStrategy, byte[] encryptionKey) {
        this.shards = new ConcurrentHashMap<>();
        this.shardingStrategy = new ShardingStrategy(numberOfShards);

        for (int i = 0; i < numberOfShards; i++) {
            shards.put(i, new CacheBox(dbFilePrefix + i + ".cbx", true, encryptionKey, encryptionStrategy));
        }
    }

    public void put(String key, CacheValue value) {
        int shardId = shardingStrategy.getShardForKey(key);
        shards.get(shardId).put(key, value);
    }

    public CacheValue get(String key) {
        int shardId = shardingStrategy.getShardForKey(key);
        return shards.get(shardId).get(key);
    }

    public void delete(String key) {
        int shardId = shardingStrategy.getShardForKey(key);
        shards.get(shardId).delete(key);
    }







    public Map<String, CacheValue> search(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            results.putAll(shard.search(query));
        }
        return results;
    }
    public void setEncryptionEnabled(boolean encryptionEnabled) {
        shards.values().forEach(shard -> shard.setEncryptionEnabled(encryptionEnabled));
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        shards.values().forEach(shard -> shard.setEncryptionKey(encryptionKey));
    }

    public byte[] getEncryptionKey() {

        return shards.values().stream().findAny().get().getEncryptionKey();
    }
    public EncryptionStrategy getEncryptionStrategy() {
        return shards.values().stream().findAny().get().getEncryptionStrategy();

    }

    public void setEncryptionStrategy(EncryptionStrategy encryptionStrategy) {
        shards.values().forEach(shard -> shard.setEncryptionStrategy(encryptionStrategy));
    }
    public int getShardForKey(String key) {
        return Math.abs(key.hashCode()) % shards.size();
    }
    public Map<String, CacheValue> searchStaged(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            results.putAll(shard.searchStaged(query));
        }
        return results;
    }
    public Map<String, CacheValue> getStagedState() {
        Map<String, CacheValue> stagedState = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            stagedState.putAll(shard.getStagedState());
        }
        return stagedState;
    }

    public Map<String, CacheValue> getCommittedState() {
        Map<String, CacheValue> committedState = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            committedState.putAll(shard.getCommittedState());
        }
        return committedState;
    }
    public Map<String, CacheValue> searchCommitted(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            results.putAll(shard.searchCommitted(query));
        }
        return results;
    }
    public void beginTransaction() {
        shards.values().forEach(shard -> shard.beginTransaction());
    }

    public void commit() {
        shards.values().forEach(shard -> shard.commit());
    }

    public void rollback() {
        shards.values().forEach(shard -> shard.rollback());
    }

    public boolean isTransactionActive() {
        return shards.values().stream().allMatch(CacheBox::isTransactionActive);
    }
}