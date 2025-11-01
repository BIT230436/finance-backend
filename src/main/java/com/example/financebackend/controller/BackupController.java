package com.example.financebackend.controller;

import com.example.financebackend.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Backup & Restore Controller
 * API để backup và restore database
 */
@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/finance_db}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:root}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    /**
     * Trigger database backup
     * Only ADMIN can backup
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerBackup() {
        Long userId = AuthUtil.getCurrentUserId();
        logger.info("Backup triggered by user: {}", userId);

        try {
            // Extract database name from URL
            String dbName = extractDatabaseName(datasourceUrl);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupDir = "backups";
            String backupFile = String.format("%s/%s_%s.sql", backupDir, dbName, timestamp);

            // Create backup directory if not exists
            Files.createDirectories(Paths.get(backupDir));

            // Build mysqldump command
            ProcessBuilder processBuilder = new ProcessBuilder(
                "mysqldump",
                "--user=" + dbUsername,
                "--password=" + dbPassword,
                "--single-transaction",
                "--routines",
                "--triggers",
                "--result-file=" + backupFile,
                dbName
            );

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // Check if file was created
                File file = new File(backupFile);
                if (file.exists()) {
                    long fileSize = file.length();

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Backup thành công");
                    response.put("filename", backupFile);
                    response.put("size", fileSize);
                    response.put("timestamp", timestamp);

                    logger.info("Backup successful: {} ({}  bytes)", backupFile, fileSize);
                    return ResponseEntity.ok(response);
                } else {
                    throw new RuntimeException("Backup file không được tạo");
                }
            } else {
                logger.error("Backup failed with exit code: {}. Output: {}", exitCode, output);
                throw new RuntimeException("Backup thất bại: " + output);
            }

        } catch (Exception e) {
            logger.error("Backup error: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi backup: " + e.getMessage());
            response.put("hint", "Đảm bảo mysqldump có trong PATH và database đang chạy");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get backup status - last backup time
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBackupStatus() {
        try {
            File backupDir = new File("backups");
            Map<String, Object> response = new HashMap<>();

            if (!backupDir.exists() || !backupDir.isDirectory()) {
                response.put("lastBackup", null);
                response.put("message", "Chưa có backup nào");
                return ResponseEntity.ok(response);
            }

            // Find latest backup file
            File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".sql") || name.endsWith(".sql.gz"));

            if (files == null || files.length == 0) {
                response.put("lastBackup", null);
                response.put("message", "Chưa có backup nào");
                return ResponseEntity.ok(response);
            }

            // Get latest file
            File latestFile = files[0];
            for (File file : files) {
                if (file.lastModified() > latestFile.lastModified()) {
                    latestFile = file;
                }
            }

            LocalDateTime lastModified = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(latestFile.lastModified()),
                java.time.ZoneId.systemDefault()
            );

            response.put("lastBackup", lastModified);
            response.put("filename", latestFile.getName());
            response.put("size", latestFile.length());
            response.put("enabled", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting backup status: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("lastBackup", null);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    private String extractDatabaseName(String jdbcUrl) {
        // Extract from jdbc:mysql://localhost:3306/database_name
        String[] parts = jdbcUrl.split("/");
        String dbPart = parts[parts.length - 1];
        // Remove query parameters if any
        return dbPart.split("\\?")[0];
    }
}

