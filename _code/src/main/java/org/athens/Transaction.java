package org.athens;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private final List<CacheOp> operations;
    private final CacheBox cacheBox;

    public Transaction(CacheBox cacheBox) {
        this.cacheBox = cacheBox;
        this.operations = new ArrayList<>();
    }

    public void put(String key, CacheValue value) {
        PutOperation op = new PutOperation(key, value);
        operations.add(op);
        op.execute(cacheBox);
    }

    public void delete(String key) {
        CacheValue value = cacheBox.get(key);
        DeleteOperation op = new DeleteOperation(key, value);
        operations.add(op);
        op.execute(cacheBox);
    }

    public void commit() {
        // Implement commit logic
    }

    public void rollback() {
        // Implement rollback logic
    }
}