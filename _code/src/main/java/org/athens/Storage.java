package org.athens;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

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
                    String[] parts = line.split(":", 3);
                    if (parts.length == 3) {
                        store.put(parts[0], CacheValue.deserialize(parts[1] + ":" + parts[2]));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading database: " + e.getMessage());
        }
        return store;
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