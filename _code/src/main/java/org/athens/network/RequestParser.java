package org.athens.network;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RequestParser {
    public static List<String> parseRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String firstLine = reader.readLine();
        if (firstLine == null || firstLine.charAt(0) != '*') {
            throw new IOException("Invalid CBSP array format");
        }
        int numElements = Integer.parseInt(firstLine.substring(1));
        List<String> elements = new ArrayList<>();
        for (int i = 0; i < numElements; i++) {
            String lengthLine = reader.readLine();
            if (lengthLine == null || lengthLine.charAt(0) != '$') {
                throw new IOException("Invalid CBSP bulk string format");
            }

            int length = Integer.parseInt(lengthLine.substring(1));
            char[] buffer = new char[length];
            if (reader.read(buffer, 0, length) != length) {
                throw new IOException("Incomplete CBSP bulk string");
            }

            elements.add(new String(buffer));
            reader.readLine(); // Consume trailing \r\n
        }

        return elements;
    }

    //CBSP Encoding
    public static String encodeSimpleString(String message) {
        return STR."+\{message}\r\n";
    }

    public static String encodeError(String error) {
        return STR."-\{error}\r\n";
    }

    public static String encodeBulkString(String data) {
        return STR."$\{data.length()}\r\n\{data}\r\n";
    }

    public static String encodeInteger(long value) {
        return STR.":\{value}\r\n";
    }


}
