package org.athens;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Initialize the CacheBox with file paths for the DB and WAL
        String dbFile = "cachebox.db";
        String walFile = "cachebox.wal";
        CacheBox cacheBox = new CacheBox(dbFile, walFile);



        // Create scanner for user input
        Scanner scanner = new Scanner(System.in);
        System.out.println("CacheBox CLI v1.0");
        System.out.println("Type 'help' for available commands.");

        // Main loop for accepting commands
        while (true) {
            System.out.print("\ncbox> ");
            String input = scanner.nextLine().trim();

            // Exit the application
            if (input.equalsIgnoreCase("exit")) {
                cacheBox.saveToDisk();
                System.out.println("Exiting CacheBox...");
                break;
            }
            // Show help
            else if (input.equalsIgnoreCase("help")) {
                printHelp();
            }
            // Put command to insert a key-value pair
            else if (input.startsWith("put")) {
                handlePutCommand(input, cacheBox);
            }
            // Get command to retrieve a value by key
            else if (input.startsWith("get")) {
                handleGetCommand(input, cacheBox);
            }
            // Delete command to remove a key-value pair
            else if (input.startsWith("delete")) {
                handleDeleteCommand(input, cacheBox);
            }
            // List command to show all keys
            else if (input.equalsIgnoreCase("list")) {
                handleListCommand(cacheBox);
            }
            // Search command to search for keys
            else if (input.startsWith("search")) {
                handleSearchCommand(input, cacheBox);
            }
            // Begin transaction
            else if (input.equalsIgnoreCase("begin transaction")) {
                cacheBox.beginTransaction();
                System.out.println("Transaction started.");
            }
            // Commit the transaction
            else if (input.equalsIgnoreCase("commit")) {
                cacheBox.commit();
                System.out.println("Transaction committed.");
            }
            // Rollback the transaction
            else if (input.equalsIgnoreCase("rollback")) {
                cacheBox.rollback();
                System.out.println("Transaction rolled back.");
            }
            // Show the WAL log file content
            else if (input.startsWith("log")) {
                cacheBox.displayLog();
            }
            // Unknown command
            else {
                System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }

        scanner.close();
    }

    // Display available commands
    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("put <key> <value> - Adds a key-value pair to the cache.");
        System.out.println("get <key> - Retrieves the value for a given key.");
        System.out.println("delete <key> - Removes a key from the cache.");
        System.out.println("list - Lists all keys in the cache.");
        System.out.println("search <key> - Searches for a key.");
        System.out.println("begin transaction - Starts a new transaction.");
        System.out.println("commit - Commits the current transaction.");
        System.out.println("rollback - Rolls back the current transaction.");
        System.out.println("log - Displays the operations in the WAL.");
        System.out.println("exit - Exits the CacheBox CLI.");
    }

    // Handle 'put' command
    private static void handlePutCommand(String input, CacheBox cacheBox) {
        String[] parts = input.split(" ", 3);
        if (parts.length < 3) {
            System.out.println("Usage: put <key> <value>");
            return;
        }
        String key = parts[1];
        String value = parts[2];
        cacheBox.put(key, new CacheValue(value));
        System.out.println("Put " + key + "=" + value);
    }

    // Handle 'get' command
    private static void handleGetCommand(String input, CacheBox cacheBox) {
        String[] parts = input.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("Usage: get <key>");
            return;
        }
        String key = parts[1];
        CacheValue value = cacheBox.get(key);
        if (value != null) {
            System.out.println("Got " + key + "=" + value.getData());
        } else {
            System.out.println("Key not found: " + key);
        }
    }

    // Handle 'delete' command
    private static void handleDeleteCommand(String input, CacheBox cacheBox) {
        String[] parts = input.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("Usage: delete <key>");
            return;
        }
        String key = parts[1];
        cacheBox.delete(key);
        System.out.println("Deleted key: " + key);
    }

    // Handle 'list' command
    private static void handleListCommand(CacheBox cacheBox) {
        System.out.println("Listing all keys:");
        cacheBox.store.forEach((key, value) -> System.out.println(key));
    }

    // Handle 'search' command (search for a key)
    private static void handleSearchCommand(String input, CacheBox cacheBox) {
        String[] parts = input.split(" ", 2);
        if (parts.length < 2) {
            System.out.println("Usage: search <key>");
            return;
        }
        String key = parts[1];
        CacheValue value = cacheBox.get(key);
        if (value != null) {
            System.out.println("Found " + key + "=" + value.getData());
        } else {
            System.out.println("No such key: " + key);
        }
    }
}
