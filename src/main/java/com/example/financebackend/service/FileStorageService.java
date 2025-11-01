package com.example.financebackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, Long userId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tin trống");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Tên tệp tin trống");
        }

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("Chỉ chấp nhận tệp tin hình ảnh và PDF");
        }

        // Validate file size (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước tệp tin phải nhỏ hơn 5MB");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, "receipts", userId.toString());
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String filename = UUID.randomUUID().toString() + extension;
        Path targetLocation = uploadPath.resolve(filename);

        // Save file
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path for database storage
        return "/uploads/receipts/" + userId + "/" + filename;
    }

    public void deleteFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        Path fileToDelete = Paths.get(uploadDir).resolve(filePath.substring(1)); // Remove leading /
        if (Files.exists(fileToDelete)) {
            Files.delete(fileToDelete);
        }
    }
}
