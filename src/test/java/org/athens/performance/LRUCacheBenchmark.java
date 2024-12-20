//package org.athens.performance;
//
//
//import org.athens.db.core.LRUCache;
//import org.openjdk.jmh.annotations.*;
//import org.openjdk.jmh.runner.Runner;
//import org.openjdk.jmh.runner.RunnerException;
//import org.openjdk.jmh.runner.options.Options;
//import org.openjdk.jmh.runner.options.OptionsBuilder;
//
//import java.util.Random;
//import java.util.concurrent.TimeUnit;
//
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@State(Scope.Benchmark)
//@Fork(value = 1, warmups = 1)
//@Warmup(iterations = 3, time = 1)
//@Measurement(iterations = 5, time = 1)
//public class LRUCacheBenchmark {
//
//    @Param({"100", "1000", "10000"})
//    private int cacheSize;
//
//    @Param({"0.2", "0.5", "0.8"})
//    private double hitRatio;
//
//    private LRUCache<Integer, String> cache;
//    private Integer[] keys;
//    private Random random;
//    private int dataSize;
//
//    @Setup
//    public void setup() {
//        cache = new LRUCache<>(cacheSize);
//        random = new Random(42); // Fixed seed for reproducibility
//
//        // Create data with size larger than cache to force evictions
//        dataSize = (int)(cacheSize * 1.5);
//        keys = new Integer[dataSize];
//
//        // Initialize data
//        for (int i = 0; i < dataSize; i++) {
//            keys[i] = i;
//            cache.put(i, "Value-" + i);
//        }
//    }
//
//    @Benchmark
//    public String measureGetHit() {
//        // Select keys that are likely to be in cache based on hit ratio
//        int index = random.nextInt((int)(cacheSize * hitRatio));
//        return cache.get(keys[index]);
//    }
//
//    @Benchmark
//    public String measureGetMiss() {
//        // Select keys that are likely to be evicted
//        int index = random.nextInt(dataSize - cacheSize) + cacheSize;
//        return cache.get(keys[index]);
//    }
//
//    @Benchmark
//    public String measurePut() {
//        int key = random.nextInt(dataSize);
//        return cache.put(key, "NewValue-" + key);
//    }
//
//    @Benchmark
//    public void measureEviction() {
//        // Force eviction by putting new entries
//        int newKey = dataSize + random.nextInt(1000);
//        cache.put(newKey, "EvictionValue-" + newKey);
//    }
//
//    // Method to run the benchmark from IDE
//    public static void main(String[] args) throws RunnerException {
//        Options opt = new OptionsBuilder()
//                .include(LRUCacheBenchmark.class.getSimpleName())
//                .forks(1)
//                .build();
//
//        new Runner(opt).run();
//    }
//}