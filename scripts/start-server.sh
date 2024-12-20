#!/bin/bash

# Path to the JAR file
JAR_FILE="target/_code-1.0-SNAPSHOT.jar"

# Check if the JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please build the project using 'mvn clean package' first."
    exit 1
fi

# Start the server
java -jar "$JAR_FILE" server