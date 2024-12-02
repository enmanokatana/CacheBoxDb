package org.athens;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class CacheBox {

    private final Map<String, CacheValue> store;
    private final String dbFile;

    private final String walFile;
    private final ReentrantLock lock = new ReentrantLock();
    private final CacheValidation.ValidationManager validationManager;
    private boolean loggingEnabled; // Flag to control logging



    public CacheBox(String filename) {
        this.validationManager = new CacheValidation.ValidationManager();
        setupDefaultValidationRules();

        this.dbFile = filename;
        this.walFile = filename + ".log";

        this.store = new HashMap<>();
        loadFromDisk();
        this.loggingEnabled = false; // Default logging state


    }

    public void put(String key, CacheValue value) {
        lock.lock();
        try{
            logOperation("PUT",key,value.serialize());
            validateAndPut(key,value);
            saveToDisk();

        }finally {
            lock.unlock();
        }
    }
    private void validateAndPut(String key, CacheValue value) {
        CacheValidation.ValidationResult result = validationManager.validate(key, value);
        if (!result.isValid()) {
            throw new ValidationException("Validation failed for key '" + key + "': " + result.getErrorMessage());
        }
        store.put(key, value);
    }
    public CacheValue get(String key) {
        lock.lock();
        try{
            return store.get(key);
        }finally {
            lock.unlock();
        }
    }

    public void delete(String key) {
       lock.lock();
       try{
           logOperation("DELETE",key,null);
           store.remove(key);
           saveToDisk();
       }finally {
           lock.unlock();
       }
    }

    public boolean contains(String key) {
        lock.lock();
        try{
            return store.containsKey(key);
        }finally {
            lock.unlock();
        }
    }
    public Transaction beginTransaction(){
        return new Transaction(this);
    }

    private void recoverFromWAL() {
        if (!Files.exists(Paths.get(walFile))) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(walFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 3);
                if (parts.length >= 2) {
                    String operation = parts[0];
                    String key = parts[1];
                    String value = parts.length == 3 ? parts[2] : null;

                    if ("PUT".equals(operation) && value != null) {
                        store.put(key, CacheValue.deserialize(value));
                    } else if ("DELETE".equals(operation)) {
                        store.remove(key);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to recover from WAL: " + e.getMessage());
        }
    }
    private void loadFromDisk() {
        lock.lock();
        try {
            recoverFromWAL();
            if (!Files.exists(Paths.get(dbFile))) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        store.put(parts[0], CacheValue.deserialize(parts[1]));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading database: " + e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    public void saveToDisk() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
            for (Map.Entry<String, CacheValue> entry : store.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue().serialize());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving database: " + e.getMessage());
        }
        clearWAL();
    }
    private void clearWAL() {
        lock.lock();
        try {
            try {
                Files.deleteIfExists(Paths.get(walFile));
            } catch (IOException e) {
                System.err.println("Failed to clear WAL: " + e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    public static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("put <type> <key> <value> - Store a value of specified type");
        System.out.println("  Types: string, int, bool, list");
        System.out.println("  Example: put string name John");
        System.out.println("  Example: put int age 25");
        System.out.println("  Example: put bool active true");
        System.out.println("  Example: put list colors red,blue,green");
        System.out.println("get <key> - Retrieve a value by key");
        System.out.println("delete <key> - Delete a key-value pair");
        System.out.println("list - Show all stored key-value pairs");
        System.out.println("type <key> - Show the type of a stored value");
        System.out.println("search - Show search options");
        System.out.println("log <enable|disable> - Enable or disable Write-Ahead Logging");
        System.out.println("help - Show this help message");
        System.out.println("exit - Exit the program\n");
    }
    public Map<String, CacheValue> getStore() {
        return store;
    }

    private void setupDefaultValidationRules() {
        // Example validation rules
        validationManager.addRule(
                CacheValidation.ValidationRule.forKey("name", String.class)
                        .required()
                        .lengthBetween(2, 50)
        );

        validationManager.addRule(
                CacheValidation.ValidationRule.forKey("age", Integer.class)
                        .required()
                        .validate(age -> age >= 0)
                        .rangeBetween(0, 120)
        );

        validationManager.addRule(
                CacheValidation.ValidationRule.forKey("email", String.class)
                        .matchPattern("^[A-Za-z0-9+_.-]+@(.+)$")
        );
    }
    public Map<String, CacheValue> search(CacheQuery query) {
        lock.lock();
        try {
            Map<String, CacheValue> results = new HashMap<>();
            for (Map.Entry<String, CacheValue> entry : store.entrySet()) {
                if (matchesQuery(entry.getKey(), entry.getValue(), query)) {
                    results.put(entry.getKey(), entry.getValue());
                }
            }
            return results;
        } finally {
            lock.unlock();
        }
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
    private void logOperation(String operation, String key, String value) {
        if (!loggingEnabled) {
            return; // Skip logging if disabled
        }
        try{

            createLogFileIfNotExists(); // Ensure the log file exists

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(walFile, true))) {
                writer.write(operation + ":" + key + ":" + (value == null ? "null" : value));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to WAL: " + e.getMessage());
        }
    }
    private void createLogFileIfNotExists() {
        try {
            if (!Files.exists(Paths.get(walFile))) {
                Files.createFile(Paths.get(walFile)); // Create the log file if it doesn't exist
            }
        } catch (IOException e) {
            System.err.println("Failed to create WAL file: " + e.getMessage());
        }
    }
    public void enableLogging(boolean enable) {
        this.loggingEnabled = enable; // `loggingEnabled` is a boolean field in CacheBox
        if (enable) {
            System.out.println("Logging is now enabled.");
        } else {
            System.out.println("Logging is now disabled.");
        }
    }

}
