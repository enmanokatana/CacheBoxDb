# CacheBox: A Lightweight Key-Value Database for Exploratory Data Analysis

CacheBox is a lightweight, multi-type key-value database implemented in Java, designed to simplify your data storage and analysis workflow. It integrates file-based persistence, advanced search capabilities, and an interactive CLI for seamless data exploration. Whether you're working on small-scale data storage, prototyping, or educational purposes, CacheBox provides a robust solution for your needs.

---

## Key Features

- **Zero External Dependencies**: Built entirely in Java, CacheBox requires no additional libraries.
- **File-Based Persistence**: Data is automatically saved to disk, ensuring durability across sessions.
- **Multi-Type Support**: Store and retrieve data in various formats, including String, Integer, Boolean, List, and Null.
- **Advanced Search**: Perform complex queries with pattern matching, range searches, and type filtering.
- **Interactive CLI**: A user-friendly command-line interface for managing your data.
- **Encryption Support**: Secure your data with AES, XOR, or no encryption strategies.
- **Sharding and Load Balancing**: Distribute data across multiple shards for better performance.
- **Real-Time Monitoring**: Track performance metrics and visualize data with live monitoring.
- **Data Validation**: Ensure data integrity with custom validation rules and type constraints.
- **Network Interface and Server**: Run CacheBox as a server with a custom CBSP protocol for networked access.

---

## Getting Started

### Installation

To use CacheBox, follow these steps:

1. Clone the repository:

   ```bash
   git clone https://github.com/enmanokatana/CacheBoxDb.git
   ```

2. Navigate to the project directory:

   ```bash
   cd cachebox
   ```

3. Build the project using Maven:

   ```bash
   mvn clean package
   ```

4. Navigate to the scripts directory:

   ```bash
   cd scripts
   ```

5. Start the CLI or server mode depending on your use case:

   - To start the CLI:
     ```bash
     ./start-cli
     ```
   - To start the server:
     ```bash
     ./start-server
     ```

---

### Quick Start

#### Using the CLI

1. Start the CacheBox CLI:

   ```bash
   ./start-cli
   ```

2. Follow the on-screen instructions to interact with the database.

#### Example Commands

- **Store a value**:

  ```bash
  db> put string name John
  ```

- **Retrieve a value**:

  ```bash
  db> get name
  ```

- **Search for values**:

  ```bash
  db> search -pattern "J.*"
  ```

- **Enable encryption**:

  ```bash
  db> encrypt enable
  ```

---

### Using the Server

CacheBox also supports a server mode for networked access. Start the server with:

```bash
./start-server
```

The server listens on port `20029` by default. You can interact with it using the provided client or custom scripts.

---

## Advanced Features

### Search Functionality

CacheBox supports advanced search queries with the following options:

- **Pattern Matching**: Use regular expressions to search keys or values.
- **Range Queries**: Filter numeric values within a specified range.
- **Type Filtering**: Restrict results to specific data types (e.g., String, Integer).
- **Staged vs. Committed**: Search only staged (uncommitted) or committed data.

#### Example Commands

```bash
db> search -pattern "na.*"
db> search -range 10 50
db> search -type string
db> search -staged
db> search -committed
```

---

### Encryption

CacheBox supports encryption for data at rest. You can enable encryption, set encryption algorithms (AES, XOR, or no encryption), and manage encryption keys.

#### Example Commands

```bash
db> encrypt enable
db> encrypt set_algorithm AES
db> encrypt set_key my16bytekey
db> encrypt generate_key
```

---

### Sharding and Load Balancing

CacheBox supports sharding and load balancing across multiple cache boxes. The `ShardedCacheBox` class manages multiple shards, and the `LoadBalancer` distributes requests across these shards.

---

### Real-Time Monitoring

CacheBox provides real-time performance monitoring and snapshot metrics. You can start live monitoring or take a snapshot of the current performance metrics.

#### Example Commands

```bash
db> snapshot_performance
db> live_performance
db> stop_lp
```

---

## Network Interface and Server

### CBSP Protocol

CacheBox includes a custom **CBSP (CacheBox Serialization Protocol)** for communication between the server and clients. The CBSP protocol is designed to be simple and efficient, supporting the following operations:

- **PUT**: Store a value in the database.
- **GET**: Retrieve a value by key.
- **DELETE**: Remove a key-value pair.
- **SEARCH**: Perform advanced queries.
- **PING**: Check server availability.

#### Example CBSP Request

```
*3
$3
PUT
$4
name
$4
John
```

#### Example CBSP Response

```
+OK
```

### Server Mode

The CacheBox server runs on port `20029` and supports multiple client connections. It uses a thread pool to handle concurrent requests efficiently.

#### Starting the Server

```bash
./start-server
```

#### Example Client Interaction

You can interact with the server using a custom client or tools like `telnet`:

```bash
telnet localhost 20029
```

---

## Use Cases

- **Application Configuration Storage**: Store application settings and configurations.
- **Development and Testing**: Use CacheBox for quick data storage in development environments.
- **Small-Scale Data Storage**: Ideal for small-scale data storage needs.
- **Prototyping and Proof of Concepts**: Quickly prototype ideas without complex setup.
- **Educational Purposes**: Learn about key-value databases and data persistence.
- **Simple Caching Solutions**: Use CacheBox as a lightweight caching layer.

---

## Best Practices

1. **Key Naming**: Use consistent naming conventions and avoid special characters.
2. **Data Types**: Use appropriate data types for values and handle null values explicitly.
3. **Operations**: Check value types before operations and handle exceptions appropriately.
4. **Search Queries**: Use patterns, ranges, and type filtering for structured searches.
5. **Encryption**: Use encryption for sensitive data and manage keys securely.
6. **Sharding**: Distribute data across multiple shards for better performance and scalability.
7. **Monitoring**: Regularly monitor performance and take snapshots for analysis.

---

## Contributing

We welcome contributions to CacheBox! Here are some areas where you can help:

- **Implementing Features**: Work on upcoming roadmap features.
- **Adding Data Types**: Extend support for more data types.
- **Performance Improvements**: Optimize the database for better performance.
- **Testing**: Add test coverage for existing and new features.
- **Documentation**: Improve the documentation and examples.
- **Bug Fixes**: Report and fix bugs in the repository.

---

## License

CacheBox is licensed under the MIT License. Feel free to use it in your own projects!

---

## Version History

- **2.2 (Current)**

  - Added encryption support with AES, XOR, and no encryption strategies.
  - Introduced sharding and load balancing.
  - Added real-time performance monitoring and snapshot metrics.
  - Enhanced CLI with encryption management and live monitoring.
  - Added network interface and CBSP protocol for server mode.

- **1.1**

  - Added advanced search and query capabilities.
  - Pattern matching, range queries, and type filtering.
  - Updated CLI to include search options.

- **1.0**

  - Multi-type support (String, Integer, Boolean, List).
  - Enhanced CLI with type commands.
  - Improved error handling.
  - Type-aware storage format.

- **0.1 (Initial Release)**

  - Basic CRUD operations.
  - String-only support.
  - Simple CLI.
  - File-based persistence.

---

## Support

For issues, questions, or contributions, please open an issue in the repository.

---

Made with ☕ by developers, for developers!

