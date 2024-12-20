package org.athens.db.core;

import org.athens.db.encryption.AESEncryptionStrategy;
import org.athens.utils.CacheQuery;
import org.athens.utils.CacheValue;
import org.athens.db.encryption.EncryptionStrategy;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheBox {
    private final LRUCache<String, CacheValue> cache;
    private final ConcurrentHashMap<String, String> keyIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> valueIndex = new ConcurrentHashMap<>();
    private final Storage storage;
    private final TransactionManager transactionManager;
    private EncryptionStrategy encryptionStrategy;
    private final String dbFile;

    private boolean encryptionEnabled;
    private byte[] encryptionKey;

    public CacheBox(String dbFile, boolean encryptionEnabled, byte[] encryptionKey, EncryptionStrategy encryptionStrategy, int maxSize) {
        this.dbFile = dbFile;
        this.storage = new Storage(dbFile, encryptionEnabled, encryptionKey, encryptionStrategy);
        this.cache = new LRUCache<>(maxSize);
        this.transactionManager = new TransactionManager(cache);
        this.encryptionEnabled = encryptionEnabled;
        this.encryptionKey = encryptionKey;
        this.encryptionStrategy = encryptionStrategy;

        storage.loadWithRecovery(cache);
        initializeIndexes();
    }

    public CacheBox(String dbFile, int maxSize) {
        this.dbFile = dbFile;
        byte[] key = new SecureRandom().generateSeed(16);
        EncryptionStrategy strategy = new AESEncryptionStrategy();
        this.storage = new Storage(dbFile, true, key, strategy);
        this.cache = new LRUCache<>(maxSize);
        this.transactionManager = new TransactionManager(cache);
        this.encryptionEnabled = true;
        this.encryptionKey = key;
        this.encryptionStrategy = strategy;
        storage.loadWithRecovery(cache);
        initializeIndexes();
    }

    private void initializeIndexes() {
        for (Map.Entry<String, CacheValue> entry : cache.entrySet()) {
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
        storage.saveToDisk(cache);
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
        return new HashMap<>(cache);
    }

    public Map<String, CacheValue> searchCommitted(CacheQuery query) {
        Map<String, CacheValue> results = new HashMap<>();

        if (query.getPattern() != null) {
            for (String indexedKey : keyIndex.keySet()) {
                if (indexedKey.matches(query.getPattern())) {
                    results.put(indexedKey, cache.get(indexedKey));
                }
            }
        }

        if (query.getMinValue() != null || query.getMaxValue() != null) {
            for (Map.Entry<String, Integer> entry : valueIndex.entrySet()) {
                Integer intValue = entry.getValue();
                if ((query.getMinValue() == null || intValue >= query.getMinValue()) &&
                        (query.getMaxValue() == null || intValue <= query.getMaxValue())) {
                    results.put(entry.getKey(), cache.get(entry.getKey()));
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
        return cache;
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

    public String getDbFile() {
        return dbFile;
    }
}