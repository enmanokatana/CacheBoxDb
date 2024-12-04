package org.athens.db.core;

import org.athens.utils.CacheValue;
import org.athens.db.encryption.EncryptionStrategy;

import java.io.*;
import java.util.*;

public class Storage {
    private final String dbFile;
    private boolean encryptionEnabled;
    private byte[] encryptionKey;
    private EncryptionStrategy encryptionStrategy;

    public Storage(String dbFile, boolean encryptionEnabled, byte[] encryptionKey, EncryptionStrategy encryptionStrategy) {
        this.dbFile = dbFile;
        this.encryptionEnabled = encryptionEnabled;
        this.encryptionKey = encryptionKey;
        this.encryptionStrategy = encryptionStrategy;
    }

    public Map<String, CacheValue> loadFromDisk() {
        Map<String, CacheValue> store = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String header = reader.readLine();
            if (header == null) {
                return loadOldFormat(reader);
            }
            String[] headerParts = header.split(",");
            String versionPart = headerParts[0];
            String encryptionPart = headerParts[1];
            boolean fileEncrypted = Boolean.parseBoolean(encryptionPart.split("=")[1]);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] keyValue = line.split("=", 2);
                String key = keyValue[0];
                String valueData = keyValue[1];
                if (fileEncrypted) {
                    byte[] encryptedData = Base64.getDecoder().decode(valueData);
                    byte[] decryptedData = encryptionStrategy.decrypt(encryptedData, encryptionKey);
                    String serialized = new String(decryptedData);
                    CacheValue cacheValue = CacheValue.deserialize(serialized);
                    store.put(key, cacheValue);
                } else {
                    CacheValue cacheValue = CacheValue.deserialize(valueData);
                    store.put(key, cacheValue);
                }
            }
        } catch (FileNotFoundException e) {
            createFileIfNotExists();
            loadFromDisk();
            //throw new RuntimeException("Error loading database: File not found: " + dbFile, e);
        } catch (IOException e) {
            throw new RuntimeException("Error loading database: " + e.getMessage(), e);
        }
        return store;
    }

    private void createFileIfNotExists() {
        File file = new File(dbFile);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Error creating database file: " + dbFile, e);
            }
        }
    }

    private Map<String, CacheValue> loadOldFormat(BufferedReader reader) throws IOException {
        // Implement loading logic for files without headers
        // Assume no encryption
        Map<String, CacheValue> store = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] keyValue = line.split("=", 2);
            String key = keyValue[0];
            String valueData = keyValue[1];
            CacheValue cacheValue = CacheValue.deserialize(valueData);
            store.put(key, cacheValue);
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
            writer.write("version=2,encryptionEnabled=" + encryptionEnabled + "\n");
            for (Map.Entry<String, CacheValue> entry : store.entrySet()) {
                String serializedValue = entry.getValue().serialize();
                if (encryptionEnabled) {
                    byte[] encryptedData = encryptionStrategy.encrypt(serializedValue.getBytes(), encryptionKey);
                    serializedValue = Base64.getEncoder().encodeToString(encryptedData);
                }
                writer.write(entry.getKey() + "=" + serializedValue + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving database: " + e.getMessage(), e);
        }
    }

    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public void setEncryptionStrategy(EncryptionStrategy encryptionStrategy) {
        this.encryptionStrategy = encryptionStrategy;
    }
}