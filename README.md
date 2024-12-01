# CacheBox

A lightweight, zero-dependency key-value database implemented in Java with file-based persistence and command-line interface.

## Features

- 📦 Zero external dependencies
- 💾 Automatic file-based persistence
- ⚡ Fast in-memory operations with disk backup
- 🔄 CRUD operations (Create, Read, Update, Delete)
- 📝 Human-readable storage format
- 💻 Interactive command-line interface
- 🧵 Thread-safe operations

## Quick Start

```java
// Initialize the database
CacheBox db = new CacheBox("mydata.cbx");

// Store a value
db.put("user", "John Doe");

// Retrieve a value
String value = db.get("user"); // Returns "John Doe"

// Delete a value
db.delete("user");
```

## Command Line Interface

Start the CLI and interact with your database using simple commands:

```bash
$ java CacheBox
CacheBox v1.0
Type 'help' for available commands

db> help

Available commands:
put <key> <value> - Store a key-value pair
get <key> - Retrieve a value by key
delete <key> - Delete a key-value pair
list - Show all stored key-value pairs
help - Show this help message
exit - Exit the program
```

## Storage Format

CacheBox uses a simple, human-readable storage format:
```
key=value
user1=John Doe
user2=Jane Smith
```

Files are stored with the `.cbx` extension.

## Use Cases

- Application configuration storage
- Development and testing environments
- Small-scale data storage needs
- Prototyping and proof of concepts
- Educational purposes
- Simple caching solutions

## Limitations

- Single file storage
- String-only data type support
- Basic querying capabilities
- No built-in encryption
- Local file system only

## Contributing

Contributions are welcome! Here are some areas where you could help:

- Adding support for different data types
- Implementing data compression
- Adding encryption support
- Creating a network interface
- Adding transaction support
- Implementing backup/restore functionality

## License

MIT License - feel free to use this in your own projects!

## Implementation Example

```java
CacheBox db = new CacheBox("users.cbx");

// Basic operations
db.put("name", "John Doe");
db.put("email", "john@example.com");

// Bulk operations
Map<String, String> userData = new HashMap<>();
userData.put("age", "30");
userData.put("city", "New York");
db.putAll(userData);

// Check existence
if (db.contains("name")) {
    System.out.println(db.get("name"));
}
```

## Performance

- Read operations: O(1)
- Write operations: O(1)
- File synchronization: After each write operation
- Memory footprint: Proportional to stored data size

## Best Practices

1. Close the database properly when finished
2. Use appropriate key naming conventions
3. Handle exceptions for file operations
4. Regularly backup your data file
5. Monitor file size growth

## Version History

- 1.0 (Initial Release)
  - Basic CRUD operations
  - Command-line interface
  - File-based persistence
  - Thread-safe operations

## Support

For issues, questions, or contributions, please open an issue in the repository.

---
Made with ☕ by developers, for developers
