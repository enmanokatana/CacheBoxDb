package org.athens;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TxIdManager {
    private static TxIdManager instance;
    private static final AtomicInteger lastTxId = new AtomicInteger(0);
    private final File txIdFile = new File("last_txid.txt");
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private TxIdManager() {
        loadLastTxId();
        scheduleCheckpoint();
    }

    public static TxIdManager getInstance() {
        if (instance == null) {
            instance = new TxIdManager();
        }
        return instance;
    }

    public synchronized int getNextTxId() {
        return lastTxId.incrementAndGet();
    }

    private void loadLastTxId() {
        try {
            if (txIdFile.exists()) {
                String lastTxIdStr = new String(Files.readAllBytes(txIdFile.toPath()));
                lastTxId.set(Integer.parseInt(lastTxIdStr));
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void scheduleCheckpoint() {
        scheduler.scheduleAtFixedRate(this::writeLastTxIdToFile, 0, 1, TimeUnit.MINUTES);
    }

    private void writeLastTxIdToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(txIdFile))) {
            writer.write(String.valueOf(lastTxId.get()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkpoint() {
        writeLastTxIdToFile();
    }
}