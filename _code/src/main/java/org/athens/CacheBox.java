package org.athens;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CacheBox {
    private static final Logger logger = LoggerFactory.getLogger(CacheBox.class);
    private final Map<String, CacheValue> store;
    private final String dbFile;
    private final String walFile;
    private final ReentrantLock lock = new ReentrantLock();
    private final CacheValidation.ValidationManager validationManager;
    private boolean loggingEnabled;

    public CacheBox(String dbFile, String walFile) {
        this.store = new HashMap<>();
        this.dbFile = dbFile;
        this.walFile = walFile;
        this.validationManager = new CacheValidation.ValidationManager();
        setupDefaultValidationRules();
        loadFromDisk();
        this.loggingEnabled = false;
    }

    public void put(String key, CacheValue value) {
        lock.lock();
        try {
            validateAndPut(key, value);
            if (loggingEnabled) {
                logOperation("PUT", key, value.serialize());
            }
            saveToDisk();
        } finally {
            lock.unlock();
        }
    }

    public CacheValue get(String key) {
        lock.lock();
        try {
            return store.get(key);
        } finally {
            lock.unlock();
        }
    }

    public void delete(String key) {
        lock.lock();
        try {
            CacheValue value = store.remove(key);
            if (loggingEnabled) {
                logOperation("DELETE", key, value != null ? value.serialize() : "NULL:null");
            }
            saveToDisk();
        } finally {
            lock.unlock();
        }
    }

    public void loadFromDisk() {
        lock.lock();
        try {
            recoverFromWAL();
            readDataFromDbFile();
        } finally {
            lock.unlock();
        }
    }

    public void saveToDisk() {
        lock.lock();
        try {
            writeDataToDbFile();
            clearWAL();
        } finally {
            lock.unlock();
        }
    }

    private void recoverFromWAL() {
        List<String> operations = readOperationsFromWAL();
        for (String operation : operations) {
            String[] parts = operation.split(":", 3);
            if (parts.length < 3) {
                continue;
            }
            String opType = parts[0];
            String key = parts[1];
            String data = parts[2];
            switch (opType) {
                case "PUT":
                    CacheValue value = CacheValue.deserialize(data);
                    store.put(key, value);
                    break;
                case "DELETE":
                    store.remove(key);
                    break;
            }
        }
    }

    private List<String> readOperationsFromWAL() {
        List<String> operations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(walFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                operations.add(line);
            }
        } catch (IOException e) {
            logger.error("Failed to read from WAL: " + e.getMessage());
        }
        return operations;
    }

    private void readDataFromDbFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    store.put(parts[0], CacheValue.deserialize(parts[1]));
                }
            }
        } catch (IOException e) {
            logger.error("Error loading database: " + e.getMessage());
        }
    }

    private void writeDataToDbFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
            for (Map.Entry<String, CacheValue> entry : store.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue().serialize());
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("Error saving database: " + e.getMessage());
        }
    }

    private void clearWAL() {
        try {
            Files.deleteIfExists(Paths.get(walFile));
        } catch (IOException e) {
            logger.error("Failed to clear WAL: " + e.getMessage());
        }
    }

    private void logOperation(String operation, String key, String value) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(walFile, true))) {
            writer.write(operation + ":" + key + ":" + value);
            writer.newLine();
        } catch (IOException e) {
            logger.error("Failed to write to WAL: " + e.getMessage());
        }
    }

    private void setupDefaultValidationRules() {
        // Define default validation rules
    }

    private void validateAndPut(String key, CacheValue value) {
        // Apply validation rules
    }
}
