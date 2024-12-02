package org.athens;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Transaction {
    private final Map<String, CacheValue> globalStore;
    private final Map<String, CacheValue> stagedChanges = new HashMap<>();
    private final Map<String, CacheValue> stagedDeletions = new HashMap<>();
    private final Map<String, Integer> readVersions = new HashMap<>();
    private final File logFile = new File("transaction_log.txt");
    private int txId;
    public Transaction(Map<String, CacheValue> globalStore) {
        this.globalStore = globalStore;
        this.txId = TxIdManager.getInstance().getNextTxId();

    }

    public void put(String key, CacheValue value) {
        writeLogEntry("PUT:" + txId + ":" + key + ":" + value.serialize());

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
            writeLogEntry("PUT:" + txId + ":" + entry.getKey() + ":" + entry.getValue().serialize());
        }
        for (String key : stagedDeletions.keySet()) {
            writeLogEntry("DELETE:" + txId + ":" + key);
        }
        // First, process deletions
        for (String key : stagedDeletions.keySet()) {
            CacheValue current = globalStore.get(key);
            int expectedVersion = readVersions.getOrDefault(key, current != null ? current.getVersion() : 0);
            if (current != null && current.getVersion() != expectedVersion) {
                throw new ConcurrencyException("Conflict on key " + key);
            }
            globalStore.remove(key);
        }
        // Then, process changes
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
        // Log commit marker
        writeLogEntry("COMMIT:" + txId);

        // Checkpoint txId
        TxIdManager.getInstance().checkpoint();
        stagedChanges.clear();
        stagedDeletions.clear();
        readVersions.clear();
    }
    public void rollback() {
        writeLogEntry("ROLLBACK:" + txId);

        stagedChanges.clear();
        stagedDeletions.clear();
        readVersions.clear();
    }

    public Map<String, CacheValue> getStagedChanges() {
        return stagedChanges;
    }
    private void writeLogEntry(String entry) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(entry + "\n");
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to log file: " + e.getMessage());
        }
    }
}