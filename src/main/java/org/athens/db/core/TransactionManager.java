package org.athens.db.core;

import org.athens.utils.CacheValue;

public class TransactionManager {
    private final LRUCache<String, CacheValue> globalStore; // Persistent store
    private final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<>();

    public TransactionManager(LRUCache<String, CacheValue> globalStore) {
        this.globalStore = globalStore;
    }

    public Transaction beginTransaction() {
        if (currentTransaction.get() != null) {
            throw new IllegalStateException("Transaction already active on this thread");
        }
        Transaction transaction = new Transaction(globalStore);
        currentTransaction.set(transaction);
        return transaction;
    }

    public void commit() {
        Transaction transaction = currentTransaction.get();
        if (transaction == null) {
            throw new IllegalStateException("No active transaction to commit");
        }
        transaction.commit();
        currentTransaction.remove();
    }

    public void rollback() {
        Transaction transaction = currentTransaction.get();
        if (transaction == null) {
            throw new IllegalStateException("No active transaction to rollback");
        }
        transaction.rollback();
        currentTransaction.remove();
    }

    public boolean isTransactionActive() {
        return currentTransaction.get() != null;
    }

    public Transaction getActiveTransaction() {
        Transaction transaction = currentTransaction.get();
        if (transaction == null) {
            throw new IllegalStateException("No active transaction");
        }
        return transaction;
    }
}