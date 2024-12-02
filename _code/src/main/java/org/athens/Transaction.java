package org.athens;

import java.util.HashMap;
import java.util.Map;

public class Transaction {
    private final Map<String, CacheValue> transactionStore;
    private final CacheBox cacheBox;

    public Transaction(CacheBox cacheBox) {
        this.cacheBox = cacheBox;
        this.transactionStore = new HashMap<>();
    }

    public void put(String key, CacheValue value) {
        transactionStore.put(key, value);
    }

    public void delete(String key) {
        transactionStore.put(key, null); // Mark for deletion
    }

    public CacheValue get(String key) {
        return transactionStore.containsKey(key) ? transactionStore.get(key) : cacheBox.get(key);
    }

    public void commit() {
        synchronized (cacheBox) {
            for (Map.Entry<String, CacheValue> entry : transactionStore.entrySet()) {
                if (entry.getValue() == null) {
                    cacheBox.delete(entry.getKey());
                } else {
                    cacheBox.put(entry.getKey(), entry.getValue());
                }
            }
        }
        transactionStore.clear();
    }

    public void rollback() {
        transactionStore.clear(); // Discard all staged changes
    }
}
