package org.athens.network;

import org.athens.db.encryption.AESEncryptionStrategy;
import org.athens.db.encryption.EncryptionStrategy;
import org.athens.db.shrading.LoadBalancer;
import org.athens.db.shrading.ShardedCacheBox;
import org.athens.monitoring.MonitoringService;
import org.athens.utils.CacheValue;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    private static ShardedCacheBox cacheBox;
    private static LoadBalancer loadBalancer;
    MonitoringService monitoringService = new MonitoringService(cacheBox.getShards());
    private static final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public static void main(String[] args){
        int port = 20029;
        initializeCacheBox();
        System.out.println(STR."starting cache box server on port \{port}");

        try(ServerSocket serverSocket = new ServerSocket(port)){
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }catch(IOException e){
            System.err.println("Server error : "+e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable{
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocet) {
            this.clientSocket = clientSocet;
        }

        /**
         * Runs this operation.
         */
        @Override
        public void run() {
            try(
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true)
                    ){
                String inputLine;
                while ((inputLine = in.readLine())!=null){
                    String response = processCommand(inputLine.trim());
                    out.println(response);
                }
            }catch (IOException e){
                System.err.println("Client Handler Exception" + e.getMessage());
                e.printStackTrace();
            }finally {
                try{
                    clientSocket.close();
                }catch (IOException e){
                    System.err.println("Error closing cclient socket"+e.getMessage());
                }
            }
        }
        private String processCommand(String command) {
            String[] parts = command.split(" ", 4); // Split into max 3 parts
            String action = parts[0].toUpperCase();

            switch (action) {
                case "PUT":
                    if (parts.length < 4) {
                        return "ERROR: PUT requires a key and a value.";
                    }
                    cacheBox.beginTransaction();

                    if (!cacheBox.isTransactionActive()) {
                        System.out.println("Error: Start a transaction using 'begin' first.");
                        return "no Transaction Active Error";
                    }

                    String inputType = parts[1].toLowerCase();
                    String inputKey = parts[2];
                    String inputValue = parts[3];

                    switch (inputType) {
                        case "string":
                            cacheBox.put(inputKey, CacheValue.of(0, inputValue));
                            cacheBox.commit();

                            return "OK";
                        case "int":
                            cacheBox.put(inputKey, CacheValue.of(0, Integer.parseInt(inputValue)));
                            cacheBox.commit();


                            return "OK";
                        case "bool":
                            cacheBox.put(inputKey, CacheValue.of(0, Boolean.parseBoolean(inputValue)));
                            cacheBox.commit();

                            return "OK";
                        case "list":
                            cacheBox.put(inputKey, CacheValue.of(0, Arrays.asList(inputValue.split(","))));
                            cacheBox.commit();
                            return "OK";
                        default:
                            return "Unknown type. Use: string, int, bool, or list";

                    }
                case "GET":
                    if (parts.length < 2) return "ERROR: GET requires a key.";
                    cacheBox.beginTransaction();

                    if (!cacheBox.isTransactionActive()) {
                        return "Error: Start a transaction using 'begin' first.";
                    }
                    CacheValue getValue = cacheBox.get(parts[1]);
                    cacheBox.commit();
                    if (getValue != null) {
                        return getValue.asString();
                    } else {
                        return "Key not found.";
                    }
                case "DELETE":
                    if (parts.length < 2) return "ERROR: DELETE requires a key.";
                    return store.remove(parts[1]) != null ? "OK" : "NULL";
                default:
                    return "ERROR: Unknown command.";
            }
        }
    }

    private static void initializeCacheBox() {
        byte[] encryptionKey = new SecureRandom().generateSeed(16);
        EncryptionStrategy encryptionStrategy = new AESEncryptionStrategy();
        boolean encryptionEnabled = true;
        int maxSize = 1000;

        List<ShardedCacheBox> cacheBoxes = Arrays.asList(
                new ShardedCacheBox(4, "db_files/shard1_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize),
                new ShardedCacheBox(4, "db_files/shard2_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize),
                new ShardedCacheBox(4, "db_files/shard3_", encryptionStrategy, encryptionEnabled, encryptionKey, maxSize)
        );

        loadBalancer = new LoadBalancer(cacheBoxes, Arrays.asList(1, 1, 1));
        cacheBox = loadBalancer.getNextCacheBox();
    }


}





