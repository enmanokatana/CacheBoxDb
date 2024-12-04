package org.athens;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheBoxTest {

    @TempDir
     File tempDir;

    private CacheBox db;
    private String dbFile;

    @BeforeEach
    void setUp() throws IOException {
        dbFile = tempDir.toPath().resolve("cachebox_test.cbx").toString();
        db = new CacheBox(dbFile);
    }

    @AfterEach
    void tearDown() {
        db = null;
    }

    @Test
    void testSingleTransactionCommit() {
        db.beginTransaction();
        db.put("key1", CacheValue.of(0, "value1"));
        db.commit();

        Map<String, CacheValue> committedState = db.getCommittedState();
        assertEquals(1, committedState.size());
        CacheValue value = committedState.get("key1");
        assertNotNull(value);
        assertEquals(1, value.getVersion());
        assertEquals("value1", value.asString());
    }

    @Test
    void testVersioningOnExistingKeys() {
        db.beginTransaction();
        db.put("key2", CacheValue.of(0, "value2"));
        db.commit();

        db.beginTransaction();
        db.put("key2", CacheValue.of(0, "value2 updated"));
        db.commit();

        Map<String, CacheValue> committedState = db.getCommittedState();
        CacheValue value = committedState.get("key2");
        assertEquals(2, value.getVersion());
        assertEquals("value2 updated", value.asString());
    }

    @Test
    void testConcurrencyConflict() {
        db.beginTransaction();
        db.put("key3", CacheValue.of(0, "value3"));
        db.commit();

        // Transaction 1
        db.beginTransaction();
        db.put("key3", CacheValue.of(0, "value3 updated by T1"));

        // Transaction 2
        db.rollback(); // End Transaction 1
        db.beginTransaction();
        db.put("key3", CacheValue.of(0, "value3 updated by T2"));
        db.commit();

        assertThrows(ConcurrencyException.class, () -> {
            db.commit();
        });
    }

    @Test
    void testRollback() {
        db.beginTransaction();
        db.put("key4", CacheValue.of(0, "value4"));
        db.rollback();

        Map<String, CacheValue> committedState = db.getCommittedState();
        assertNull(committedState.get("key4"));
    }

    @Test
    void testDeletionAndReinsertion() {
        db.beginTransaction();
        db.put("key5", CacheValue.of(0, "value5"));
        db.commit();

        db.beginTransaction();
        db.delete("key5");
        db.put("key5", CacheValue.of(0, "value5 reinserted"));
        db.commit();

        Map<String, CacheValue> committedState = db.getCommittedState();
        CacheValue value = committedState.get("key5");
        assertEquals(1, value.getVersion());
        assertEquals("value5 reinserted", value.asString());
    }
}