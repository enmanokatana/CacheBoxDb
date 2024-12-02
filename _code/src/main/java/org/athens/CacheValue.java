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
    private final int version;


    public CacheValue(int version, Type type, Object value) {
        this.version = version;
        this.type = type;
        this.value = value;
    }
    public int getVersion() {
        return version;
    }
    public static CacheValue of(int version, String value) {
        if (version < 0) {
            throw new IllegalArgumentException("Version cannot be negative.");
        }
        return new CacheValue(version, Type.STRING, value);
    }

    public static CacheValue of(int version, Integer value) {
        return new CacheValue(version, Type.INTEGER, value);
    }

    public static CacheValue of(int version, Boolean value) {
        return new CacheValue(version, Type.BOOLEAN, value);
    }

    public static CacheValue of(int version, List<?> value) {
        return new CacheValue(version, Type.LIST, new ArrayList<>(value));
    }

    public static CacheValue ofNull(int version) {
        return new CacheValue(version, Type.NULL, null);
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
        if (isNull()) return "NULL:" + version + ":null";
        switch (type) {
            case STRING: return "STRING:" + version + ":" + value;
            case INTEGER: return "INTEGER:" + version + ":" + value;
            case BOOLEAN: return "BOOLEAN:" + version + ":" + value;
            case LIST: return "LIST:" + version + ":" + String.join(",", ((List<?>) value).stream()
                    .map(Object::toString)
                    .toArray(String[]::new));
            default: throw new IllegalStateException("Unknown type: " + type);
        }
    }
    public static CacheValue deserialize(String data) {
        String[] parts = data.split(":", 3);
        if (parts.length != 3) throw new IllegalArgumentException("Invalid format");

        Type type = Type.valueOf(parts[0]);
        int version = Integer.parseInt(parts[1]);
        String value = parts[2];

        switch (type) {
            case NULL: return ofNull(version);
            case STRING: return of(version, value);
            case INTEGER: return of(version, Integer.parseInt(value));
            case BOOLEAN: return of(version, Boolean.parseBoolean(value));
            case LIST: return of(version, Arrays.asList(value.split(",")));
            default: throw new IllegalStateException("Unknown type: " + type);
        }
    }
    public Object getValue() {
        return value;
    }

}
