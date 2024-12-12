package org.athens.network;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RequestParser {

    /**
     * Parses a CBSP request from an InputStream.
     * @param inputStream The input stream connected to the client.
     * @return A list of strings representing the parsed CBSP command.
     * @throws IOException If the input is invalid or cannot be parsed.
     */
    public static List<String> parseRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<String> elements = new ArrayList<>();

        try {
            // Read the array header (*<count>\r\n)
            String firstLine = reader.readLine();
            if (firstLine == null || firstLine.isEmpty()) {
                throw new IllegalArgumentException("Input is empty or null");
            }

            if (firstLine.charAt(0) != '*') {
                throw new IllegalArgumentException("Invalid CBSP format: Expected array header '*', got: " + firstLine);
            }

            int numElements;
            try {
                numElements = Integer.parseInt(firstLine.substring(1).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number of elements: " + firstLine.substring(1));
            }

            // Read each bulk string
            for (int i = 0; i < numElements; i++) {
                String lengthLine = reader.readLine();
                if (lengthLine == null || lengthLine.isEmpty() || lengthLine.charAt(0) != '$') {
                    throw new IllegalArgumentException("Invalid CBSP format: Expected bulk string header '$', got: " + lengthLine);
                }

                int length;
                try {
                    length = Integer.parseInt(lengthLine.substring(1).trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid bulk string length: " + lengthLine.substring(1));
                }

                char[] buffer = new char[length];
                if (reader.read(buffer, 0, length) != length) {
                    throw new IllegalArgumentException("Incomplete bulk string data for length: " + length);
                }

                elements.add(new String(buffer)); // Add the bulk string to the list
                reader.readLine(); // Consume the trailing \r\n
            }
        } catch (Exception e) {
            // Log and rethrow as an IOException
            throw new IOException("Error parsing CBSP request: " + e.getMessage(), e);
        }

        return elements;
    }

    // CBSP Encoding Helpers
    public static String encodeSimpleString(String message) {
        return "+" + message + "\r\n";
    }

    public static String encodeError(String error) {
        return "-" + error + "\r\n";
    }

    public static String encodeBulkString(String data) {
        return "$" + data.length() + "\r\n" + data + "\r\n";
    }

    public static String encodeInteger(long value) {
        return ":" + value + "\r\n";
    }
}
