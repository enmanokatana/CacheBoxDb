package org.athens.network;

import org.athens.db.encryption.AESEncryptionStrategy;
import org.athens.db.encryption.EncryptionStrategy;
import org.athens.db.shrading.LoadBalancer;
import org.athens.db.shrading.ShardedCacheBox;
import org.athens.utils.CacheValue;
import org.athens.utils.KeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static ShardedCacheBox cacheBox;

    public static void main(String[] args) {
        int port = 20029;
        initializeCacheBox();
        logger.info("Starting Cache Box server on port {}", port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connection established: {}", clientSocket.getRemoteSocketAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage(), e);
        }
    }

    private static class ClientHandler implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    InputStream inputStream = clientSocket.getInputStream();
                    OutputStream outputStream = clientSocket.getOutputStream()
            ) {
                logger.debug("Client handler started for {}", clientSocket.getRemoteSocketAddress());
                while (true) {
                    try {
                        // Parse the client request
                        List<String> command = RequestParser.parseRequest(inputStream);
                        logger.debug("Received command: {}", command);

                        // Process the command and get the response
                        String response = processCommand(command);
                        logger.debug("Processed command. Response: {}", response);

                        // Send the response back to the client
                        outputStream.write(response.getBytes());
                    } catch (IOException e) {
                        String errorResponse = RequestParser.encodeError("Invalid request: " + e.getMessage());
                        outputStream.write(errorResponse.getBytes());
                        logger.warn("Malformed input from {}: {}", clientSocket.getRemoteSocketAddress(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.error("Client handler error: {}", e.getMessage(), e);
            } finally {
                try {
                    clientSocket.close();
                    logger.info("Closed connection for {}", clientSocket.getRemoteSocketAddress());
                } catch (IOException e) {
                    logger.error("Error closing client socket: {}", e.getMessage(), e);
                }
            }
        }

        private String processCommand(List<String> commandParts) {
            if (commandParts.isEmpty()) {
                logger.warn("Empty command received");
                return RequestParser.encodeError("Empty command");
            }

            String action = commandParts.get(0).toUpperCase();
            logger.debug("Processing action: {}", action);

            try {
                switch (action) {
                    case "PUT":
                        if (commandParts.size() < 4) {
                            logger.warn("PUT command has insufficient arguments");
                            return RequestParser.encodeError("PUT requires action, type, key, and value");
                        }

                        cacheBox.beginTransaction();
                        if (!cacheBox.isTransactionActive()) {
                            logger.warn("No transaction active for PUT command");
                            return RequestParser.encodeError("No transaction active. Start with 'begin'.");
                        }

                        String inputType = commandParts.get(1).toLowerCase();
                        String inputKey = commandParts.get(2);
                        String inputValue = commandParts.get(3);
                        logger.debug("PUT command details: type={}, key={}, value={}", inputType, inputKey, inputValue);

                        switch (inputType) {
                            case "string":
                                cacheBox.put(inputKey, CacheValue.of(0, inputValue));
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
                                logger.warn("Unsupported type for PUT command: {}", inputType);
                                return RequestParser.encodeError("Unknown type. Supported types: string, int, bool, list");
                        }

                        cacheBox.commit();
                        logger.info("PUT command successful for key: {}", inputKey);
                        return RequestParser.encodeSimpleString("OK");

                    case "GET":
                        if (commandParts.size() < 2) {
                            logger.warn("GET command has insufficient arguments");
                            return RequestParser.encodeError("GET requires a key");
                        }

                        cacheBox.beginTransaction();
                        if (!cacheBox.isTransactionActive()) {
                            logger.warn("No transaction active for GET command");
                            return RequestParser.encodeError("No transaction active. Start with 'begin'.");
                        }

                        String key = commandParts.get(1);
                        logger.debug("GET command for key: {}", key);

                        CacheValue getValue = cacheBox.get(key);
                        cacheBox.commit();

                        if (getValue != null) {
                            logger.info("GET command successful. Key: {}, Value: {}", key, getValue.asString());
                            return RequestParser.encodeBulkString(getValue.asString());
                        } else {
                            logger.info("GET command failed. Key not found: {}", key);
                            return RequestParser.encodeSimpleString("NULL");
                        }

                    default:
                        logger.warn("Unknown command received: {}", action);
                        return RequestParser.encodeError("Unknown command: " + action);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid number format in command: {}", e.getMessage(), e);
                return RequestParser.encodeError("Invalid number format: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error while processing command: {}", e.getMessage(), e);
                return RequestParser.encodeError("Unexpected error: " + e.getMessage());
            }
        }
    }

    private static void initializeCacheBox() {
        byte[] encryptionKey = KeyManager.getOrCreateEncryptionKey();
        EncryptionStrategy encryptionStrategy = new AESEncryptionStrategy();
        boolean encryptionEnabled = true;
        int maxSize = 1000;

        List<ShardedCacheBox> cacheBoxes = Arrays.asList(
                new ShardedCacheBox(4, "db_files/shard1_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize),
                new ShardedCacheBox(4, "db_files/shard2_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize),
                new ShardedCacheBox(4, "db_files/shard3_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize)
        );

        LoadBalancer loadBalancer = new LoadBalancer(cacheBoxes, Arrays.asList(1, 1, 1));
        cacheBox = loadBalancer.getNextCacheBox();
        logger.info("CacheBox initialized with shards");
    }
}
