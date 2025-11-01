#!/bin/bash
# ===================================================================
# Database Backup Script for Linux/Mac
# ===================================================================
# This script creates a backup of the finance_db MySQL database
# 
# Prerequisites:
# 1. MySQL/MariaDB installed
# 2. mysqldump command available
# 3. Update USERNAME and PASSWORD below
# 
# Usage: chmod +x backup-database.sh && ./backup-database.sh
# 
# For automated backups, add to crontab:
# 0 2 * * * /path/to/backup-database.sh
# (This runs backup every day at 2 AM)
# ===================================================================

# Configuration
DB_NAME="finance_db"
DB_USER="root"
DB_PASSWORD="your_password"  # Update this!
DB_HOST="localhost"
DB_PORT="3306"

# Backup directory
BACKUP_DIR="backups"
DATE_TIME=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${DATE_TIME}.sql"

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

echo "==================================================================="
echo "Finance DB Backup Script"
echo "==================================================================="
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "Backup file: $BACKUP_FILE"
echo ""

# Perform backup
echo "Starting backup..."
mysqldump --host="$DB_HOST" \
          --port="$DB_PORT" \
          --user="$DB_USER" \
          --password="$DB_PASSWORD" \
          --single-transaction \
          --routines \
          --triggers \
          --events \
          "$DB_NAME" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "==================================================================="
    echo "Backup completed successfully!"
    echo "==================================================================="
    echo "File: $BACKUP_FILE"
    echo "Size: $(du -h "$BACKUP_FILE" | cut -f1)"
    echo ""
    
    # Compress backup
    echo "Compressing backup..."
    gzip "$BACKUP_FILE"
    if [ $? -eq 0 ]; then
        echo "Compressed to: ${BACKUP_FILE}.gz"
    fi
    
    # Delete backups older than 30 days
    echo ""
    echo "Cleaning old backups (older than 30 days)..."
    find "$BACKUP_DIR" -name "*.sql.gz" -type f -mtime +30 -delete
    find "$BACKUP_DIR" -name "*.sql" -type f -mtime +30 -delete
    
    echo ""
    echo "All done!"
    echo "==================================================================="
else
    echo ""
    echo "==================================================================="
    echo "ERROR: Backup failed!"
    echo "==================================================================="
    echo "Please check:"
    echo "1. MySQL is running"
    echo "2. Database name is correct"
    echo "3. Username and password are correct"
    echo "4. mysqldump is installed"
    echo ""
    exit 1
fi

