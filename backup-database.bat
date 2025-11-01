@echo off
REM ===================================================================
REM Database Backup Script for Windows
REM ===================================================================
REM This script creates a backup of the finance_db MySQL database
REM 
REM Prerequisites:
REM 1. MySQL/MariaDB installed
REM 2. mysqldump in PATH or update MYSQLDUMP_PATH below
REM 3. Update USERNAME and PASSWORD below
REM 
REM Usage: backup-database.bat
REM ===================================================================

SETLOCAL EnableDelayedExpansion

REM Configuration
SET DB_NAME=finance_db
SET DB_USER=root
SET DB_PASSWORD=160925
SET DB_HOST=localhost
SET DB_PORT=3306

REM Backup directory
SET BACKUP_DIR=backups
SET DATE_TIME=%date:~-4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%
SET DATE_TIME=%DATE_TIME: =0%
SET BACKUP_FILE=%BACKUP_DIR%\%DB_NAME%_%DATE_TIME%.sql

REM MySQL dump path (update if needed)
SET MYSQLDUMP_PATH=mysqldump

REM Create backup directory if not exists
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo ===================================================================
echo Finance DB Backup Script
echo ===================================================================
echo Database: %DB_NAME%
echo Host: %DB_HOST%:%DB_PORT%
echo Backup file: %BACKUP_FILE%
echo.

REM Perform backup
echo Starting backup...
"%MYSQLDUMP_PATH%" --host=%DB_HOST% --port=%DB_PORT% --user=%DB_USER% --password=%DB_PASSWORD% --single-transaction --routines --triggers --events %DB_NAME% > "%BACKUP_FILE%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===================================================================
    echo Backup completed successfully!
    echo ===================================================================
    echo File: %BACKUP_FILE%
    echo Size: 
    for %%A in ("%BACKUP_FILE%") do echo %%~zA bytes
    echo.
    
    REM Compress backup (if 7-Zip is available)
    where 7z >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo Compressing backup...
        7z a -tgzip "%BACKUP_FILE%.gz" "%BACKUP_FILE%"
        if %ERRORLEVEL% EQU 0 (
            del "%BACKUP_FILE%"
            echo Compressed to: %BACKUP_FILE%.gz
        )
    )
    
    REM Delete backups older than 30 days
    echo.
    echo Cleaning old backups (older than 30 days)...
    forfiles /P "%BACKUP_DIR%" /S /M *.sql* /D -30 /C "cmd /c del @path" 2>nul
    
    echo.
    echo All done!
) else (
    echo.
    echo ===================================================================
    echo ERROR: Backup failed!
    echo ===================================================================
    echo Please check:
    echo 1. MySQL is running
    echo 2. Database name is correct
    echo 3. Username and password are correct
    echo 4. mysqldump is in PATH
    echo.
    exit /b 1
)

ENDLOCAL
pause

