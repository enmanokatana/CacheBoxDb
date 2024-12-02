package org.athens;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class TxIdManager {
    private static TxIdManager instance;
    private static final Object lock = new Object();
    private int lastTxId;
    private final File txIdFile = new File("last_txid.txt");

    private TxIdManager() {
        try {
            lastTxId = Integer.parseInt(new String(Files.readAllBytes(txIdFile.toPath())));
        } catch (IOException | NumberFormatException e) {
            lastTxId = 0;
        }
    }

    public static TxIdManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new TxIdManager();
                }
            }
        }
        return instance;
    }

    public synchronized int getNextTxId() {
        lastTxId++;
        return lastTxId;
    }

    public synchronized void checkpoint() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(txIdFile))) {
            writer.write(String.valueOf(lastTxId));
            writer.flush();
        } catch (IOException e) {
            // Handle exception
        }
    }
}