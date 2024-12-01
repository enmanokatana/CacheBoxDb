package org.athens;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheBox {

    private Map<String, CacheValue> store;
    private final String dbFile;

    private final CacheValidation.ValidationManager validationManager;



    public CacheBox(String filename) {
        this.validationManager = new CacheValidation.ValidationManager();
        setupDefaultValidationRules();

        this.dbFile = filename;
        this.store = new HashMap<>();
        loadFromDisk();

    }

    public void put(String key, CacheValue value) {
        CacheValidation.ValidationResult result = validationManager.validate(key, value);
        if (!result.isValid()) {
            throw new ValidationException(
                    "Validation failed for key '" + key + "': " +
                            result.getErrorMessage()
            );
        }
        store.put(key, value);
        saveToDisk();
    }

    public CacheValue get(String key) {
        return store.get(key);
    }

    public void delete(String key) {
        store.remove(key);
        saveToDisk();
    }

    public boolean contains(String key) {
        return store.containsKey(key);
    }

    private void loadFromDisk() {
        try {
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
            }
        } catch (IOException e) {
            System.err.println("Error loading database: " + e.getMessage());
        }
    }

    private void saveToDisk() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))) {
            for (Map.Entry<String, CacheValue> entry : store.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue().serialize());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving database: " + e.getMessage());
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
        Map<String, CacheValue> results = new HashMap<>();

        for (Map.Entry<String, CacheValue> entry : store.entrySet()) {
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
