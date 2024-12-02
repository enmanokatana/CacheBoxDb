package org.athens;

public class DeleteOperation extends CacheOp {
    private final String key;
    private final CacheValue value;

    public DeleteOperation(String key, CacheValue value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void execute(CacheBox cacheBox) {
        cacheBox.delete(key);
    }

    @Override
    public void undo(CacheBox cacheBox) {
        cacheBox.put(key, value);
    }

    @Override
    public String serialize() {
        return "DELETE:" + key + ":" + (value != null ? value.serialize() : "NULL:null");
    }
}