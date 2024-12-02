package org.athens;

import java.util.HashMap;
import java.util.Map;

public class Transaction {
    private final Map<String, CacheValue> globalStore;
    private final Map<String, CacheValue> stagedChanges = new HashMap<>();
    private final Map<String, CacheValue> stagedDeletions = new HashMap<>();
    private final Map<String, Integer> readVersions = new HashMap<>();

    public Transaction(Map<String, CacheValue> globalStore) {
        this.globalStore = globalStore;
    }

    public void put(String key, CacheValue value) {
        stagedChanges.put(key, value);
        stagedDeletions.remove(key);
    }

    public CacheValue get(String key) {
        if (stagedChanges.containsKey(key)) {
            return stagedChanges.get(key);
        }
        if (stagedDeletions.containsKey(key)) {
            return null;
        }
        CacheValue value = globalStore.get(key);
        if (value != null) {
            readVersions.put(key, value.getVersion());
        }
        return value;
    }

    public void delete(String key) {
        if (globalStore.containsKey(key) || stagedChanges.containsKey(key)) {
            stagedDeletions.put(key, stagedChanges.getOrDefault(key, globalStore.get(key)));
            stagedChanges.remove(key);
        }
    }

    public void commit() {
        for (Map.Entry<String, CacheValue> entry : stagedChanges.entrySet()) {
            String key = entry.getKey();
            CacheValue newValue = entry.getValue();
            CacheValue current = globalStore.get(key);
            int expectedVersion = readVersions.getOrDefault(key, current != null ? current.getVersion() : 0);
            if (current != null && current.getVersion() != expectedVersion) {
                throw new ConcurrencyException("Conflict on key " + key);
            }
            int newVersion = current != null ? current.getVersion() + 1 : 1;
            int finalVersion = newValue.getVersion() == 0 ? newVersion : newValue.getVersion();
            CacheValue updatedValue = new CacheValue(finalVersion, newValue.getType(), newValue.getValue());
            globalStore.put(key, updatedValue);
        }
        stagedChanges.clear();
        readVersions.clear();
    }
    public void rollback() {
        stagedChanges.clear();
        stagedDeletions.clear();
        readVersions.clear();
    }

    public Map<String, CacheValue> getStagedChanges() {
        return stagedChanges;
    }
}