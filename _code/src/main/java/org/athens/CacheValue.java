package org.athens;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CacheValue implements Serializable {
    public enum Type {
        STRING,
        INTEGER,
        BOOLEAN,
        LIST,
        NULL
    }
    private final Type type;
    private final Object value;

    private CacheValue(Type type,Object value){
        this.type = type;
        this.value = value;
    }
    public static CacheValue of(String value) {
        return new CacheValue(Type.STRING, value);
    }

    public static CacheValue of(Integer value) {
        return new CacheValue(Type.INTEGER, value);
    }

    public static CacheValue of(Boolean value) {
        return new CacheValue(Type.BOOLEAN, value);
    }

    public static CacheValue of(List<?> value) {
        return new CacheValue(Type.LIST, new ArrayList<>(value));
    }

    public static CacheValue ofNull() {
        return new CacheValue(Type.NULL, null);
    }

    public String asString() {
        return value != null ? value.toString() : null;
    }
    public Integer asInteger() {
        return (Integer) value;
    }

    public Boolean asBoolean() {
        return (Boolean) value;
    }

    @SuppressWarnings("unchecked")
    public List<Object> asList() {
        return (List<Object>) value;
    }
    public Type getType() {
        return type;
    }

    public boolean isNull() {
        return type == Type.NULL;
    }

    public String serialize() {
        if (isNull()) return "NULL:null";
        switch (type) {
            case STRING: return "STRING:" + value;
            case INTEGER: return "INTEGER:" + value;
            case BOOLEAN: return "BOOLEAN:" + value;
            case LIST: return "LIST:" + String.join(",", ((List<?>) value).stream()
                    .map(Object::toString)
                    .toArray(String[]::new));
            default: throw new IllegalStateException("Unknown type: " + type);
        }
    }
    public static CacheValue deserialize(String data) {
        String[] parts = data.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid format");

        Type type = Type.valueOf(parts[0]);
        String value = parts[1];

        switch (type) {
            case NULL: return ofNull();
            case STRING: return of(value);
            case INTEGER: return of(Integer.parseInt(value));
            case BOOLEAN: return of(Boolean.parseBoolean(value));
            case LIST: return of(Arrays.asList(value.split(",")));
            default: throw new IllegalStateException("Unknown type: " + type);
        }
    }

    public Object getValue() {
        return value;
    }

}
