//package org.athens.performance;
//
//import org.athens.db.core.CacheBox;
//import org.athens.utils.CacheQuery;
//import org.athens.utils.CacheValue;
//import org.openjdk.jmh.annotations.*;
//import org.openjdk.jmh.runner.Runner;
//import org.openjdk.jmh.runner.RunnerException;
//import org.openjdk.jmh.runner.options.Options;
//import org.openjdk.jmh.runner.options.OptionsBuilder;
//
//import java.io.File;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@State(Scope.Benchmark)
//@Fork(value = 1, warmups = 1)
//@Warmup(iterations = 3, time = 1)
//@Measurement(iterations = 5, time = 1)
//public class CacheBoxValueBenchmark {
//
//    @Param({"1000", "10000"})
//    private int cacheSize;
//
//    private CacheBox cacheBox;
//    private Random random;
//    private String[] keys;
//    private CacheValue[] stringValues;
//    private CacheValue[] intValues;
//    private CacheValue[] booleanValues;
//    private CacheValue[] listValues;
//    private String dbFile;
//
//    @Setup
//    public void setup() {
//        random = new Random(42);
//        dbFile = "benchmark_db_" + System.nanoTime() + ".db";
//
//        // Initialize CacheBox
//        cacheBox = new CacheBox(dbFile, cacheSize);
//
//        // Prepare test data
//        keys = new String[cacheSize];
//        stringValues = new CacheValue[cacheSize];
//        intValues = new CacheValue[cacheSize];
//        booleanValues = new CacheValue[cacheSize];
//        listValues = new CacheValue[cacheSize];
//
//        // Initialize different types of values
//        for (int i = 0; i < cacheSize; i++) {
//            keys[i] = "key-" + i;
//            stringValues[i] = CacheValue.of(i, "value-" + i);
//            intValues[i] = CacheValue.of(i, i);
//            booleanValues[i] = CacheValue.of(i, i % 2 == 0);
//            listValues[i] = CacheValue.of(i, Arrays.asList(i, "item" + i, i % 2 == 0));
//        }
//
//        // Populate cache with initial data
//        cacheBox.beginTransaction();
//        for (int i = 0; i < cacheSize / 4; i++) {
//            cacheBox.put(keys[i], stringValues[i]);
//            cacheBox.put(keys[i + cacheSize/4], intValues[i]);
//            cacheBox.put(keys[i + cacheSize/2], booleanValues[i]);
//            cacheBox.put(keys[i + 3*cacheSize/4], listValues[i]);
//        }
//        cacheBox.commit();
//    }
//
//    @TearDown
//    public void tearDown() {
//        new File(dbFile).delete();
//    }
//
//    @Benchmark
//    public void measureStringValueOperations() {
//        int index = random.nextInt(cacheSize / 4);
//        cacheBox.beginTransaction();
//        String key = keys[index];
//        CacheValue value = stringValues[index];
//        cacheBox.put(key, value);
//        CacheValue retrieved = cacheBox.get(key);
//        assert retrieved.getType() == CacheValue.Type.STRING;
//        cacheBox.commit();
//    }
//
//    @Benchmark
//    public void measureIntegerValueOperations() {
//        int index = random.nextInt(cacheSize / 4);
//        cacheBox.beginTransaction();
//        String key = keys[index + cacheSize/4];
//        CacheValue value = intValues[index];
//        cacheBox.put(key, value);
//        CacheValue retrieved = cacheBox.get(key);
//        assert retrieved.getType() == CacheValue.Type.INTEGER;
//        cacheBox.commit();
//    }
//
//    @Benchmark
//    public void measureListValueOperations() {
//        int index = random.nextInt(cacheSize / 4);
//        cacheBox.beginTransaction();
//        String key = keys[index + 3*cacheSize/4];
//        CacheValue value = listValues[index];
//        cacheBox.put(key, value);
//        CacheValue retrieved = cacheBox.get(key);
//        assert retrieved.getType() == CacheValue.Type.LIST;
//        cacheBox.commit();
//    }
//
//    @Benchmark
//    public void measureVersionedUpdates() {
//        int index = random.nextInt(cacheSize / 4);
//        String key = keys[index];
//        cacheBox.beginTransaction();
//        // Update value with incrementing versions
//        CacheValue value1 = CacheValue.of(1, "version1");
//        CacheValue value2 = CacheValue.of(2, "version2");
//        cacheBox.put(key, value1);
//        cacheBox.put(key, value2);
//        CacheValue retrieved = cacheBox.get(key);
//        assert retrieved.getVersion() == 2;
//        cacheBox.commit();
//    }
//
//
//
//    @Benchmark
//    public String measureSerializationDeserialization() {
//        int index = random.nextInt(cacheSize);
//        CacheValue value = listValues[index]; // Using list values as they're most complex
//        String serialized = value.serialize();
//        CacheValue deserialized = CacheValue.deserialize(serialized);
//        assert deserialized.getType() == value.getType();
//        return serialized;
//    }
//
//    @Benchmark
//    public void measureNullValueHandling() {
//        String key = "null-key-" + random.nextInt(1000);
//        cacheBox.beginTransaction();
//        CacheValue nullValue = CacheValue.ofNull(1);
//        cacheBox.put(key, nullValue);
//        CacheValue retrieved = cacheBox.get(key);
//        assert retrieved.isNull();
//        cacheBox.commit();
//    }
//
//    // Method to run the benchmark from IDE
//    public static void main(String[] args) throws RunnerException {
//        Options opt = new OptionsBuilder()
//                .include(CacheBoxValueBenchmark.class.getSimpleName())
//                .forks(1)
//                .build();
//
//        new Runner(opt).run();
//    }
//}