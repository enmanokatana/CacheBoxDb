package org.athens.db.shrading;

import java.util.List;

public class LoadBalancer {
    private final WeightedRoundRobinLoadBalancer weightedRoundRobinLoadBalancer;

    public LoadBalancer(List<ShardedCacheBox> cacheBoxes, List<Integer> weights) {
        this.weightedRoundRobinLoadBalancer = new WeightedRoundRobinLoadBalancer(cacheBoxes, weights);
    }

    public ShardedCacheBox getNextCacheBox() {
        return weightedRoundRobinLoadBalancer.getNextCacheBox();
    }
}