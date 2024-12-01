package org.athens;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Store {
    private Map<String, String> store;
    private final String dbFile;

    public Store(String filename) {
        this.dbFile = filename;
        this.store = new HashMap<>();
        loadFromDisk();
    }
    public void put(String key, String value) {
        store.put(key, value);
        saveToDisk();
    }

    public String get(String key) {
        return store.get(key);
    }

    public void delete(String key) {
        store.remove(key);
        saveToDisk();
    }

    public boolean contains(String key) {
        return store.containsKey(key);
    }

    public void listAll() {
        if (store.isEmpty()) {
            System.out.println("Database is empty");
            return;
        }

        for (Map.Entry<String, String> entry : store.entrySet()) {
            System.out.printf("Key: %s, Value: %s%n", entry.getKey(), entry.getValue());
        }
    }
    private void loadFromDisk(){
        try{
            if (!Files.exists(Paths.get(dbFile))){
                return;
            }
            try(BufferedReader reader = new BufferedReader(new FileReader(dbFile))){
                String line;
                while((line  = reader.readLine() )!=null){
                    String[] parts = line.split("=",2);
                    if (parts.length == 2){
                        store.put(parts[0],parts[1]);
                    }
                }
            }

        }catch(IOException e){
            System.err.println("Error laoding Database" + e.getMessage());

        }
    }

    private void saveToDisk(){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile))){
            for (Map.Entry<String, String> entry : store.entrySet()){
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }

      } catch (IOException e) {
        System.err.println("Error saving database: " + e.getMessage());
    }
    }
    public static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("put <key> <value> - Store a key-value pair");
        System.out.println("get <key> - Retrieve a value by key");
        System.out.println("delete <key> - Delete a key-value pair");
        System.out.println("list - Show all stored key-value pairs");
        System.out.println("help - Show this help message");
        System.out.println("exit - Exit the program\n");
    }
}
