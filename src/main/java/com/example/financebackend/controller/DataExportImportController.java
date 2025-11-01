package com.example.financebackend.controller;

import com.example.financebackend.dto.UserDataExportDto;
import com.example.financebackend.service.DataExportImportService;
import com.example.financebackend.util.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class DataExportImportController {

    private final DataExportImportService dataExportImportService;

    public DataExportImportController(DataExportImportService dataExportImportService) {
        this.dataExportImportService = dataExportImportService;
    }

    @GetMapping("/export-data")
    public UserDataExportDto exportData() {
        Long userId = AuthUtil.getCurrentUserId();
        return dataExportImportService.exportUserData(userId);
    }

    @PostMapping("/import-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void importData(@RequestBody UserDataExportDto importData) {
        Long userId = AuthUtil.getCurrentUserId();
        dataExportImportService.importUserData(importData, userId);
    }
}

