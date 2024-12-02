package org.athens;

public class PutOperation extends CacheOp {
    private final String key;
    private final CacheValue value;

    public PutOperation(String key, CacheValue value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void execute(CacheBox cacheBox) {
        cacheBox.put(key, value);
    }

    @Override
    public void undo(CacheBox cacheBox) {
        cacheBox.delete(key);
    }

    @Override
    public String serialize() {
        return "PUT:" + key + ":" + value.serialize();
    }
}