package org.athens;

public class DeleteOperation extends CacheOp {
    private CacheValue previousValue;

    public DeleteOperation(String key) {
        super(key);
    }

    @Override
    public void execute(CacheBox cacheBox) {
        previousValue = cacheBox.get(key);
        cacheBox.delete(key);
    }

    @Override
    public void undo(CacheBox cacheBox) {
        if (previousValue != null) {
            cacheBox.put(key, previousValue);
        }
    }

    @Override
    public String serialize() {
        return "DELETE:" + key;
    }
}
