package org.athens;

public abstract class CacheOp {
    protected final String key;

    public CacheOp(String key) {
        this.key = key;
    }

    public abstract void execute(CacheBox cacheBox);

    public abstract void undo(CacheBox cacheBox);

    public abstract String serialize();
}
