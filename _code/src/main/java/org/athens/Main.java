package org.athens;

import org.athens.utils.AESEncryptionStrategy;
import org.athens.utils.EncryptionStrategy;
import org.athens.utils.NoEncryptionStrategy;
import org.athens.utils.XOREncryptionStrategy;

import java.security.SecureRandom;
import java.util.*;

public class Main {
    private static ShardedCacheBox cacheBox;
    private static LoadBalancer loadBalancer;

    public static void main(String[] args) {
        initializeCacheBox();
        MonitoringService monitoringService = new MonitoringService(cacheBox.getShards());
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
                        cacheBox.getCommittedState().forEach((key, value) ->
                                System.out.printf("%s (%s): %s%n", key, value.getType(), value.asString()));
                        break;

                    case "begin", "-b":
                        cacheBox.beginTransaction();
                        System.out.println("Transaction started.");
                        break;
                    case "snapshot_performance","sp":
                        System.out.println("Snapshot of current performance metrics:");
                        System.out.println(monitoringService.getSnapshotMetrics());
                        break;

                    case "live_performance","lp":
                        if (monitoringService.isLiveMonitoringActive()) {
                            System.out.println("Live monitoring is already active.");
                            break;
                        }
                        monitoringService.setLiveMonitoringActive(true);
                        System.out.println("Starting live performance monitoring...");
                        monitoringService.startLiveMonitoring();
                        break;

                    case "stop_lp":
                        if (!monitoringService.isLiveMonitoringActive()) {
                            System.out.println("Live monitoring is not active.");
                            break;
                        }
                        monitoringService.setLiveMonitoringActive(false);
                        monitoringService.stopLiveMonitoring();
                        break;

                    case "commit", "-c":
                        cacheBox.commit();
                        System.out.println("Transaction committed.");
                        break;

                    case "rollback", "-r":
                        cacheBox.rollback();
                        System.out.println("Transaction rolled back.");
                        break;

                    case "put", "-p":
                        if (parts.length < 4) {
                            System.out.println("Usage: put <type> <key> <value>");
                            break;
                        }
                        if (!cacheBox.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }

                        String inputType = parts[1].toLowerCase();
                        String inputKey = parts[2];
                        String inputValue = input.substring(input.indexOf(parts[3])); // Handle spaces in value

                        switch (inputType) {
                            case "string":
                                cacheBox.put(inputKey, CacheValue.of(0, inputValue)); // Use version 0
                                break;
                            case "int":
                                cacheBox.put(inputKey, CacheValue.of(0, Integer.parseInt(inputValue)));
                                break;
                            case "bool":
                                cacheBox.put(inputKey, CacheValue.of(0, Boolean.parseBoolean(inputValue)));
                                break;
                            case "list":
                                cacheBox.put(inputKey, CacheValue.of(0, Arrays.asList(inputValue.split(","))));
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
                        if (!cacheBox.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }
                        CacheValue getValue = cacheBox.get(parts[1]);
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
                        if (!cacheBox.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }
                        cacheBox.delete(parts[1]);
                        System.out.println("Key staged for deletion (uncommitted).");
                        break;

                    case "encrypt":
                        if (parts.length < 2) {
                            printHelp();
                            break;
                        }
                        String subCommand = parts[1];
                        switch (subCommand.toLowerCase()) {
                            case "enable":
                                cacheBox.setEncryptionEnabled(true);
                                System.out.println("Encryption enabled.");
                                break;
                            case "disable":
                                cacheBox.setEncryptionEnabled(false);
                                System.out.println("Encryption disabled.");
                                break;
                            case "set_algorithm":
                                if (parts.length < 3) {
                                    System.out.println("Usage: encrypt set_algorithm <algorithm_keyword>");
                                    break;
                                }
                                String algorithmKeyword = parts[2].toUpperCase();
                                if (ENCRYPTION_ALGORITHMS.containsKey(algorithmKeyword)) {
                                    if (algorithmKeyword.equals("AES")) {
                                        if (cacheBox.getEncryptionKey() == null || cacheBox.getEncryptionKey().length != 16) {
                                            System.out.println("AES requires a 16-byte key.");
                                            System.out.println("Do you want to set a key now? (yes/no)");
                                            String choice = scanner.nextLine().trim().toLowerCase();
                                            if (choice.equals("yes")) {
                                                while (true) {
                                                    System.out.println("Do you want to generate a random key or enter your own? (generate/enter)");
                                                    String keyOption = scanner.nextLine().trim().toLowerCase();
                                                    if (keyOption.equals("generate")) {
                                                        byte[] key = new byte[16];
                                                        new SecureRandom().nextBytes(key);
                                                        cacheBox.setEncryptionKey(key);
                                                        System.out.println("Generated AES key: " + new String(key));
                                                        break;
                                                    } else if (keyOption.equals("enter")) {
                                                        while (true) {
                                                            System.out.println("Enter a 16-byte key (or 'cancel' to exit):");
                                                            String keyInput = scanner.nextLine().trim();
                                                            if (keyInput.equals("cancel")) {
                                                                System.out.println("AES setup aborted.");
                                                                break;
                                                            }
                                                            byte[] keyBytes = keyInput.getBytes();
                                                            if (keyBytes.length == 16) {
                                                                cacheBox.setEncryptionKey(keyBytes);
                                                                break;
                                                            } else {
                                                                System.out.println("Invalid key length. Please enter a 16-byte key.");
                                                            }
                                                        }
                                                        if (cacheBox.getEncryptionKey() != null && cacheBox.getEncryptionKey().length == 16) {
                                                            break;
                                                        } else {
                                                            System.out.println("AES setup aborted.");
                                                            break;
                                                        }
                                                    } else {
                                                        System.out.println("Invalid choice. Please enter 'generate' or 'enter'.");
                                                    }
                                                }
                                            } else if (choice.equals("no")) {
                                                System.out.println("AES setup aborted due to missing key.");
                                                break;
                                            } else {
                                                System.out.println("Invalid input. Please enter 'yes' or 'no'.");
                                                break;
                                            }
                                        }
                                        if (cacheBox.getEncryptionKey() != null && cacheBox.getEncryptionKey().length == 16) {
                                            try {
                                                EncryptionStrategy strategy = new AESEncryptionStrategy();
                                                cacheBox.setEncryptionStrategy(strategy);
                                                cacheBox.setEncryptionEnabled(true);
                                                System.out.println("Encryption algorithm set to AES.");
                                            } catch (Exception e) {
                                                System.out.println("Error setting AES encryption strategy: " + e.getMessage());
                                            }
                                        } else {
                                            System.out.println("AES setup aborted due to invalid key.");
                                        }
                                    } else {
                                        try {
                                            Class<? extends EncryptionStrategy> strategyClass = ENCRYPTION_ALGORITHMS.get(algorithmKeyword);
                                            EncryptionStrategy strategy = strategyClass.getDeclaredConstructor().newInstance();
                                            cacheBox.setEncryptionStrategy(strategy);
                                            cacheBox.setEncryptionEnabled(true);
                                            System.out.println("Encryption algorithm set to " + algorithmKeyword);
                                        } catch (Exception e) {
                                            System.out.println("Error setting encryption strategy: " + e.getMessage());
                                        }
                                    }
                                } else {
                                    System.out.println("Unknown algorithm: " + algorithmKeyword);
                                    System.out.println("Available algorithms: NO, XOR, AES");
                                }
                                break;
                            case "set_key":
                                if (parts.length < 3) {
                                    System.out.println("Usage: encrypt set_key <key>");
                                    break;
                                }
                                String keyInput = parts[2];
                                byte[] keyBytes = keyInput.getBytes();
                                if (cacheBox.getEncryptionStrategy() instanceof AESEncryptionStrategy && keyBytes.length != 16) {
                                    System.out.println("Invalid key length for AES. Please provide a 16-byte key.");
                                } else {
                                    cacheBox.setEncryptionKey(keyBytes);
                                    System.out.println("Encryption key set.");
                                }
                                break;
                            case "generate_key":
                                if (cacheBox.getEncryptionStrategy() instanceof AESEncryptionStrategy) {
                                    byte[] key = new byte[16];
                                    new Random().nextBytes(key);
                                    cacheBox.setEncryptionKey(key);
                                    System.out.println("Generated AES key: " + new String(key));
                                } else {
                                    System.out.println("AES algorithm must be set before generating a key.");
                                }
                                break;
                        }
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
                                results = cacheBox.searchStaged(query);
                            } else if (searchCommitted) {
                                results = cacheBox.searchCommitted(query);
                            } else {
                                results = cacheBox.search(query); // Default: Search both
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
                        if (!cacheBox.isTransactionActive()) {
                            System.out.println("Error: Start a transaction using 'begin' first.");
                            break;
                        }
                        System.out.println("Current staged state (uncommitted):");
                        cacheBox.getStagedState().forEach((key, value) ->
                                System.out.printf("%s (%s): %s%n", key, value.getType(), value.asString()));
                        break;

                    default:
                        System.out.println("Unknown command. Type 'help' or '-h' for available commands.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void initializeCacheBox() {
        byte[] encryptionKey = new SecureRandom().generateSeed(16);
        EncryptionStrategy encryptionStrategy = new AESEncryptionStrategy();
        boolean encryptionEnabled = true;
        int maxSize = 1000;

        List<ShardedCacheBox> cacheBoxes = Arrays.asList(
                new ShardedCacheBox(4, "shard1_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize),
                new ShardedCacheBox(4, "shard2_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize),
                new ShardedCacheBox(4, "shard3_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize)
        );

        loadBalancer = new LoadBalancer(cacheBoxes, Arrays.asList(1, 1, 1)); // Equal weights for simplicity
        cacheBox = loadBalancer.getNextCacheBox();
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
        System.out.println("encrypt - Manage encryption settings");
        System.out.println("  encrypt enable - Enable encryption with the current strategy and key");
        System.out.println("  encrypt disable - Disable encryption");
        System.out.println("  encrypt set_algorithm <algorithm_keyword> - Set the encryption algorithm");
        System.out.println("    Available algorithms: NO, XOR, AES");
        System.out.println("  encrypt set_key <key> - Set the encryption key (16-byte for AES)");
        System.out.println("  encrypt generate_key - Generate a random 16-byte key for AES");
        System.out.println("snapshot performance - Display a snapshot of current performance metrics");
        System.out.println("live performance - Start live performance monitoring");
        System.out.println("stop live performance - Stop live performance monitoring");        System.out.println("help, -h - Show this help message");
        System.out.println("exit, -x - Exit the program");
    }

    private static final Map<String, Class<? extends EncryptionStrategy>> ENCRYPTION_ALGORITHMS = new HashMap<>();

    static {
        ENCRYPTION_ALGORITHMS.put("NO", NoEncryptionStrategy.class);
        ENCRYPTION_ALGORITHMS.put("XOR", XOREncryptionStrategy.class);
        ENCRYPTION_ALGORITHMS.put("AES", AESEncryptionStrategy.class);
    }
}