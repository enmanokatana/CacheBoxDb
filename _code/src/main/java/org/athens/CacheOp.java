package org.athens;

public abstract class CacheOp {
    public abstract void execute(CacheBox cacheBox);
    public abstract void undo(CacheBox cacheBox);
    public abstract String serialize();
}