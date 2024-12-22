# CacheBox: A Lightweight Key-Value Database for Exploratory Data Analysis

CacheBox is a lightweight, multi-type key-value database implemented in Java, designed to simplify data storage and analysis workflows. It features file-based persistence, advanced search capabilities, and an interactive CLI for seamless data exploration. Whether you're a student learning about databases, working on small-scale data storage, prototyping, or participating in open-source projects, CacheBox provides a simple and robust solution.

---

## 🌟 Why Choose CacheBox?

- **Learn by Building**: Ideal for students and beginners to explore key-value databases.
- **Open Source**: Fully customizable for your projects.
- **Lightweight & Easy-to-Use**: Runs directly from the command line with no external dependencies.
- **Feature-Rich**: Supports multiple data types, encryption, and real-time monitoring.

---

## Key Features

### ⚙️ Core Functionalities
- **Zero External Dependencies**: CacheBox is built entirely in Java, requiring no additional libraries.
- **Multi-Type Support**: Store data in various formats, including:
  - `String`
  - `Integer`
  - `Boolean`
  - `List`
  - `Null`
- **File-Based Persistence**: Automatically saves data to disk, ensuring durability across sessions.
- **Interactive CLI**: User-friendly command-line interface for easy data management.

### 🔍 Advanced Features
- **Advanced Search**: Perform complex queries with pattern matching, range searches, and type filtering.
- **Encryption**: Secure your data with AES, XOR, or no encryption.
- **Sharding & Load Balancing**: Scale horizontally by distributing data across shards.
- **Real-Time Monitoring**: Track performance metrics and visualize data in real time.
- **Network Interface**: Access CacheBox remotely using the custom CBSP protocol.
- **Data Validation**: Ensure data integrity with custom validation rules and type constraints.

---

## 🚀 Getting Started

### Installation

CacheBox is designed to run directly from the command line. Simply download the `cachebox.jar` file and execute it:

#### Start the CLI:
```bash
java -jar cachebox.jar cli
```

#### Start the Server:
```bash
java -jar cachebox.jar server
```

---

### Quick Start

#### Using the CLI

1. **Start CacheBox CLI**:
   ```bash
   java -jar cachebox.jar cli
   ```

2. **Basic Commands**:
   - **Store a Value**:
     ```bash
     db> put string name John
     ```
   - **Retrieve a Value**:
     ```bash
     db> get name
     ```
   - **Search for Values**:
     ```bash
     db> search -pattern "J.*"
     ```
   - **Enable Encryption**:
     ```bash
     db> encrypt enable
     ```

#### Using the Server

1. **Start the Server**:
   ```bash
   java -jar cachebox.jar server
   ```

2. **Connect to the Server**:
   - Default port: `20029`
   - Use a client or tools like `telnet` to interact:
     ```bash
     telnet localhost 20029
     ```

---

## 📊 Advanced Features

### Advanced Search

Perform complex queries to filter and retrieve data efficiently:
- **Pattern Matching**:
  ```bash
  db> search -pattern "na.*"
  ```
- **Range Queries**:
  ```bash
  db> search -range 10 50
  ```
- **Type Filtering**:
  ```bash
  db> search -type string
  ```
- **Staged vs. Committed**:
  ```bash
  db> search -staged
  db> search -committed
  ```

### Encryption

Protect your data with built-in encryption:
- **Enable Encryption**:
  ```bash
  db> encrypt enable
  ```
- **Set Encryption Algorithm**:
  ```bash
  db> encrypt set_algorithm AES
  ```
- **Manage Encryption Keys**:
  ```bash
  db> encrypt set_key my16bytekey
  db> encrypt generate_key
  ```

### Real-Time Monitoring

Stay on top of performance:
- **Start Live Monitoring**:
  ```bash
  db> live_performance
  ```
- **Take a Snapshot**:
  ```bash
  db> snapshot_performance
  ```
- **Stop Live Monitoring**:
  ```bash
  db> stop_lp
  ```

### Sharding and Load Balancing

Distribute data across multiple shards for enhanced performance and scalability.

---

## 🌍 Open Source Community

### Contributing

We welcome contributions from everyone! Here are some ways to get involved:
- **Implement New Features**: Work on the roadmap or suggest your own ideas.
- **Enhance Performance**: Optimize existing functionality.
- **Add More Data Types**: Extend multi-type support.
- **Improve Documentation**: Make it easier for others to learn and use CacheBox.
- **Test and Debug**: Help us identify and resolve issues.

### How to Contribute
1. Fork the repository on GitHub.
2. Create a new branch for your feature or fix.
3. Submit a pull request with detailed explanations.

---

## 🎓 Educational Use Cases

CacheBox is perfect for students and developers who want to:
- Learn about key-value databases and their functionality.
- Prototype small-scale applications without complex setups.
- Explore advanced features like encryption, sharding, and monitoring.
- Understand the basics of building a server-client protocol (CBSP).

---

## 🔧 Best Practices

1. **Use Consistent Keys**: Avoid special characters and keep key names clear.
2. **Choose Appropriate Types**: Match data types to the nature of your data.
3. **Secure Data**: Enable encryption for sensitive information.
4. **Leverage Sharding**: Scale your workload effectively.
5. **Monitor Regularly**: Use real-time metrics to track performance.

---

## 🛠️ Technical Details

### CBSP Protocol
CacheBox Serialization Protocol (CBSP) ensures efficient server-client communication.
- **Supported Commands**:
  - `PUT`: Store a value.
  - `GET`: Retrieve a value by key.
  - `DELETE`: Remove a key-value pair.
  - `SEARCH`: Perform queries.
  - `PING`: Check server availability.

#### Example Request
```
*3
$3
PUT
$4
name
$4
John
```

#### Example Response
```
+OK
```

### Version History
- **2.0** (Current):
  - Encryption, sharding, and real-time monitoring added.
  - Network interface introduced.
- **1.1**:
  - Advanced search capabilities.
- **1.0**:
  - Multi-type support and enhanced CLI.
- **0.1**:
  - Initial release with basic CRUD and persistence.

---

## License

CacheBox is licensed under the MIT License, making it free and open to use for all!

---

## Support

Have questions or need help? Open an issue in the repository or reach out to the community.

---

Made with 💻 and ☕ by developers, for developers. Start exploring CacheBox today!

