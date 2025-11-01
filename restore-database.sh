#!/bin/bash
# ===================================================================
# Database Restore Script for Linux/Mac
# ===================================================================
# This script restores the finance_db MySQL database from a backup
# 
# Usage: ./restore-database.sh <backup_file.sql>
# Example: ./restore-database.sh backups/finance_db_20250101_120000.sql.gz
# ===================================================================

# Configuration
DB_NAME="finance_db"
DB_USER="root"
DB_PASSWORD="your_password"  # Update this!
DB_HOST="localhost"
DB_PORT="3306"

# Check if backup file is provided
if [ -z "$1" ]; then
    echo "ERROR: No backup file specified!"
    echo ""
    echo "Usage: ./restore-database.sh <backup_file>"
    echo "Example: ./restore-database.sh backups/finance_db_20250101_120000.sql.gz"
    echo ""
    echo "Available backups:"
    ls -1 backups/*.sql* 2>/dev/null || echo "No backups found in backups/ directory"
    exit 1
fi

BACKUP_FILE="$1"

# Check if file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERROR: Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "==================================================================="
echo "Finance DB Restore Script"
echo "==================================================================="
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "Backup file: $BACKUP_FILE"
echo ""

# Check if file is compressed
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo "File is compressed. Extracting..."
    gunzip -c "$BACKUP_FILE" > "${BACKUP_FILE%.gz}"
    BACKUP_FILE="${BACKUP_FILE%.gz}"
    echo "Extracted to: $BACKUP_FILE"
    echo ""
fi

echo "WARNING: This will DROP and RECREATE the database!"
echo "All current data will be LOST!"
echo ""
read -p "Are you sure you want to continue? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

echo ""
echo "Restoring database..."

# Drop and recreate database
mysql --host="$DB_HOST" \
      --port="$DB_PORT" \
      --user="$DB_USER" \
      --password="$DB_PASSWORD" \
      -e "DROP DATABASE IF EXISTS $DB_NAME; CREATE DATABASE $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to recreate database!"
    exit 1
fi

# Import backup
mysql --host="$DB_HOST" \
      --port="$DB_PORT" \
      --user="$DB_USER" \
      --password="$DB_PASSWORD" \
      "$DB_NAME" < "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "==================================================================="
    echo "Restore completed successfully!"
    echo "==================================================================="
else
    echo ""
    echo "==================================================================="
    echo "ERROR: Restore failed!"
    echo "==================================================================="
    exit 1
fi

