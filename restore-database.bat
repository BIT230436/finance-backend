@echo off
REM ===================================================================
REM Database Restore Script for Windows
REM ===================================================================
REM This script restores the finance_db MySQL database from a backup
REM 
REM Usage: restore-database.bat <backup_file.sql>
REM Example: restore-database.bat backups\finance_db_20250101_120000.sql
REM ===================================================================

SETLOCAL EnableDelayedExpansion

REM Configuration
SET DB_NAME=finance_db
SET DB_USER=root
SET DB_PASSWORD=160925
SET DB_HOST=localhost
SET DB_PORT=3306

REM MySQL path (update if needed)
SET MYSQL_PATH=mysql

REM Check if backup file is provided
if "%~1"=="" (
    echo ERROR: No backup file specified!
    echo.
    echo Usage: restore-database.bat ^<backup_file^>
    echo Example: restore-database.bat backups\finance_db_20250101_120000.sql
    echo.
    echo Available backups:
    dir /B backups\*.sql 2>nul
    dir /B backups\*.sql.gz 2>nul
    pause
    exit /b 1
)

SET BACKUP_FILE=%~1

REM Check if file exists
if not exist "%BACKUP_FILE%" (
    echo ERROR: Backup file not found: %BACKUP_FILE%
    pause
    exit /b 1
)

echo ===================================================================
echo Finance DB Restore Script
echo ===================================================================
echo Database: %DB_NAME%
echo Host: %DB_HOST%:%DB_PORT%
echo Backup file: %BACKUP_FILE%
echo.

REM Check if file is compressed
echo %BACKUP_FILE% | findstr /C:".gz" >nul
if %ERRORLEVEL% EQU 0 (
    echo File is compressed. Extracting...
    
    REM Check if 7-Zip is available
    where 7z >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: 7-Zip not found! Please install 7-Zip or extract manually.
        pause
        exit /b 1
    )
    
    7z x "%BACKUP_FILE%" -o"%~dp1" -y
    SET BACKUP_FILE=%BACKUP_FILE:~0,-3%
    echo Extracted to: !BACKUP_FILE!
    echo.
)

echo WARNING: This will DROP and RECREATE the database!
echo All current data will be LOST!
echo.
set /p CONFIRM="Are you sure you want to continue? (yes/no): "

if /I not "%CONFIRM%"=="yes" (
    echo Restore cancelled.
    pause
    exit /b 0
)

echo.
echo Restoring database...

REM Drop and recreate database
"%MYSQL_PATH%" --host=%DB_HOST% --port=%DB_PORT% --user=%DB_USER% --password=%DB_PASSWORD% -e "DROP DATABASE IF EXISTS %DB_NAME%; CREATE DATABASE %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to recreate database!
    pause
    exit /b 1
)

REM Import backup
"%MYSQL_PATH%" --host=%DB_HOST% --port=%DB_PORT% --user=%DB_USER% --password=%DB_PASSWORD% %DB_NAME% < "%BACKUP_FILE%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===================================================================
    echo Restore completed successfully!
    echo ===================================================================
) else (
    echo.
    echo ===================================================================
    echo ERROR: Restore failed!
    echo ===================================================================
    exit /b 1
)

ENDLOCAL
pause

