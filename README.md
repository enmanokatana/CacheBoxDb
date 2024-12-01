Here's the updated README file with the added **Data Validation & Constraints** section:

---

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
- 🔍 Advanced search and query capabilities
- 🧪 Type verification and validation
- ✅ Data Validation & Constraints (v1.2)

## Quick Start

```java
// Initialize the database
CacheBox db = new CacheBox("mydata.cbx");

// Store different types of values
db.put("name", CacheValue.of("John Doe"));          // String
db.put("age", CacheValue.of(30));                   // Integer
db.put("active", CacheValue.of(true));              // Boolean
db.put("tags", CacheValue.of(Arrays.asList("a", "b"))); // List
```

## Command Line Interface

```bash
$ java CacheBox
CacheBox v1.1
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
search [options] - Search and filter stored data
  Options:
    -pattern <regex>   Search by pattern
    -range <min> <max> Search by numeric range
    -type <type>       Filter by type (string, int, bool, list)
help - Show this help message
exit - Exit the program
```

## Search Functionality

The new search command allows users to perform advanced queries on the stored data:
- `-pattern <regex>`: Match keys or values using a regular expression.
- `-range <min> <max>`: Search for numeric values within a specified range.
- `-type <type>`: Filter results by data type (e.g., `string`, `int`, `bool`, `list`).

### Example Commands
```bash
cbox> search -pattern "na.*"
cbox> search -range 10 50
cbox> search -type string
```

## Data Types Support

Currently supported types:
- String: Text values
- Integer: Whole numbers
- Boolean: true/false values
- List: Comma-separated values
- Null: Explicit null values

## Data Validation & Constraints (v1.2)

### Validation Rules
- **Required Fields**: You can mark specific keys as required, ensuring that the value for that key must be provided when stored.
- **Type Validation**: CacheBox ensures that the value's type matches the type specified for that key (e.g., a string cannot be stored as an integer).
- **Custom Validation Rules**: You can add custom validation rules such as checking the value against specific constraints (e.g., numerical ranges, regex patterns).

### Example Usage
```java
// Create the validation rules
ValidationRule<Integer> ageRule = ValidationRule.forKey("age", Integer.class)
    .required()
    .addValidator(value -> value >= 0 && value <= 120);

ValidationRule<String> nameRule = ValidationRule.forKey("name", String.class)
    .required()
    .addValidator(value -> value.length() > 1);

// Add rules to the manager
ValidationManager validationManager = new ValidationManager();
validationManager.addRule(ageRule);
validationManager.addRule(nameRule);

// Example cache value to validate
CacheValue ageValue = CacheValue.of(25);
CacheValue nameValue = CacheValue.of("John Doe");

// Validate for a specific key
ValidationResult ageValidationResult = validationManager.validate("age", ageValue);
ValidationResult nameValidationResult = validationManager.validate("name", nameValue);

// Process validation results
if (!ageValidationResult.isValid()) {
    System.out.println(ageValidationResult.getErrorMessage());
}
if (!nameValidationResult.isValid()) {
    System.out.println(nameValidationResult.getErrorMessage());
}
```

### How It Works:
- Validation rules can be added for specific keys, and CacheBox will validate data before storing it.
- Custom validation logic can be applied through predicates for various use cases.
- Validation is only triggered for the keys with associated rules, ensuring that your data is validated correctly.

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

1. **Key Naming**
   - Use consistent naming conventions
   - Avoid special characters
   - Keep keys readable and meaningful

2. **Data Types**
   - Use appropriate types for values
   - Consider using lists for related data
   - Handle null values explicitly

3. **Operations**
   - Check value types before operations
   - Handle exceptions appropriately
   - Regular backups of data files

4. **Search Queries**
   - Use patterns to filter text-based data
   - Specify ranges for numeric searches
   - Leverage type filtering for structured data

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

- **1.1 (Current)**
  - Added advanced search and query capabilities
  - Pattern matching, range queries, and type filtering
  - Updated CLI to include search options

- **1.0**
  - Multi-type support (String, Integer, Boolean, List)
  - Enhanced CLI with type commands
  - Improved error handling
  - Type-aware storage format

- **0.1 (Initial Release)**
  - Basic CRUD operations
  - String-only support
  - Simple CLI
  - File-based persistence

## Support

For issues, questions, or contributions, please open an issue in the repository.

---
Made with ☕ by developers, for developers!

---
