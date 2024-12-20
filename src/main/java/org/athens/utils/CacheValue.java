package org.athens.utils;

import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        String encodedValue = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8);
        switch (type) {
            case STRING:
                return "STRING:" + version + ":" + encodedValue;
            case INTEGER:
                return "INTEGER:" + version + ":" + encodedValue;
            case BOOLEAN:
                return "BOOLEAN:" + version + ":" + encodedValue;
            case LIST:
                return "LIST:" + version + ":" + encodedValue;
            default:
                throw new IllegalStateException("Unknown type: " + type);
        }
    }
    public static CacheValue deserialize(String data) {

//        String[] parts = data.split(":", 3);
//        if (parts.length != 3) throw new IllegalArgumentException("Invalid format");
//
//        Type type = Type.valueOf(parts[0]);
//        int version = Integer.parseInt(parts[1]);
//        String value = parts[2];


        String[] parts = data.split(":", 3);
        if (parts.length != 3) throw new IllegalArgumentException("Invalid format");
        Type type = Type.valueOf(parts[0]);
        int version = Integer.parseInt(parts[1]);
        String encodedValue = parts[2];
        String value = URLDecoder.decode(encodedValue, StandardCharsets.UTF_8);
        return switch (type) {
            case NULL -> ofNull(version);
            case STRING -> of(version, value);
            case INTEGER -> of(version, Integer.parseInt(value));
            case BOOLEAN -> of(version, Boolean.parseBoolean(value));
            case LIST -> of(version, Arrays.asList(value.split(",")));
            default -> throw new IllegalStateException("Unknown type: " + type);
        };
    }
    public Object getValue() {
        return value;
    }

}
