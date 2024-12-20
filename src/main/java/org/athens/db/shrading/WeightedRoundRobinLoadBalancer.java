package org.athens.db.shrading;
import org.athens.db.shrading.ShardedCacheBox;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WeightedRoundRobinLoadBalancer {
    private final List<ShardedCacheBox> cacheBoxes;
    private final List<Integer> weights;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public WeightedRoundRobinLoadBalancer(List<ShardedCacheBox> cacheBoxes, List<Integer> weights) {
        this.cacheBoxes = cacheBoxes;
        this.weights = weights;
    }

    public ShardedCacheBox getNextCacheBox() {
        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        int currentWeight = currentIndex.getAndIncrement() % totalWeight;
        int cumulativeWeight = 0;
        for (int i = 0; i < cacheBoxes.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (currentWeight < cumulativeWeight) {
                return cacheBoxes.get(i);
            }
        }
        return cacheBoxes.get(0); // Fallback
    }
}