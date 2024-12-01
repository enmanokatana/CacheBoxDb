package org.athens;

import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        CacheBox db = new CacheBox("cachebox.cbx");
        Scanner scanner = new Scanner(System.in);

        System.out.println("CacheBox v1.0 - Multi-type Key-Value Store");
        System.out.println("Type 'help' for available commands");

        while (true) {
            System.out.print("\ncbox> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+", 4);

            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }

            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "exit":
                        System.out.println("Goodbye!");
                        return;

                    case "help":
                        CacheBox.printHelp();
                        break;

                    case "put":
                        if (parts.length < 4) {
                            System.out.println("Usage: put <type> <key> <value>");
                            break;
                        }
                        String type = parts[1].toLowerCase();
                        String key = parts[2];
                        String value = parts[3];

                        switch (type) {
                            case "string":
                                db.put(key, CacheValue.of(value));
                                break;
                            case "int":
                                db.put(key, CacheValue.of(Integer.parseInt(value)));
                                break;
                            case "bool":
                                db.put(key, CacheValue.of(Boolean.parseBoolean(value)));
                                break;
                            case "list":
                                db.put(key, CacheValue.of(Arrays.asList(value.split(","))));
                                break;
                            default:
                                System.out.println("Unknown type. Use: string, int, bool, or list");
                                break;
                        }
                        System.out.println("Stored successfully");
                        break;

                    case "get":
                        if (parts.length != 2) {
                            System.out.println("Usage: get <key>");
                            break;
                        }
                        CacheValue getValue = db.get(parts[1]);
                        if (getValue != null) {
                            System.out.println("Value: " + getValue.asString());
                        } else {
                            System.out.println("Key not found");
                        }
                        break;

                    case "delete":
                        if (parts.length != 2) {
                            System.out.println("Usage: delete <key>");
                            break;
                        }
                        if (db.contains(parts[1])) {
                            db.delete(parts[1]);
                            System.out.println("Deleted successfully");
                        } else {
                            System.out.println("Key not found");
                        }
                        break;

                    case "list":
                        if (db.getStore().isEmpty()) {
                            System.out.println("Database is empty");
                        } else {
                            for (Map.Entry<String, CacheValue> entry : db.getStore().entrySet()) {
                                System.out.printf("%s (%s): %s%n",
                                        entry.getKey(),
                                        entry.getValue().getType(),
                                        entry.getValue().asString());
                            }
                        }
                        break;

                    case "type":
                        if (parts.length != 2) {
                            System.out.println("Usage: type <key>");
                            break;
                        }
                        CacheValue typeValue = db.get(parts[1]);
                        if (typeValue != null) {
                            System.out.println("Type: " + typeValue.getType());
                        } else {
                            System.out.println("Key not found");
                        }
                        break;

                    default:
                        System.out.println("Unknown command. Type 'help' for available commands");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

    }
}