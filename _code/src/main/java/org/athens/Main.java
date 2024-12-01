package org.athens;
import org.athens.Store;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Store db = new Store("mydb.txt");
        Scanner scanner = new Scanner(System.in);

        System.out.println("C A C H E B O X ...");
        System.out.println("Simple Key Value Database");
        System.out.println("Type 'help' or '-h' for available commands");

        while(true){
            System.out.println("\ndb>");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+", 3);
            if (parts.length == 0 || parts[0].isEmpty()) {
                continue;
            }
            String command = parts[0].toLowerCase();
            try {
                switch (command) {
                    case "exit":
                        System.out.println("Goodbye!");
                        return;

                    case "help", "-h":
                        Store.printHelp();
                        break;

                    case "put":
                        if (parts.length != 3) {
                            System.out.println("Usage: put <key> <value>");
                            break;
                        }
                        db.put(parts[1], parts[2]);
                        System.out.println("Stored successfully");
                        break;

                    case "get":
                        if (parts.length != 2) {
                            System.out.println("Usage: get <key>");
                            break;
                        }
                        String value = db.get(parts[1]);
                        if (value != null) {
                            System.out.println("Value: " + value);
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
                        db.listAll();
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