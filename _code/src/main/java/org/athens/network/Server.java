package org.athens.network;

import org.athens.db.encryption.AESEncryptionStrategy;
import org.athens.db.encryption.EncryptionStrategy;
import org.athens.db.shrading.LoadBalancer;
import org.athens.db.shrading.ShardedCacheBox;
import org.athens.network.commands.CommandFactory;
import org.athens.utils.KeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    public static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static ShardedCacheBox cacheBox;
    private static final int PORT = 20029;
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 4;
    private static final int KEEP_ALIVE_TIME = 60;
    private static final int CONNECTION_BACKLOG = 50;

    private static final ThreadPoolExecutor connectionPool = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactory() {
                private final AtomicInteger threadCounter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("Connection_Handler-" + threadCounter.getAndIncrement());
                    thread.setUncaughtExceptionHandler((t, e) ->
                            logger.error("Uncaught exception in thread {}", t.getName(), e)
                    );
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static void main(String[] args) {
        initializeCacheBox();
        logger.info("Starting Cache Box server on port {} with {} core threads", PORT, CORE_POOL_SIZE);

        setupShutdownHook();

        try (ServerSocket serverSocket = new ServerSocket(PORT, CONNECTION_BACKLOG)) {
            logger.info("Server started on port {}", PORT);
            startMetricsReporter();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    connectionPool.execute(new ClientHandler(clientSocket));
                } catch (RejectedExecutionException e) {
                    logger.error("Connection rejected - server overloaded", e);
                } catch (IOException e) {
                    logger.error("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            logger.error("Server startup failed", e);
        }
    }

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Initiating server shutdown...");
            connectionPool.shutdown();
            try {
                if (!connectionPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    connectionPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                connectionPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Server shutdown complete");
        }));
    }

    private static void startMetricsReporter() {
        ScheduledExecutorService metricsReporter = Executors.newSingleThreadScheduledExecutor();
        metricsReporter.scheduleAtFixedRate(() -> {
            logger.info("Server metrics - Active threads: {}, Queue size: {}, Completed tasks: {}, Total tasks: {}",
                    connectionPool.getActiveCount(),
                    connectionPool.getQueue().size(),
                    connectionPool.getCompletedTaskCount(),
                    connectionPool.getTaskCount());
        }, 0, 60, TimeUnit.SECONDS);
    }

    private static class ClientHandler implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        private static final int SOCKET_TIMEOUT = 30000;
        private static final int REQUEST_TIMEOUT = 5000;

        private final Socket clientSocket;
        private final ExecutorService requestProcessor;
        private final String clientId;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientId = clientSocket.getRemoteSocketAddress().toString();
            this.requestProcessor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("Request_Processor-" + clientId);
                return thread;
            });

            try {
                clientSocket.setSoTimeout(SOCKET_TIMEOUT);
                clientSocket.setTcpNoDelay(true);
                clientSocket.setKeepAlive(true);
            } catch (IOException e) {
                logger.warn("Could not set socket options for client {}", clientId, e);
            }
        }

        @Override
        public void run() {
            try (
                    InputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());
                    OutputStream outputStream = new BufferedOutputStream(clientSocket.getOutputStream())
            ) {
                logger.debug("Started handling client {}");
                processClientRequests(inputStream, outputStream);
            } catch (IOException e) {
                logger.error("Error handling client {}", clientId, e);
            } finally {
                cleanup();
            }
        }

        private void processClientRequests(InputStream inputStream, OutputStream outputStream) throws IOException {
            while (!Thread.currentThread().isInterrupted() && !clientSocket.isClosed()) {
                try {
                    clientSocket.setSoTimeout(REQUEST_TIMEOUT);
                    List<String> command = RequestParser.parseRequest(inputStream);
                    String response = processCommand(command);
                    synchronized (outputStream) {
                        outputStream.write(response.getBytes());
                        outputStream.flush();
                    }
                } catch (SocketTimeoutException e) {
                    logger.warn("Idle connection timed out for client {}", clientId);
                    break;
                } catch (IOException e) {
                    logger.error("Error reading from client {}", clientId, e);
                    break;
                } catch (Exception e) {
                    handleRequestError(e, outputStream);
                    break;
                }
            }
        }

        private void handleRequestError(Exception e, OutputStream outputStream) {
            try {
                String errorResponse;
                if (e instanceof TimeoutException) {
                    errorResponse = RequestParser.encodeError("Request timed out");
                    logger.warn("Request timeout for client {}", clientId);
                } else {
                    errorResponse = RequestParser.encodeError("Internal server error");
                    logger.error("Error processing request for client {}", clientId, e);
                }

                synchronized (outputStream) {
                    outputStream.write(errorResponse.getBytes());
                    outputStream.flush();
                }
            } catch (IOException writeError) {
                logger.error("Error sending error response to client {}", clientId, writeError);
            }
        }

        private void cleanup() {
            requestProcessor.shutdownNow();
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                logger.error("Error closing connection for client {}", clientId, e);
            }
            logger.debug("Cleaned up resources for client {}", clientId);
        }

        private String processCommand(List<String> commandParts) {
            if (commandParts == null || commandParts.isEmpty()) {
                return RequestParser.encodeError("Empty command");
            }

            try {
                String action = commandParts.get(0).toUpperCase();
                CacheCommand cmd = CommandFactory.getCommand(action);

                if (cmd == null) {
                    logger.warn("Unknown command from client {}: {}", clientId, action);
                    return RequestParser.encodeError("Unknown command: " + action);
                }

                return cmd.execute(commandParts.subList(1, commandParts.size()), cacheBox);
            } catch (Exception e) {
                logger.error("Error processing command from client {}: {}", clientId, commandParts, e);
                return RequestParser.encodeError("Server error: " + e.getMessage());
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
