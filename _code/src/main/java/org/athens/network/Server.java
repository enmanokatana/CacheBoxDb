package org.athens.network;

import org.athens.db.encryption.AESEncryptionStrategy;
import org.athens.db.encryption.EncryptionStrategy;
import org.athens.db.shrading.LoadBalancer;
import org.athens.db.shrading.ShardedCacheBox;
import org.athens.network.commands.CommandFactory;
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
    public static final Logger logger = LoggerFactory.getLogger(Server.class);

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
            try {
                String action = commandParts.getFirst().toUpperCase();

                CacheCommand cmd = CommandFactory.getCommand(action);
                if (cmd == null) {
                    logger.warn("Unknown command: {}", action);
                    return RequestParser.encodeError("Unknown command");
                }

                return cmd.execute(commandParts.subList(1, commandParts.size()),cacheBox);
            } catch (Exception e) {
                logger.error("Error processing commandParts: {}", commandParts.toString(), e);
                return RequestParser.encodeError("Server error occurred");
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
