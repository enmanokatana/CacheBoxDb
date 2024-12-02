package org.athens;

import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        CacheBox db = new CacheBox("cachebox.cbx");
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to CacheBox v2.0 - ACID-compliant Key-Value Database");
        System.out.println("Type 'help' or '-h' for a list of commands");

        while (true) {
            System.out.print("\ndb> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+");

            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }

            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "exit", "-x":
                        System.out.println("Goodbye!");
                        return;

                    case "help", "-h":
                        printHelp();
                        break;

                    case "show", "-s":
                        System.out.println("Committed database state:");
                        Map<String, CacheValue> committedState = db.getCommittedState();
                        if (committedState.isEmpty()) {
                            System.out.println("The database is empty.");
                        } else {
                            for (Map.Entry<String, CacheValue> entry : committedState.entrySet()) {
                                System.out.printf("%s (%s): %s%n",
                                        entry.getKey(),
                                        entry.getValue().getType(),
                                        entry.getValue().asString());
                            }
                        }
                        break;

                    case "begin", "-b":
                        db.beginTransaction();
                        System.out.println("Transaction started.");
                        break;

                    case "commit", "-c":
                        db.commit();
                        System.out.println("Transaction committed.");
                        break;

                    case "rollback", "-r":
                        db.rollback();
                        System.out.println("Transaction rolled back.");
                        break;

                    case "put", "-p":
                        if (parts.length < 4) {
                            System.out.println("Usage: put <type> <key> <value>");
                            break;
                        }
                        if (!db.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }

                        String inputType = parts[1].toLowerCase();
                        String inputKey = parts[2];
                        String inputValue = input.substring(input.indexOf(parts[3])); // Handle spaces in value

                        switch (inputType) {
                            case "string":
                                db.put(inputKey, CacheValue.of(0, inputValue)); // Use version 0
                                break;
                            case "int":
                                db.put(inputKey, CacheValue.of(0, Integer.parseInt(inputValue)));
                                break;
                            case "bool":
                                db.put(inputKey, CacheValue.of(0, Boolean.parseBoolean(inputValue)));
                                break;
                            case "list":
                                db.put(inputKey, CacheValue.of(0, java.util.Arrays.asList(inputValue.split(","))));
                                break;
                            default:
                                System.out.println("Unknown type. Use: string, int, bool, or list");
                                break;
                        }
                        System.out.println("Value staged successfully (uncommitted).");
                        break;

                    case "get", "-g":
                        if (parts.length != 2) {
                            System.out.println("Usage: get <key>");
                            break;
                        }
                        if (!db.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }
                        CacheValue getValue = db.get(parts[1]);
                        if (getValue != null) {
                            System.out.println("Value: " + getValue.asString());
                        } else {
                            System.out.println("Key not found.");
                        }
                        break;

                    case "delete", "-d":
                        if (parts.length != 2) {
                            System.out.println("Usage: delete <key>");
                            break;
                        }
                        if (!db.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }
                        db.delete(parts[1]);
                        System.out.println("Key staged for deletion (uncommitted).");
                        break;

                    case "search":
                        if (parts.length < 2) {
                            System.out.println("Usage: search [options]");
                            System.out.println("Options:");
                            System.out.println("  -pattern <regex>   Search by key/value pattern");
                            System.out.println("  -range <min> <max> Search by number range");
                            System.out.println("  -type <type>       Filter by type (string/int/bool/list)");
                            System.out.println("  -staged            Search staged changes only");
                            System.out.println("  -committed         Search committed state only");
                            break;
                        }

                        CacheQuery.Builder queryBuilder = new CacheQuery.Builder();
                        boolean searchStaged = false;
                        boolean searchCommitted = false;

                        for (int i = 1; i < parts.length; i++) {
                            switch (parts[i]) {
                                case "-pattern":
                                    if (i + 1 < parts.length) {
                                        queryBuilder.withPattern(parts[++i]);
                                    }
                                    break;
                                case "-range":
                                    if (i + 2 < parts.length) {
                                        queryBuilder.withRange(Integer.parseInt(parts[++i]), Integer.parseInt(parts[++i]));
                                    }
                                    break;
                                case "-type":
                                    if (i + 1 < parts.length) {
                                        queryBuilder.withType(CacheValue.Type.valueOf(parts[++i].toUpperCase()));
                                    }
                                    break;
                                case "-staged":
                                    searchStaged = true;
                                    break;
                                case "-committed":
                                    searchCommitted = true;
                                    break;
                            }
                        }

                        try {
                            Map<String, CacheValue> results;
                            CacheQuery query = queryBuilder.build();

                            if (searchStaged) {
                                results = db.searchStaged(query);
                            } else if (searchCommitted) {
                                results = db.searchCommitted(query);
                            } else {
                                results = db.search(query); // Default: Search both
                            }

                            if (results.isEmpty()) {
                                System.out.println("No matches found.");
                            } else {
                                System.out.println("Search results:");
                                results.forEach((key1, value1) ->
                                        System.out.printf("%s (%s): %s%n", key1, value1.getType(), value1.asString()));
                            }
                        } catch (Exception e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                        break;

                    case "list", "-l":
                        if (!db.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }
                        System.out.println("Current staged state (uncommitted):");
                        for (Map.Entry<String, CacheValue> entry : db.getStagedState().entrySet()) {
                            System.out.printf("%s (%s): %s%n",
                                    entry.getKey(),
                                    entry.getValue().getType(),
                                    entry.getValue().asString());
                        }
                        break;

                    default:
                        System.out.println("Unknown command. Type 'help' or '-h' for available commands.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("begin, -b - Start a new transaction");
        System.out.println("commit, -c - Commit the current transaction");
        System.out.println("rollback, -r - Roll back the current transaction");
        System.out.println("put, -p <type> <key> <value> - Stage a value of the specified type");
        System.out.println("  Types: string, int, bool, list");
        System.out.println("  Example: put string name John");
        System.out.println("  Example: put int age 25");
        System.out.println("  Example: put bool active true");
        System.out.println("  Example: put list colors red,blue,green");
        System.out.println("get, -g <key> - Retrieve a value by key (uncommitted changes are included)");
        System.out.println("delete, -d <key> - Stage a key for deletion");
        System.out.println("list, -l - Show all staged key-value pairs (uncommitted changes are included)");
        System.out.println("show, -s - Displays the committed database state");
        System.out.println("search - Displays menu");
        System.out.println("help, -h - Show this help message");
        System.out.println("exit, -x - Exit the program");
    }
}
