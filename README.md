# CacheBox

A lightweight, multi-type key-value database implemented in Java with file-based persistence and an interactive CLI.

## Features

- 📦 Zero external dependencies
- 💾 Automatic file-based persistence
- 🔢 Multiple data type support (String, Integer, Boolean, List)
- ⚡ Fast in-memory operations with disk backup
- 🔄 CRUD operations (Create, Read, Update, Delete)
- 📝 Human-readable storage format
- 💻 Interactive command-line interface
- 🧵 Thread-safe operations
- 🔍 Type verification and validation

## Quick Start

```java
// Initialize the database
CacheBox db = new CacheBox("mydata.cbx");

// Store different types of values
db.put("name", CacheValue.of("John Doe"));          // String
db.put("age", CacheValue.of(30));                   // Integer
db.put("active", CacheValue.of(true));              // Boolean
db.put("tags", CacheValue.of(Arrays.asList("a","b"))); // List
```

## Command Line Interface

```bash
$ java CacheBox
CacheBox v1.0
Type 'help' for available commands

cbox> help

Available commands:
put <type> <key> <value> - Store a value of specified type
  Types: string, int, bool, list
  Example: put string name John
  Example: put int age 25
  Example: put bool active true
  Example: put list colors red,blue,green
get <key> - Retrieve a value by key
delete <key> - Delete a key-value pair
list - Show all stored key-value pairs
type <key> - Show the type of a stored value
help - Show this help message
exit - Exit the program
```

## Data Types Support

Currently supported types:
- String: Text values
- Integer: Whole numbers
- Boolean: true/false values
- List: Comma-separated values
- Null: Explicit null values

## Storage Format

CacheBox uses a type-aware storage format:
```
key=TYPE:value
name=STRING:John Doe
age=INTEGER:30
active=BOOLEAN:true
colors=LIST:red,blue,green
```

## Roadmap

### Coming in v1.1
- Search & Query Features
  - Pattern matching
  - Range queries
  - Type filtering

### Coming in v1.2
- Data Validation & Constraints
  - Value format validation
  - Required fields
  - Custom validation rules

### Coming in v1.3
- Transaction Support
  - Atomic operations
  - Rollback capability
  - Operation logging

### Coming in v1.4
- Backup & Recovery
  - Automatic backups
  - Point-in-time recovery
  - Import/Export

## Use Cases

- Application configuration storage
- Development and testing environments
- Small-scale data storage needs
- Prototyping and proof of concepts
- Educational purposes
- Simple caching solutions

## Best Practices

1. Key Naming
   - Use consistent naming conventions
   - Avoid special characters
   - Keep keys readable and meaningful

2. Data Types
   - Use appropriate types for values
   - Consider using lists for related data
   - Handle null values explicitly

3. Operations
   - Check value types before operations
   - Handle exceptions appropriately
   - Regular backups of data files

## Contributing

Areas where you can help:
- Implementing upcoming roadmap features
- Adding more data types
- Improving performance
- Adding test coverage
- Documentation improvements
- Bug fixes

## License

MIT License - feel free to use this in your own projects!

## Version History

- 1.0 (Current)
  - Multi-type support (String, Integer, Boolean, List)
  - Enhanced CLI with type commands
  - Improved error handling
  - Type-aware storage format

- 0.1 (Initial Release)
  - Basic CRUD operations
  - String-only support
  - Simple CLI
  - File-based persistence

## Support

For issues, questions, or contributions, please open an issue in the repository.

---
Made with ☕ by developers, for developers
