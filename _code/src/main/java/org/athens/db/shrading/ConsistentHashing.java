package org.athens.db.shrading;

import org.athens.db.core.CacheBox;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashing {
    private final SortedMap<Integer, CacheBox> circle = new TreeMap<>();
    private final int numberOfReplicas;

    public ConsistentHashing(int numberOfReplicas, List<CacheBox> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        for (CacheBox node : nodes) {
            add(node);
        }
    }

    public void add(CacheBox node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeKey = node.toString() + i;
            int hash = getHash(nodeKey);
            circle.put(hash, node);
        }
    }

    public void remove(CacheBox node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeKey = node.toString() + i;
            int hash = getHash(nodeKey);
            circle.remove(hash);
        }
    }

    public CacheBox get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = getHash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, CacheBox> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    private int getHash(String key) {
        return Math.abs(key.hashCode());
    }
}