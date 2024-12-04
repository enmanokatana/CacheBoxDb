package org.athens;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {
    private final List<ShardedCacheBox> cacheBoxes;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public LoadBalancer(List<ShardedCacheBox> cacheBoxes) {
        this.cacheBoxes = cacheBoxes;
    }

    public ShardedCacheBox getNextCacheBox() {
        int index = currentIndex.getAndIncrement() % cacheBoxes.size();
        return cacheBoxes.get(index);
    }

}