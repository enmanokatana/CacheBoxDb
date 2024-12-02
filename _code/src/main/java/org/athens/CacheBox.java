package org.athens;

import java.util.HashMap;
import java.util.Map;

public class CacheBox {
    private final Map<String, CacheValue> globalStore;
    private final Storage storage;
    private final TransactionManager transactionManager;

    public CacheBox(String dbFile) {
        this.storage = new Storage(dbFile);
        this.globalStore = storage.loadFromDisk();
        this.transactionManager = new TransactionManager(globalStore);
    }

    public void put(String key, CacheValue value) {
        transactionManager.getActiveTransaction().put(key, value);
    }

    public CacheValue get(String key) {
        return transactionManager.getActiveTransaction().get(key);
    }

    public void delete(String key) {
        transactionManager.getActiveTransaction().delete(key);
    }

    public void commit() {
        transactionManager.commit();
        storage.saveToDisk(globalStore); // Persist changes
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
    /**
     * Retrieves the current staged state of the active transaction.
     * @return A map representing the current staged changes.
     */
    public Map<String, CacheValue> getStagedState() {
        if (!isTransactionActive()) {
            throw new IllegalStateException("No active transaction.");
        }
        return transactionManager.getActiveTransaction().getStagedChanges();
    }
    /**
     * Retrieves the current committed state of the database (global store).
     * This excludes uncommitted changes from any active transaction.
     */
    public Map<String, CacheValue> getCommittedState() {
        return new HashMap<>(globalStore); // Return a copy to prevent external modification
    }

    /**
     * Search the committed state of the database using the provided query.
     */
    public Map<String, CacheValue> searchCommitted(CacheQuery query) {
        return search(query, globalStore);
    }

    /**
     * Search the staged changes in the current transaction.
     */
    public Map<String, CacheValue> searchStaged(CacheQuery query) {
        if (!isTransactionActive()) {
            throw new IllegalStateException("No active transaction.");
        }
        return search(query, transactionManager.getActiveTransaction().getStagedChanges());
    }

    /**
     * Search both committed and staged states, combining results.
     */
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

    /**
     * Perform a search on the given data map using the provided query.
     */
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
}
