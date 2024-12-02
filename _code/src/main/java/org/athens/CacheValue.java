package org.athens;

import java.util.Arrays;
import java.util.List;

public class CacheValue {
    public enum Type {
        STRING, INTEGER, BOOLEAN, LIST, NULL
    }

    private final Type type;
    private final Object value;

    private CacheValue(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static CacheValue ofString(String value) {
        return new CacheValue(Type.STRING, value);
    }

    public static CacheValue ofInteger(Integer value) {
        return new CacheValue(Type.INTEGER, value);
    }

    public static CacheValue ofBoolean(Boolean value) {
        return new CacheValue(Type.BOOLEAN, value);
    }

    public static CacheValue ofList(List<String> value) {
        return new CacheValue(Type.LIST, value);
    }

    public static CacheValue ofNull() {
        return new CacheValue(Type.NULL, null);
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String serialize() {
        switch (type) {
            case STRING:
                return "STRING:" + value;
            case INTEGER:
                return "INTEGER:" + value;
            case BOOLEAN:
                return "BOOLEAN:" + value;
            case LIST:
                return "LIST:" + String.join(",", (List<String>) value);
            case NULL:
                return "NULL:null";
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public static CacheValue deserialize(String data) {
        String[] parts = data.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid format");
        }

        Type type = Type.valueOf(parts[0]);
        String value = parts[1];

        switch (type) {
            case STRING:
                return ofString(value);
            case INTEGER:
                return ofInteger(Integer.parseInt(value));
            case BOOLEAN:
                return ofBoolean(Boolean.parseBoolean(value));
            case LIST:
                return ofList(Arrays.asList(value.split(",")));
            case NULL:
                return ofNull();
            default:
                throw new IllegalStateException("Unknown type: " + type);
        }
    }
}