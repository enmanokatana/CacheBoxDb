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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    public static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static ShardedCacheBox cacheBox;
    private static final int PORT = 20029;

    private static final ExecutorService clientHandlerPool =
            Executors.newThreadPerTaskExecutor(
                    new ThreadFactory() {
                        private final AtomicInteger threadCounter = new AtomicInteger(1);
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(r);
                            thread.setName("Client_Handler-"+threadCounter.getAndIncrement());
                            thread.setUncaughtExceptionHandler((t,e)->
                                    logger.error("Uncaught exception in thread {}",t.getName(),e)
                            );
                            return thread;
                        }
                    }
            );

    public static void main(String[] args) {
        initializeCacheBox();
        logger.info("Starting Cache Box server on port {}", PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            logger.info("Init server shutdown ...");
            clientHandlerPool.shutdown();
            try{
                if (!clientHandlerPool.awaitTermination(5, TimeUnit.SECONDS)){
                    clientHandlerPool.shutdownNow();
                }

            }catch (InterruptedException e){
                clientHandlerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));

        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            logger.info("Server started on port {}",PORT);
            while(!Thread.currentThread().isInterrupted()){
                try{
                    Socket clientSocket = serverSocket.accept();
                    logger.info("New client connection from {}", clientSocket.getRemoteSocketAddress());
                    clientHandlerPool.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    logger.error("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            logger.error("Server startup failed", e);
        }
    }

    private static class ClientHandler implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        private static final int SOCKET_TIMEOUT = 30000;
        private final ExecutorService requestExecutor = Executors.newSingleThreadExecutor();


        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                clientSocket.setSoTimeout(SOCKET_TIMEOUT);
            } catch (IOException e) {
                logger.warn("Could not set socket timeout", e);
            }
        }

        @Override
        public void run() {
            try (
                    InputStream inputStream = clientSocket.getInputStream();
                    OutputStream outputStream = clientSocket.getOutputStream()
            ) {
                logger.debug("Client handler started for {}", clientSocket.getRemoteSocketAddress());
                while (!Thread.currentThread().isInterrupted() &&!clientSocket.isClosed()) {
                    Future<String> responseFuture = requestExecutor.submit(() -> {
                        List<String> command = RequestParser.parseRequest(inputStream);
                        return processCommand(command);
                    });
                    try {
                        String response = responseFuture.get(5, TimeUnit.SECONDS);
                        outputStream.write(response.getBytes());
                        outputStream.flush();
                    } catch (TimeoutException e) {
                        logger.warn("Request processing timed out");
                        outputStream.write("Request timed out".getBytes());
                        break;
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Error processing request", e);
                        break;
                    }

                }
            } catch (IOException e) {
                logger.error("Client handler error: {}", e.getMessage(), e);
            }finally {
                requestExecutor.shutdown();
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.error("Error closing client socket", e);
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
