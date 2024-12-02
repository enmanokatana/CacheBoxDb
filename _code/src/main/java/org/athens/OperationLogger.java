package org.athens;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationLogger {
    private static final Logger logger = LoggerFactory.getLogger(OperationLogger.class);

    public static void logOperation(String operationType, String key, CacheValue value) {
        logger.info("Operation: {} | Key: {} | Value: {}", operationType, key, value != null ? value : "null");
    }
}
