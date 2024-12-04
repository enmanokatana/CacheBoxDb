package org.athens;

import org.athens.utils.EncryptionStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShardedCacheBox {
    private final Map<Integer, CacheBox> shards;
    private final ConsistentHashing consistentHashing;
    private final EncryptionStrategy encryptionStrategy;
    private final boolean encryptionEnabled;
    private final byte[] encryptionKey;
    private final int maxSize;

    public ShardedCacheBox(int initialNumberOfShards, String dbFilePrefix, EncryptionStrategy encryptionStrategy, boolean encryptionEnabled, byte[] encryptionKey, int maxSize) {
        this.shards = new ConcurrentHashMap<>();
        this.encryptionStrategy = encryptionStrategy;
        this.encryptionEnabled = encryptionEnabled;
        this.encryptionKey = encryptionKey;
        this.maxSize = maxSize;

        List<CacheBox> initialShards = new ArrayList<>();
        for (int i = 0; i < initialNumberOfShards; i++) {
            CacheBox shard = new CacheBox(dbFilePrefix + i + ".cbx", encryptionEnabled, encryptionKey, encryptionStrategy, maxSize);
            shards.put(i, shard);
            initialShards.add(shard);
        }
        this.consistentHashing = new ConsistentHashing(100, initialShards);
    }

    public void put(String key, CacheValue value) {
        CacheBox shard = consistentHashing.get(key);
        shard.put(key, value);
    }

    public CacheValue get(String key) {
        CacheBox shard = consistentHashing.get(key);
        return shard.get(key);
    }

    public void delete(String key) {
        CacheBox shard = consistentHashing.get(key);
        shard.delete(key);
    }

    public void commit() {
        for (CacheBox shard : shards.values()) {
            shard.commit();
        }
    }

    public void rollback() {
        for (CacheBox shard : shards.values()) {
            shard.rollback();
        }
    }

    public void beginTransaction() {
        for (CacheBox shard : shards.values()) {
            shard.beginTransaction();
        }
    }

    public boolean isTransactionActive() {
        return shards.values().stream().allMatch(CacheBox::isTransactionActive);
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

    public Map<String, CacheValue> search(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            results.putAll(shard.search(query));
        }
        return results;
    }

    public Map<String, CacheValue> searchStaged(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            results.putAll(shard.searchStaged(query));
        }
        return results;
    }

    public Map<String, CacheValue> searchCommitted(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();
        for (CacheBox shard : shards.values()) {
            results.putAll(shard.searchCommitted(query));
        }
        return results;
    }

    public void addShard() {
        int newShardId = shards.size();
        CacheBox newShard = new CacheBox("shard" + newShardId + ".cbx", encryptionEnabled, encryptionKey, encryptionStrategy, maxSize);
        shards.put(newShardId, newShard);
        consistentHashing.add(newShard);
    }

    public void removeShard(int shardId) {
        CacheBox shardToRemove = shards.get(shardId);
        shards.remove(shardId);
        consistentHashing.remove(shardToRemove);
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        for (CacheBox shard : shards.values()) {
            shard.setEncryptionEnabled(encryptionEnabled);
        }
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        for (CacheBox shard : shards.values()) {
            shard.setEncryptionKey(encryptionKey);
        }
    }

    public void setEncryptionStrategy(EncryptionStrategy encryptionStrategy) {
        for (CacheBox shard : shards.values()) {
            shard.setEncryptionStrategy(encryptionStrategy);
        }
    }

    public EncryptionStrategy getEncryptionStrategy() {
        return encryptionStrategy;
    }

    public Map<Integer, CacheBox> getShards() {
        return shards;
    }
}