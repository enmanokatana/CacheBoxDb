package org.athens;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
public class LoadBalancer {
    private final WeightedRoundRobinLoadBalancer weightedRoundRobinLoadBalancer;

    public LoadBalancer(List<ShardedCacheBox> cacheBoxes, List<Integer> weights) {
        this.weightedRoundRobinLoadBalancer = new WeightedRoundRobinLoadBalancer(cacheBoxes, weights);
    }

    public ShardedCacheBox getNextCacheBox() {
        return weightedRoundRobinLoadBalancer.getNextCacheBox();
    }
}