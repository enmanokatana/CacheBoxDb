#!/bin/bash

# Directory to store the database files
DB_DIR="db"

# Create the directory if it does not exist
mkdir -p $DB_DIR

# List of shard files to create
SHARDS=("shard1_0" "shard1_1" "shard1_2" "shard1_3" "shard2_0" "shard2_1" "shard2_2" "shard2_3" "shard3_0" "shard3_1" "shard3_2" "shard3_3")

# Create each shard file if it does not exist
for SHARD in "${SHARDS[@]}"; do
    SHARD_FILE="$DB_DIR/$SHARD"
    if [ ! -f "$SHARD_FILE" ]; then
        touch "$SHARD_FILE"
        echo "Created $SHARD_FILE"
    else
        echo "$SHARD_FILE already exists"
    fi
done

echo "Database initialization complete."