package org.athens.network;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.google.protobuf.ByteString;
import org.athens.db.shrading.ShardedCacheBox;
import org.athens.db.encryption.AESEncryptionStrategy;
import org.athens.db.encryption.EncryptionStrategy;
import org.athens.utils.CacheValue;
import org.athens.keyvalue.KeyValueDB;
import org.athens.keyvalue.KeyValueDB.Request;
import org.athens.keyvalue.KeyValueDB.Response;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
public class Server implements AutoCloseable {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Server.class);

    // Configurable constants
    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024; // Reduced from 64MB
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
    private static final long TRANSACTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final int CONNECTION_TIMEOUT_MS = 10_000; // 5 seconds connection timeout
    private static final int MAX_CONNECTIONS = 100; // Maximum concurrent connections

    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final AtomicBoolean isRunning;
    private final AtomicLong totalRequests;
    private final AtomicLong failedRequests;
    private final BlockingQueue<ResponseWrapper> responseQueue;
    private final ShardedCacheBox shardedCacheBox;
    private final Map<String, Long> transactionTimeouts;
    private final ScheduledExecutorService transactionCleaner;
    private final AtomicLong currentConnections;
    private static class ResponseWrapper {
        private final Response response;
        private final SocketChannel channel;

        ResponseWrapper(Response response, SocketChannel channel) {
            this.response = response;
            this.channel = channel;
        }

        public Response getResponse() { return response; }
        public SocketChannel getChannel() { return channel; }
    }
    public static class ServerBuilder {
        private int port = 8080;
        private String host = "localhost";
        private String dbFilePrefix = "shard";
        private boolean encryptionEnabled = true;
        private byte[] encryptionKey = new byte[16];
        private int initialShards = 4;
        private int maxSize = 1000;
        private EncryptionStrategy encryptionStrategy = new AESEncryptionStrategy();

        public ServerBuilder port(int port) {
            this.port = port;
            return this;
        }

        public ServerBuilder host(String host) {
            this.host = host;
            return this;
        }

        public Server build() throws IOException {
            return new Server(this);
        }
    }

    private Server(ServerBuilder builder) throws IOException {
        // Setup enhanced logging
        setupEnhancedLogging();

        this.isRunning = new AtomicBoolean(true);
        this.totalRequests = new AtomicLong(0);
        this.failedRequests = new AtomicLong(0);
        this.currentConnections = new AtomicLong(0);
        this.responseQueue = new LinkedBlockingQueue<>();
        this.transactionTimeouts = new ConcurrentHashMap<>();

        try {
            this.selector = Selector.open();
            this.serverChannel = ServerSocketChannel.open();
            this.serverChannel.configureBlocking(false);

            // Set socket options for better performance and security
            this.serverChannel.socket().setReuseAddress(true);
            this.serverChannel.socket().setSoTimeout(CONNECTION_TIMEOUT_MS);

            this.serverChannel.bind(new InetSocketAddress(builder.host, builder.port), MAX_CONNECTIONS);
            this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Server initialized on {}:{}", builder.host, builder.port);
        } catch (IOException e) {
            logger.error("Failed to initialize server", e);
            throw e;
        }

        this.shardedCacheBox = new ShardedCacheBox(
                builder.initialShards,
                builder.dbFilePrefix,
                builder.encryptionStrategy,
                builder.encryptionEnabled,
                builder.encryptionKey,
                builder.maxSize
        );

        // Enhanced transaction cleaner with more robust error handling
        this.transactionCleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "transaction-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        this.transactionCleaner.scheduleAtFixedRate(
                this::cleanupStaleTransactions,
                TRANSACTION_TIMEOUT_MS,
                TRANSACTION_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
        );
    }    private void cleanupStaleTransactions() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = transactionTimeouts.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (currentTime - entry.getValue() > TRANSACTION_TIMEOUT_MS) {
                shardedCacheBox.rollback();
                it.remove();
                logger.warn("Transaction {} timed out and was rolled back", entry.getKey());
            }
        }
    }
    // Enhanced logging setup method
    private void setupEnhancedLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(consoleAppender);
    }
    private Response handlePutRequest(Request request) {
        try {
            if (!shardedCacheBox.isTransactionActive()) {
                shardedCacheBox.beginTransaction();
            }

            String key = request.getPutRequest().getKey();
            byte[] valueBytes = request.getPutRequest().getValue().toByteArray();
            CacheValue value = CacheValue.of(1, valueBytes);

            shardedCacheBox.put(key, value);

            return Response.newBuilder()
                    .setPutResponse(KeyValueDB.PutResponse.newBuilder()
                            .setSuccess(true)
                            .setKey(key)
                            .setStatus(KeyValueDB.PutResponse.PutStatus.CREATED)
                            .build())
                    .build();
        } catch (Exception e) {
            logger.error("Error handling put request", e);
            return createErrorResponse("Failed to put value: " + e.getMessage(), 500);
        }
    }
    public void run() {
        Thread responseHandler = startResponseHandler();

        try {
            while (isRunning.get()) {
                try {
                    if (selector.select(100) == 0) continue;

                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (!key.isValid()) continue;

                        try {
                            if (key.isAcceptable()) {
                                acceptConnection(key);
                            } else if (key.isReadable()) {
                                handleRead(key);
                            }
                        } catch (CancelledKeyException e) {
                            logger.warn("Key cancelled while processing", e);
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error in main server loop", e);
                    failedRequests.incrementAndGet();
                }
            }
        } finally {
            shutdown();
            responseHandler.interrupt();
        }
    }

    private Thread startResponseHandler() {
        Thread handler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ResponseWrapper wrapper = responseQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (wrapper != null) {
                        logger.debug("Sending response to channel: {}", wrapper.getChannel());
                        sendResponse(wrapper.getChannel(), wrapper.getResponse());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in response handler", e);
                }
            }
        }, "response-handler");
        handler.start();
        return handler;
    }

    private void acceptConnection(SelectionKey key) {
        try {
            if (currentConnections.get() >= MAX_CONNECTIONS) {
                logger.warn("Maximum connection limit reached. Rejecting new connection.");
                return;
            }

            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);

            // Configure client socket for better performance
            clientChannel.socket().setTcpNoDelay(true);
            clientChannel.socket().setKeepAlive(true);
            clientChannel.socket().setSoTimeout(CONNECTION_TIMEOUT_MS);

            clientChannel.register(selector, SelectionKey.OP_READ);
            currentConnections.incrementAndGet();

            logger.info("New connection accepted from: {}", clientChannel.getRemoteAddress());
        } catch (IOException e) {
            logger.error("Error accepting connection", e);
            failedRequests.incrementAndGet();
        }
    }

    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();

        try {
            ByteBuffer headerBuffer = ByteBuffer.allocate(4);
            int bytesRead = clientChannel.read(headerBuffer);

            if (bytesRead == -1) {
                closeChannel(clientChannel);
                return;
            }

            if (headerBuffer.hasRemaining()) {
                return; // Not enough data yet
            }

            headerBuffer.flip();
            int messageLength = headerBuffer.getInt();

            logger.debug("Received message length: {}", messageLength);

            // Added stricter size checking
            if (messageLength <= 0 || messageLength > MAX_MESSAGE_SIZE) {
                logger.error("Invalid message size: {}", messageLength);
                closeChannel(clientChannel);
                return;
            }

            ByteBuffer messageBuffer = ByteBuffer.allocate(messageLength);
            bytesRead = clientChannel.read(messageBuffer);

            if (bytesRead == -1) {
                closeChannel(clientChannel);
                return;
            }

            if (!messageBuffer.hasRemaining()) {
                messageBuffer.flip();
                byte[] data = new byte[messageBuffer.remaining()];
                messageBuffer.get(data);

                // Process the request asynchronously
                processRequest(clientChannel, data);
            }
        } catch (IOException e) {
            logger.error("Error reading from channel", e);
            closeChannel(clientChannel);
        }
    }    private void processRequest(SocketChannel clientChannel, byte[] data) {
        totalRequests.incrementAndGet();

        try {
            Request request = Request.parseFrom(data);
            String transactionId = request.getTransactionId();

            // Update or create transaction timeout
            if (transactionId != null && !transactionId.isEmpty()) {
                transactionTimeouts.put(transactionId, System.currentTimeMillis());
            }

            Response response;
            switch (request.getRequestTypeCase()) {
                case PUT_REQUEST:
                    response = handlePutRequest(request);
                    break;
                case GET_REQUEST:
                    response = handleGetRequest(request);
                    break;
                case DELETE_REQUEST:
                    response = handleDeleteRequest(request);
                    break;
                case COMMIT_REQUEST:
                    response = handleCommitRequest(request);
                    break;
                case ROLLBACK_REQUEST:
                    response = handleRollbackRequest(request);
                    break;
                default:
                    response = createErrorResponse("Invalid request type", 400);
            }

            responseQueue.offer(new ResponseWrapper(response, clientChannel));
        } catch (Exception e) {
            logger.error("Error processing request", e);
            failedRequests.incrementAndGet();
            responseQueue.offer(new ResponseWrapper(
                    createErrorResponse("Internal server error: " + e.getMessage(), 500),
                    clientChannel
            ));
        }
    }    private Response handleGetRequest(Request request) {
        try {
            if (!shardedCacheBox.isTransactionActive()) {
                shardedCacheBox.beginTransaction();
            }

            String key = request.getGetRequest().getKey();
            CacheValue value = shardedCacheBox.get(key);

            if (value == null) {
                return Response.newBuilder()
                        .setGetResponse(KeyValueDB.GetResponse.newBuilder()
                                .setSuccess(false)
                                .setKeyExists(false)
                                .build())
                        .build();
            }

            return Response.newBuilder()
                    .setGetResponse(KeyValueDB.GetResponse.newBuilder()
                            .setSuccess(true)
                            .setKey(key)
                            .setValue(ByteString.copyFrom((byte[]) value.getValue()))
                            .setKeyExists(true)
                            .build())
                    .build();
        } catch (Exception e) {
            logger.error("Error handling get request", e);
            return createErrorResponse("Failed to get value: " + e.getMessage(), 500);
        }
    }

    private Response handleDeleteRequest(Request request) {
        try {
            if (!shardedCacheBox.isTransactionActive()) {
                shardedCacheBox.beginTransaction();
            }

            String key = request.getDeleteRequest().getKey();
            CacheValue existingValue = shardedCacheBox.get(key);

            if (existingValue == null && request.getDeleteRequest().getIfExists()) {
                return Response.newBuilder()
                        .setDeleteResponse(KeyValueDB.DeleteResponse.newBuilder()
                                .setSuccess(true)
                                .setWasDeleted(false)
                                .build())
                        .build();
            }

            shardedCacheBox.delete(key);

            return Response.newBuilder()
                    .setDeleteResponse(KeyValueDB.DeleteResponse.newBuilder()
                            .setSuccess(true)
                            .setWasDeleted(true)
                            .build())
                    .build();
        } catch (Exception e) {
            logger.error("Error handling delete request", e);
            return createErrorResponse("Failed to delete value: " + e.getMessage(), 500);
        }
    }

    private Response handleCommitRequest(Request request) {
        try {
            shardedCacheBox.commit();
            String transactionId = request.getTransactionId();
            transactionTimeouts.remove(transactionId);

            return Response.newBuilder()
                    .setCommitResponse(KeyValueDB.CommitResponse.newBuilder()
                            .setSuccess(true)
                            .setTransactionId(transactionId)
                            .build())
                    .build();
        } catch (Exception e) {
            logger.error("Error handling commit request", e);
            return createErrorResponse("Failed to commit transaction: " + e.getMessage(), 500);
        }
    }


    private Response handleRollbackRequest(Request request) {
        try {
            shardedCacheBox.rollback();
            String transactionId = request.getTransactionId();
            transactionTimeouts.remove(transactionId);

            return Response.newBuilder()
                    .setRollbackResponse(KeyValueDB.RollbackResponse.newBuilder()
                            .setSuccess(true)
                            .setTransactionId(transactionId)
                            .build())
                    .build();
        } catch (Exception e) {
            logger.error("Error handling rollback request", e);
            return createErrorResponse("Failed to rollback transaction: " + e.getMessage(), 500);
        }
    }

    private Response createErrorResponse(String message, int code) {
        return Response.newBuilder()
                .setError(KeyValueDB.Error.newBuilder()
                        .setCode(code)
                        .setMessage(message)
                        .setErrorType(KeyValueDB.Error.ErrorType.INTERNAL_ERROR)
                        .build())
                .build();
    }



    private void sendResponse(SocketChannel channel, Response response) {
        try {
            byte[] data = response.toByteArray();
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(data.length);
            lengthBuffer.flip();

            ByteBuffer[] buffers = new ByteBuffer[]{
                    lengthBuffer,
                    ByteBuffer.wrap(data)
            };

            long bytesWritten = 0;
            while (bytesWritten < data.length + 4) {
                bytesWritten += channel.write(buffers);
            }
        } catch (IOException e) {
            logger.error("Error sending response", e);
            closeChannel(channel);
        }
    }

    private void closeChannel(SocketChannel channel) {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
                currentConnections.decrementAndGet();
               // logger.info("Channel closed: {}", channel.getRemoteAddress());
            }
        } catch (IOException e) {
            logger.error("Error closing channel", e);
        }
    }

    @Override
    public void close() {
        logger.info("Initiating server shutdown");
        isRunning.set(false);

        // Shutdown transaction cleaner
        transactionCleaner.shutdown();
        try {
            if (!transactionCleaner.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                transactionCleaner.shutdownNow();
                logger.warn("Transaction cleaner did not terminate cleanly");
            }
        } catch (InterruptedException e) {
            transactionCleaner.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error("Server shutdown interrupted", e);
        }

        // Close network resources
        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (IOException e) {
            logger.error("Error during final shutdown", e);
        }

        logger.info("Server shutdown complete. Total requests: {}, Failed requests: {}",
                totalRequests.get(), failedRequests.get());
    }

    private void shutdown() {
        isRunning.set(false);

        try {
            serverChannel.close();
            selector.close();
        } catch (IOException e) {
            logger.error("Error during shutdown", e);
        }
    }

    public static void main(String[] args) throws IOException {
        try (Server server = new ServerBuilder()
                .port(8080)
                .host("localhost")
                .build()) {
            server.run();
        }
    }
}