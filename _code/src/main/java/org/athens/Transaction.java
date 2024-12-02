package org.athens;

import java.util.HashMap;
import java.util.Map;

public class Transaction {
    private final Map<String, CacheValue> globalStore;
    private final Map<String, CacheValue> stagedChanges = new HashMap<>();
    private final Map<String, CacheValue> stagedDeletions = new HashMap<>();

    public Transaction(Map<String, CacheValue> globalStore) {
        this.globalStore = globalStore;
    }

    public void put(String key, CacheValue value) {
        stagedChanges.put(key, value);
        stagedDeletions.remove(key); // Undo staged deletions if any
    }

    public CacheValue get(String key) {
        if (stagedChanges.containsKey(key)) {
            return stagedChanges.get(key);
        }
        if (stagedDeletions.containsKey(key)) {
            return null; // Deleted in this transaction
        }
        return globalStore.get(key);
    }

    public void delete(String key) {
        if (globalStore.containsKey(key) || stagedChanges.containsKey(key)) {
            stagedDeletions.put(key, stagedChanges.getOrDefault(key, globalStore.get(key)));
            stagedChanges.remove(key); // Undo staged changes if any
        }
    }

    public void commit() {
        for (Map.Entry<String, CacheValue> entry : stagedChanges.entrySet()) {
            globalStore.put(entry.getKey(), entry.getValue());
        }
        for (String key : stagedDeletions.keySet()) {
            globalStore.remove(key);
        }
        stagedChanges.clear();
        stagedDeletions.clear();
    }

    public void rollback() {
        stagedChanges.clear();
        stagedDeletions.clear();
    }

    /**
     * Expose the current staged changes.
     * @return A map of key-value pairs staged for the current transaction.
     */
    public Map<String, CacheValue> getStagedChanges() {
        return stagedChanges;
    }
}
