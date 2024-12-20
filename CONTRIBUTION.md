# Contribution Guide for CacheBox

Thank you for considering contributing to CacheBox! We appreciate your interest and support in improving this lightweight, multi-type key-value database. This guide outlines the ways you can contribute, along with best practices for making your contributions as effective as possible.

---

## Table of Contents

1. [How to Contribute](#how-to-contribute)
2. [Code of Conduct](#code-of-conduct)
3. [Reporting Issues](#reporting-issues)
4. [Submitting Pull Requests](#submitting-pull-requests)
5. [Areas for Contribution](#areas-for-contribution)
6. [Development Setup](#development-setup)
7. [Style Guide](#style-guide)
8. [Testing Contributions](#testing-contributions)

---

## How to Contribute

You can contribute to CacheBox in various ways:

- Reporting bugs or suggesting new features
- Fixing issues and submitting pull requests
- Improving documentation and examples
- Enhancing performance or adding test coverage

If you're new to open-source, check out [this guide](https://opensource.guide/how-to-contribute/) to get started!

---

## Code of Conduct

Please adhere to our Code of Conduct. We are committed to fostering an inclusive and respectful community. Be kind, constructive, and respectful in your interactions.

---

## Reporting Issues

If you encounter a bug, have a feature request, or notice something off in the documentation, please let us know by opening an issue in the repository. Include the following details:

1. A clear and descriptive title
2. Steps to reproduce the issue (if applicable)
3. Expected vs actual behavior
4. Relevant logs, screenshots, or error messages
5. Environment details (e.g., Java version, OS, etc.)

---

## Submitting Pull Requests

Follow these steps to submit a pull request:

1. Fork the repository and create a new branch:

   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes, ensuring the following:

    - Code adheres to the project’s style guide
    - Your changes are well-documented
    - Tests are added or updated if necessary

3. Commit your changes with a descriptive message:

   ```bash
   git commit -m "Add feature: your feature description"
   ```

4. Push your branch to your forked repository:

   ```bash
   git push origin feature/your-feature-name
   ```

5. Open a pull request to the `main` branch of the CacheBox repository.

6. Wait for a review, and address any comments or requested changes.

---

## Areas for Contribution

Here are some areas where you can contribute:

### 1. **Roadmap Features**

- **Validation & Constraints (v1.2)**:

    - Add value format validation
    - Implement required fields
    - Extend custom validation rules

- **Transactions (v1.3)**:

    - Develop atomic operations
    - Add rollback support
    - Enable operation logging

- **Backup & Recovery (v1.4)**:

    - Create automatic backup mechanisms
    - Add point-in-time recovery
    - Implement data import/export utilities

### 2. **Performance Improvements**

- Optimize file-based persistence
- Improve in-memory operations
- Enhance query performance

### 3. **Testing**

- Write unit tests for uncovered features
- Add integration tests for CLI and core functionality

### 4. **Documentation**

- Expand usage examples
- Add tutorials for new features
- Enhance developer setup instructions

### 5. **Bug Fixes**

- Check the issue tracker for reported bugs
- Identify and fix bugs in core features or CLI

---

## Development Setup

Follow these steps to set up CacheBox for development:

1. Clone the repository:

   ```bash
   git clone https://github.com/your-username/cachebox.git
   cd cachebox
   ```

2. Install the required JDK (Java 21):

   ```bash
   sudo apt update
   sudo apt install openjdk-21-jdk
   ```

3. Build the project:

   ```bash
   mvn clean package
   ```

4. Run the application:

   ```bash
   java -jar target/cachebox-1.1.jar
   ```

5. Run the tests:

   ```bash
   mvn test
   ```

---

## Style Guide

- **Code Style**: Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
- **Naming Conventions**: Use descriptive names for variables, methods, and classes.
- **Comments**: Write clear and concise comments explaining non-obvious code.
- **Documentation**: Use Javadoc for public methods and classes.

---

## Testing Contributions

- Write tests for any new features or bug fixes.
- Use `JUnit` for unit tests.
- Run all tests locally before submitting a pull request.
- Ensure test coverage remains high (above 90%).

---

Thank you for contributing to CacheBox! Together, we can make this project even better. If you have any questions or need assistance, feel free to open an issue or contact the maintainers.

