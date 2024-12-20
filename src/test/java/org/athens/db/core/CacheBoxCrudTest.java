//package org.athens.db.core;
//
//import org.athens.utils.CacheValue;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class CacheBoxCrudTest {
//
//    private CacheBox cacheBox;
//
//    @BeforeEach
//    public void setUp() {
//        // Initialize CacheBox with a test file and encryption disabled
//        cacheBox = new CacheBox("test_crud.cbx", false, null, null, 1000);
//    }
//
//    @Test
//    public void testPutAndGet() {
//        // Test put and get operations
//
//        cacheBox.put("key1", CacheValue.of(0,"value1"));
//        assertEquals("value1", cacheBox.get("key1").getValue());
//    }
//
//    @Test
//    public void testDelete() {
//        // Test delete operation
//        cacheBox.put("key1", CacheValue.of(0,"value1"));
//        cacheBox.delete("key1");
//        assertNull(cacheBox.get("key1"));
//    }
//
//    @Test
//    public void testUpdate() {
//        // Test updating a value
//        cacheBox.put("key1", CacheValue.of(0,"value1"));
//        cacheBox.put("key1", CacheValue.of(0,"value2"));
//        assertEquals("value2", cacheBox.get("key1").getValue());
//    }
//}