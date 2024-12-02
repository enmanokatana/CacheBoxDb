package org.athens;

import java.io.*;
import java.util.*;

public class Storage {
    private final String dbFile;

    public Storage(String dbFile) {
        this.dbFile = dbFile;
    }

    public Map<String, CacheValue> loadFromDisk() {
        Map<String, CacheValue> store = new HashMap<>();
        try {
            if (!new File(dbFile).exists()) {
                return store;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Split the line by "=", expecting key and value parts
                    String[] keyValue = line.split("=", 2);
                    if (keyValue.length != 2) {
                        throw new IllegalArgumentException("Invalid format: " + line);
                    }
                    String key = keyValue[0];
                    String valueData = keyValue[1];
                    // Split the value data by ":", expecting type, version, and value
                    String[] parts = valueData.split(":", 3);
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Invalid format: " + valueData);
                    }
                    CacheValue.Type type = CacheValue.Type.valueOf(parts[0]);
                    int version = Integer.parseInt(parts[1]);
                    String value = parts[2];
                    // Deserialize based on type
                    CacheValue cacheValue;
                    switch (type) {
                        case STRING:
                            cacheValue = CacheValue.of(version, value);
                            break;
                        case INTEGER:
                            cacheValue = CacheValue.of(version, Integer.parseInt(value));
                            break;
                        case BOOLEAN:
                            cacheValue = CacheValue.of(version, Boolean.parseBoolean(value));
                            break;
                        case LIST:
                            cacheValue = CacheValue.of(version, Arrays.asList(value.split(",")));
                            break;
                        case NULL:
                            cacheValue = CacheValue.ofNull(version);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown type: " + type);
                    }
                    store.put(key, cacheValue);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading database: " + e.getMessage());
        }
        return store;
    }
    public void loadWithRecovery(Map<String, CacheValue> globalStore) {
        // Load committed data from the database file
        globalStore.putAll(loadFromDisk());

        // Replay log file to apply pending changes
        replayLog(globalStore);
    }
    private void replayLog(Map<String, CacheValue> globalStore) {
        File logFile = new File("transaction_log.txt");
        if (!logFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            Map<Integer, TransactionLog> ongoingTransactions = new HashMap<>();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 4);
                if (parts.length < 2) continue;

                String operation = parts[0];
                int txId = Integer.parseInt(parts[1]);

                switch (operation) {
                    case "BEGIN":
                        ongoingTransactions.put(txId, new TransactionLog());
                        break;
                    case "PUT":
                        if (ongoingTransactions.containsKey(txId)) {
                            ongoingTransactions.get(txId).puts.put(parts[2], CacheValue.deserialize(parts[3]));
                        }
                        break;
                    case "DELETE":
                        if (ongoingTransactions.containsKey(txId)) {
                            ongoingTransactions.get(txId).deletions.add(parts[2]);
                        }
                        break;
                    case "COMMIT":
                        TransactionLog transactionLog = ongoingTransactions.remove(txId);
                        if (transactionLog != null) {
                            // Apply changes to globalStore
                            for (Map.Entry<String, CacheValue> entry : transactionLog.puts.entrySet()) {
                                globalStore.put(entry.getKey(), entry.getValue());
                            }
                            for (String key : transactionLog.deletions) {
                                globalStore.remove(key);
                            }
                        }
                        break;
                    case "ROLLBACK":
                        ongoingTransactions.remove(txId);
                        break;
                }
            }

            // Rollback any ongoing transactions
            for (TransactionLog transactionLog : ongoingTransactions.values()) {
                for (Map.Entry<String, CacheValue> entry : transactionLog.puts.entrySet()) {
                    globalStore.remove(entry.getKey());
                }
                for (String key : transactionLog.deletions) {
                    globalStore.put(key, transactionLog.deletedValues.get(key));
                }
            }
        } catch (IOException e) {
            // Handle exception
        }
    }
    private static class TransactionLog {
        Map<String, CacheValue> puts = new HashMap<>();
        Set<String> deletions = new HashSet<>();
        Map<String, CacheValue> deletedValues = new HashMap<>();
    }

    public void saveToDisk(Map<String, CacheValue> store) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
            for (Map.Entry<String, CacheValue> entry : store.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue().serialize());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving database: " + e.getMessage());
        }
    }
}