### **RESP Request Structure**
RESP commands typically consist of:
1. **Arrays**: The command is represented as an array, starting with `*` followed by the number of elements.
2. **Bulk Strings**: Each element is a bulk string, prefixed with `$` and the string length.

#### Example: Command to Set a Key
Command: `SET name Alice`  
RESP Format:
```
*3\r\n           # Array of 3 elements
$3\r\nSET\r\n    # Bulk string "SET" (length 3)
$4\r\nname\r\n   # Bulk string "name" (length 4)
$5\r\nAlice\r\n  # Bulk string "Alice" (length 5)
```

---

### **Step-by-Step Parsing Logic**
To parse RESP, we need to:
1. **Read the array header**:
    - Look for `*` and read the number of elements in the array.
2. **Read bulk strings**:
    - For each element, look for `$` and read the length of the bulk string.
    - Read the string data, followed by the `\r\n`.

---

### **RESP Parser Implementation**
Here’s how the parsing logic is implemented in Java:

```java
// RESP Parsing Utility
package org.athens.network;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RESPParser {

    /**
     * Parses a RESP request from an InputStream.
     * 
     * @param inputStream The input stream connected to the client.
     * @return A list of strings representing the parsed RESP command.
     * @throws IOException If there are issues reading the input.
     */
    public static List<String> parseRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        // 1. Read the array header (*<count>\r\n)
        String firstLine = reader.readLine(); // Reads the first line
        if (firstLine == null || firstLine.charAt(0) != '*') {
            throw new IOException("Invalid RESP: Expected array header starting with '*'");
        }

        int numElements = Integer.parseInt(firstLine.substring(1)); // Extract the number of elements
        List<String> elements = new ArrayList<>();

        // 2. Read each bulk string
        for (int i = 0; i < numElements; i++) {
            // Read the bulk string header ($<length>\r\n)
            String lengthLine = reader.readLine();
            if (lengthLine == null || lengthLine.charAt(0) != '$') {
                throw new IOException("Invalid RESP: Expected bulk string header starting with '$'");
            }

            int length = Integer.parseInt(lengthLine.substring(1)); // Extract length
            char[] buffer = new char[length];

            // Read the bulk string data
            if (reader.read(buffer, 0, length) != length) {
                throw new IOException("Invalid RESP: Incomplete bulk string data");
            }

            elements.add(new String(buffer)); // Add parsed string to the result
            reader.readLine(); // Consume the trailing \r\n
        }

        return elements; // Return the parsed command as a list of strings
    }
}
```

---

### **Detailed Explanation of the Logic**

#### 1. **Reading the Array Header**
- The RESP command starts with an array (`*`), indicating the number of elements in the command.
- Example:
    - Input: `*3\r\n`
    - Code:
      ```java
      String firstLine = reader.readLine(); // Read "*3\r\n"
      int numElements = Integer.parseInt(firstLine.substring(1)); // Extract "3"
      ```
    - Result: We now know the command has **3 elements**.

#### 2. **Reading Each Bulk String**
- Each element in the array is a bulk string starting with `$` and specifying the length of the string.
- Example:
    - Input: `$3\r\nSET\r\n`
    - Code:
      ```java
      String lengthLine = reader.readLine(); // Read "$3\r\n"
      int length = Integer.parseInt(lengthLine.substring(1)); // Extract "3"
      char[] buffer = new char[length];
      reader.read(buffer, 0, length); // Read "SET"
      ```
    - Result: Extracted the bulk string **"SET"**.

#### 3. **Handling Trailing `\r\n`**
- After each bulk string, RESP has a `\r\n` that needs to be consumed.
- Code:
  ```java
  reader.readLine(); // Consume "\r\n"
  ```

#### 4. **Repeat for All Elements**
- Repeat the above steps for the number of elements specified in the array header.

---

### **Parsing Example**

#### Input (Raw RESP):
```
*3\r\n
$3\r\n
SET\r\n
$4\r\n
name\r\n
$5\r\n
Alice\r\n
```

#### Parsing Steps:
1. **Array Header**:
    - Read `*3\r\n`.
    - Determine the command has **3 elements**.

2. **First Bulk String**:
    - Read `$3\r\n` → length is `3`.
    - Read `SET\r\n` → parsed element: `"SET"`.

3. **Second Bulk String**:
    - Read `$4\r\n` → length is `4`.
    - Read `name\r\n` → parsed element: `"name"`.

4. **Third Bulk String**:
    - Read `$5\r\n` → length is `5`.
    - Read `Alice\r\n` → parsed element: `"Alice"`.

#### Final Parsed Output:
```java
List<String> command = List.of("SET", "name", "Alice");
```

---

### **Error Handling**
The parser includes checks to validate RESP formatting:
- If the array header doesn't start with `*`, throw an error.
- If a bulk string header doesn't start with `$`, throw an error.
- If the length of data doesn’t match the specified length, throw an error.

---

### **Use This Parser in Your Server**
Integrate `RESPParser` into your server's `ClientHandler` to parse client requests:

```java
@Override
public void run() {
    try (
        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream()
    ) {
        while (true) {
            List<String> commandParts = RESPParser.parseRequest(inputStream);
            String response = processCommand(commandParts);
            outputStream.write(response.getBytes());
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

---

### **Suggestions**
**a.** Add unit tests to validate the parsing logic for various valid and invalid RESP inputs.  
**b.** Extend the parser to handle RESP arrays as responses.  