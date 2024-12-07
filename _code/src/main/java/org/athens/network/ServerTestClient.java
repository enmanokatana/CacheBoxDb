package org.athens.network;

import com.google.protobuf.ByteString;
import org.athens.keyvalue.KeyValueDB;
import org.athens.keyvalue.KeyValueDB.Request;
import org.athens.keyvalue.KeyValueDB.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;


public class ServerTestClient {
    private static final Logger logger = LoggerFactory.getLogger(ServerTestClient.class);

    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000; // 1 second between retries
    private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024; // Reduced from 64MB

    public static void main(String[] args) {
        try {
            // Test Basic Put and Get
            testBasicPutAndGet();

            // Test Delete
          //  testDelete();

            // Test Transaction Commit
            //testTransactionCommit();

            // Test Transaction Rollback
            //testTransactionRollback();

            // Test Error Scenarios
            //testErrorScenarios();
        } catch (Exception e) {
            logger.error("Test suite failed", e);
        }
    }

    private static void testBasicPutAndGet() throws IOException {
        System.out.println("=== Testing Basic Put and Get ===");
        String key = "test_key";
        byte[] value = "Hello, Athens Database!".getBytes();

        // Put Request
        Request putRequest = Request.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setPutRequest(KeyValueDB.PutRequest.newBuilder()
                        .setKey(key)
                        .setValue(ByteString.copyFrom(value))
                        .build())
                .build();

        Response putResponse = sendRequest(putRequest);
        System.out.println("Put Response: " + putResponse.getPutResponse().getSuccess());

        // Commit Transaction
        Request commitRequest = Request.newBuilder()
                .setTransactionId(putRequest.getTransactionId())
                .setCommitRequest(KeyValueDB.CommitRequest.getDefaultInstance())
                .build();
        Response commitResponse = sendRequest(commitRequest);
        System.out.println("Commit Response: " + commitResponse.getCommitResponse().getSuccess());

        // Get Request
        Request getRequest = Request.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setGetRequest(KeyValueDB.GetRequest.newBuilder()
                        .setKey(key)
                        .build())
                .build();

        Response getResponse = sendRequest(getRequest);
        System.out.println("Get Response Success: " + getResponse.getGetResponse().getSuccess());
        System.out.println("Retrieved Value: " +
                new String(getResponse.getGetResponse().getValue().toByteArray()));
    }

    private static void testDelete() throws IOException {
        System.out.println("\n=== Testing Delete ===");
        String key = "delete_key";
        byte[] value = "To be deleted".getBytes();

        // Put Request
        Request putRequest = Request.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setPutRequest(KeyValueDB.PutRequest.newBuilder()
                        .setKey(key)
                        .setValue(ByteString.copyFrom(value))
                        .build())
                .build();

        sendRequest(putRequest);

        // Commit Transaction
        Request commitRequest = Request.newBuilder()
                .setTransactionId(putRequest.getTransactionId())
                .setCommitRequest(KeyValueDB.CommitRequest.getDefaultInstance())
                .build();
        sendRequest(commitRequest);

        // Delete Request
        Request deleteRequest = Request.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setDeleteRequest(KeyValueDB.DeleteRequest.newBuilder()
                        .setKey(key)
                        .setIfExists(true)
                        .build())
                .build();

        Response deleteResponse = sendRequest(deleteRequest);
        System.out.println("Delete Response Success: " + deleteResponse.getDeleteResponse().getSuccess());
        System.out.println("Was Deleted: " + deleteResponse.getDeleteResponse().getWasDeleted());

        // Verify deletion by attempting to get
        Request getRequest = Request.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setGetRequest(KeyValueDB.GetRequest.newBuilder()
                        .setKey(key)
                        .build())
                .build();

        Response getResponse = sendRequest(getRequest);
        System.out.println("Get After Delete - Key Exists: " + getResponse.getGetResponse().getKeyExists());
    }

    private static void testTransactionCommit() throws IOException {
        System.out.println("\n=== Testing Transaction Commit ===");
        String key1 = "transaction_key1";
        String key2 = "transaction_key2";

        // Transaction ID
        String transactionId = UUID.randomUUID().toString();

        // Put Requests in same transaction
        Request put1Request = Request.newBuilder()
                .setTransactionId(transactionId)
                .setPutRequest(KeyValueDB.PutRequest.newBuilder()
                        .setKey(key1)
                        .setValue(ByteString.copyFrom("Value 1".getBytes()))
                        .build())
                .build();

        Request put2Request = Request.newBuilder()
                .setTransactionId(transactionId)
                .setPutRequest(KeyValueDB.PutRequest.newBuilder()
                        .setKey(key2)
                        .setValue(ByteString.copyFrom("Value 2".getBytes()))
                        .build())
                .build();

        sendRequest(put1Request);
        sendRequest(put2Request);

        // Commit Transaction
        Request commitRequest = Request.newBuilder()
                .setTransactionId(transactionId)
                .setCommitRequest(KeyValueDB.CommitRequest.getDefaultInstance())
                .build();

        Response commitResponse = sendRequest(commitRequest);
        System.out.println("Commit Response Success: " + commitResponse.getCommitResponse().getSuccess());
    }

