package org.athens;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.athens.utils.AESEncryptionStrategy;
import org.athens.utils.EncryptionStrategy;
import org.athens.utils.XOREncryptionStrategy;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheBox {
    private final Cache<String, CacheValue> globalStore;
    private final ConcurrentHashMap<String, String> keyIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> valueIndex = new ConcurrentHashMap<>();
    private final Storage storage;
    private final TransactionManager transactionManager;
    private EncryptionStrategy encryptionStrategy;

    private boolean encryptionEnabled;
    private byte[] encryptionKey;

    public CacheBox(String dbFile, boolean encryptionEnabled, byte[] encryptionKey, EncryptionStrategy encryptionStrategy) {
        this.storage = new Storage(dbFile, encryptionEnabled, encryptionKey, encryptionStrategy);
        this.globalStore = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
        this.transactionManager = new TransactionManager(globalStore);
        this.encryptionEnabled = encryptionEnabled;
        this.encryptionKey = encryptionKey;
        this.encryptionStrategy = encryptionStrategy;

        storage.loadWithRecovery(globalStore.asMap());
        initializeIndexes();
    }

    public CacheBox(String dbFile) {
        byte[] key = new SecureRandom().generateSeed(16);
        EncryptionStrategy strategy = new AESEncryptionStrategy();
        this.storage = new Storage(dbFile, true, key, strategy);
        this.globalStore = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
        this.transactionManager = new TransactionManager(globalStore);
        this.encryptionEnabled = true;
        this.encryptionKey = key;
        this.encryptionStrategy = strategy;
        storage.loadWithRecovery(globalStore.asMap());
        initializeIndexes();
    }

    private void initializeIndexes() {
        for (Map.Entry<String, CacheValue> entry : globalStore.asMap().entrySet()) {
            updateIndexes(entry.getKey(), entry.getValue());
        }
    }

    public void put(String key, CacheValue value) {
        transactionManager.getActiveTransaction().put(key, value);
        updateIndexes(key, value);
    }

    private void updateIndexes(String key, CacheValue value) {
        keyIndex.put(key, key);
        if (value.getType() == CacheValue.Type.INTEGER) {
            valueIndex.put(key, (Integer) value.getValue());
        }
    }

    public CacheValue get(String key) {
        return transactionManager.getActiveTransaction().get(key);
    }

    public void delete(String key) {
        transactionManager.getActiveTransaction().delete(key);
        removeIndexes(key);
    }

    private void removeIndexes(String key) {
        keyIndex.remove(key);
        valueIndex.remove(key);
    }

    public void commit() {
        transactionManager.commit();
        storage.saveToDisk(globalStore.asMap()); // Persist changes
    }

    public void rollback() {
        transactionManager.rollback();
    }

    public void beginTransaction() {
        transactionManager.beginTransaction();
    }

    public boolean isTransactionActive() {
        return transactionManager.isTransactionActive();
    }

    public Map<String, CacheValue> getStagedState() {
        if (!isTransactionActive()) {
            throw new IllegalStateException("No active transaction.");
        }
        return transactionManager.getActiveTransaction().getStagedChanges();
    }

    public Map<String, CacheValue> getCommittedState() {
        return new HashMap<>(globalStore.asMap()); // Return a copy to prevent external modification
    }

    public Map<String, CacheValue> searchCommitted(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();

        if (query.getPattern() != null) {
            for (String indexedKey : keyIndex.keySet()) {
                if (indexedKey.matches(query.getPattern())) {
                    results.put(indexedKey, globalStore.getIfPresent(indexedKey));
                }
            }
        }

        if (query.getMinValue() != null || query.getMaxValue() != null) {
            for (Map.Entry<String, Integer> entry : valueIndex.entrySet()) {
                Integer intValue = entry.getValue();
                if ((query.getMinValue() == null || intValue >= query.getMinValue()) &&
                        (query.getMaxValue() == null || intValue <= query.getMaxValue())) {
                    results.put(entry.getKey(), globalStore.getIfPresent(entry.getKey()));
                }
            }
        }

        return results;
    }

    public Map<String, CacheValue> searchStaged(CacheQuery query) {
        if (!isTransactionActive()) {
            throw new IllegalStateException("No active transaction.");
        }
        return search(query, transactionManager.getActiveTransaction().getStagedChanges());
    }

    public Map<String, CacheValue> search(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();

        // Search committed state
        results.putAll(searchCommitted(query));

        // If a transaction is active, overlay results with staged changes
        if (isTransactionActive()) {
            Map<String, CacheValue> stagedResults = searchStaged(query);
            results.putAll(stagedResults); // Staged changes take precedence
        }

        return results;
    }

    private Map<String, CacheValue> search(CacheQuery query, Map<String, CacheValue> data) {
        Map<String, CacheValue> results = new HashMap<>();

        for (Map.Entry<String, CacheValue> entry : data.entrySet()) {
            if (matchesQuery(entry.getKey(), entry.getValue(), query)) {
                results.put(entry.getKey(), entry.getValue());
            }
        }

        return results;
    }

    private boolean matchesQuery(String key, CacheValue value, CacheQuery query) {
        // Type filter
        if (query.getTypeFilter() != null && value.getType() != query.getTypeFilter()) {
            return false;
        }

        // Pattern matching for key or string value
        if (query.getPattern() != null) {
            if (key.matches(query.getPattern())) {
                return true;
            }
            if (value.getType() == CacheValue.Type.STRING &&
                    value.getValue().toString().matches(query.getPattern())) {
                return true;
            }
        }

        // Range queries for integer values
        if (value.getType() == CacheValue.Type.INTEGER) {
            Integer intValue = (Integer) value.getValue();
            if (query.getMinValue() != null && intValue < query.getMinValue()) {
                return false;
            }
            if (query.getMaxValue() != null && intValue > query.getMaxValue()) {
                return false;
            }
        }

        return query.getPattern() == null && query.getMinValue() == null &&
                query.getMaxValue() == null && query.getTypeFilter() == null;
    }

    public void setEncryptionStrategy(EncryptionStrategy encryptionStrategy) {
        this.encryptionStrategy = encryptionStrategy;
        storage.setEncryptionStrategy(encryptionStrategy);
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
        storage.setEncryptionEnabled(encryptionEnabled);
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
        storage.setEncryptionKey(encryptionKey);
    }

    public Map<String, CacheValue> getGlobalStore() {
        return globalStore.asMap();
    }

    public Storage getStorage() {
        return storage;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public EncryptionStrategy getEncryptionStrategy() {
        return encryptionStrategy;
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }
}