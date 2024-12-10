package org.athens.network;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.google.protobuf.ByteString;
import org.athens.keyvalue.KeyValueDB;
import org.athens.keyvalue.KeyValueDB.Request;
import org.athens.keyvalue.KeyValueDB.Response;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import static org.athens.keyvalue.KeyValueDB.*;

public class Client {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Client.class);

    static {
        setupLogging();
    }

    private static void setupLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);

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

    public static void main(String[] args) {
        String key = "testKey";
        String value = "testValue";

        try {
            testBasicPutAndGet(key, value);
        } catch (IOException e) {
            logger.error("IOException occurred during test:", e);
        }
    }

    private static void testBasicPutAndGet(String key, String value) throws IOException {
        testBasicPut(key, value);
        testBasicGet(key);
    }

    private static void testBasicPut(String key, String value) throws IOException {
        logger.info("Starting PUT operation for key: {}", key);

        String transactionId = UUID.randomUUID().toString();
        Request putRequest = buildPutRequest(key, value, transactionId);
        Response putResponse = sendRequest(putRequest);

        logger.info("PUT response received. Success: {}", putResponse.getPutResponse().getSuccess());

        Request commitRequest = buildCommitRequest(transactionId);
        Response commitResponse = sendRequest(commitRequest);

        logger.info("Commit response received. Success: {}", commitResponse.getCommitResponse().getSuccess());
    }

    private static void testBasicGet(String key) throws IOException {
        logger.info("Starting GET operation for key: {}", key);

        String transactionId = UUID.randomUUID().toString();
        Request getRequest = buildGetRequest(key, transactionId);
        Response getResponse = sendRequest(getRequest);

        logger.info("GET response received. Success: {}, Key Exists: {}", getResponse.getGetResponse().getSuccess(), getResponse.getGetResponse().getKeyExists());
        if (getResponse.getGetResponse().getKeyExists()) {
            logger.debug("Retrieved Value: {}", new String(getResponse.getGetResponse().getValue().toByteArray()));
        }
    }

    private static Request buildPutRequest(String key, String value, String transactionId) {
        byte[] valueBytes = value.getBytes();
        return Request.newBuilder()
                .setTransactionId(transactionId)
                .setPutRequest(PutRequest.newBuilder()
                        .setKey(key)
                        .setValue(ByteString.copyFrom(valueBytes))
                        .build())
                .build();
    }

    private static Request buildGetRequest(String key, String transactionId) {
        return Request.newBuilder()
                .setTransactionId(transactionId)
                .setGetRequest(GetRequest.newBuilder()
                        .setKey(key)
                        .build())
                .build();
    }

    private static Request buildCommitRequest(String transactionId) {
        return Request.newBuilder()
                .setTransactionId(transactionId)
                .setCommitRequest(CommitRequest.getDefaultInstance())
                .build();
    }

    private static Response sendRequest(Request request) throws IOException {
        try (SocketChannel channel = SocketChannel.open(new InetSocketAddress("localhost", 8080))) {
            // Set a read timeout of 5 seconds
            channel.socket().setSoTimeout(5000);

            byte[] requestData = request.toByteArray();
            ByteBuffer header = ByteBuffer.allocate(4).putInt(requestData.length);
            ByteBuffer message = ByteBuffer.wrap(requestData);

            channel.write(header);
            channel.write(message);

            ByteBuffer responseHeader = ByteBuffer.allocate(4);
            channel.read(responseHeader);
            responseHeader.flip();
            int responseLength = responseHeader.getInt();

            ByteBuffer responseBody = ByteBuffer.allocate(responseLength);
            int bytesRead = channel.read(responseBody);
            while (bytesRead < responseLength) {
                bytesRead += channel.read(responseBody);
            }
            responseBody.flip();

            byte[] responseBytes = new byte[responseLength];
            responseBody.get(responseBytes);

            return Response.parseFrom(responseBytes);
        } catch (IOException e) {
            logger.error("IOException occurred while sending request:", e);
            throw e;
        }
    }
}