    private static void testTransactionRollback() throws IOException {
        System.out.println("\n=== Testing Transaction Rollback ===");
        String key = "rollback_key";

        // Transaction ID
        String transactionId = UUID.randomUUID().toString();

        // Put Request
        Request putRequest = Request.newBuilder()
                .setTransactionId(transactionId)
                .setPutRequest(KeyValueDB.PutRequest.newBuilder()
                        .setKey(key)
                        .setValue(ByteString.copyFrom("Rollback Value".getBytes()))
                        .build())
                .build();

        sendRequest(putRequest);

        // Rollback Transaction
        Request rollbackRequest = Request.newBuilder()
                .setTransactionId(transactionId)
                .setRollbackRequest(KeyValueDB.RollbackRequest.getDefaultInstance())
                .build();

        Response rollbackResponse = sendRequest(rollbackRequest);
        System.out.println("Rollback Response Success: " + rollbackResponse.getRollbackResponse().getSuccess());

        // Verify rollback by attempting to get
        Request getRequest = Request.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setGetRequest(KeyValueDB.GetRequest.newBuilder()
                        .setKey(key)
                        .build())
                .build();

        Response getResponse = sendRequest(getRequest);
        System.out.println("Get After Rollback - Key Exists: " + getResponse.getGetResponse().getKeyExists());
    }

    private static void testErrorScenarios() throws IOException {
        System.out.println("\n=== Testing Error Scenarios ===");

        // Invalid Request Type
        Request invalidRequest = Request.newBuilder().build();
        Response errorResponse = sendRequest(invalidRequest);
        System.out.println("Invalid Request Error Code: " +
                (errorResponse.hasError() ? errorResponse.getError().getCode() : "No Error"));
    }

    private static Response sendRequest(Request request) throws IOException {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try (SocketChannel socketChannel = SocketChannel.open()) {
                // Set connection timeout
                socketChannel.configureBlocking(true);
                socketChannel.socket().connect(new InetSocketAddress(HOST, PORT), CONNECTION_TIMEOUT);
                socketChannel.configureBlocking(false);

                // Send request length
                byte[] requestData = request.toByteArray();

                // Validate request size
                if (requestData.length > MAX_MESSAGE_SIZE) {
                    throw new IllegalArgumentException("Request too large");
                }

                ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(requestData.length);
                lengthBuffer.flip();
                socketChannel.write(lengthBuffer);

                // Send request data
                ByteBuffer dataBuffer = ByteBuffer.wrap(requestData);
                socketChannel.write(dataBuffer);

                // Read response length
                ByteBuffer headerBuffer = ByteBuffer.allocate(4);
                int bytesRead = 0;
                while (headerBuffer.hasRemaining()) {
                    int read = socketChannel.read(headerBuffer);
                    if (read == -1) {
                        throw new IOException("Connection closed unexpectedly");
                    }
                    bytesRead += read;
                }
                headerBuffer.flip();
                int responseLength = headerBuffer.getInt();

                // Validate response size
                if (responseLength <= 0 || responseLength > MAX_MESSAGE_SIZE) {
                    throw new IllegalArgumentException("Invalid response size: " + responseLength);
                }

                // Read response data
                ByteBuffer responseBuffer = ByteBuffer.allocate(responseLength);
                bytesRead = 0;
                while (responseBuffer.hasRemaining()) {
                    int read = socketChannel.read(responseBuffer);
                    if (read == -1) {
                        throw new IOException("Connection closed before full response received");
                    }
                    bytesRead += read;
                }
                responseBuffer.flip();

                byte[] responseData = new byte[responseLength];
                responseBuffer.get(responseData);

                return Response.parseFrom(responseData);

            } catch (ConnectException e) {
                logger.warn("Connection failed (attempt {} of {}): {}", retries + 1, MAX_RETRIES, e.getMessage());
                retries++;

                if (retries >= MAX_RETRIES) {
                    throw e;
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry", e);
                }
            }
        }

        throw new IOException("Failed to send request after " + MAX_RETRIES + " attempts");
    }}