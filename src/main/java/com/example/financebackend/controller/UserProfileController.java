package com.example.financebackend.controller;

import com.example.financebackend.dto.AuditLogDto;
import com.example.financebackend.dto.ChangePasswordRequest;
import com.example.financebackend.dto.DeleteAccountRequest;
import com.example.financebackend.dto.UpdateProfileRequest;
import com.example.financebackend.dto.UserDto;
import com.example.financebackend.dto.UserPreferencesDto;
import com.example.financebackend.entity.AuditLog;
import com.example.financebackend.service.AuthService;
import com.example.financebackend.service.AuditLogService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final AuthService authService;
    private final AuditLogService auditLogService;

    public UserProfileController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/profile")
    public UserDto getProfile() {
        Long userId = AuthUtil.getCurrentUserId();
        return authService.getProfile(userId);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public UserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return authService.updateProfile(userId, request);
    }

    @PutMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        authService.changePassword(userId, request);
    }

    @GetMapping("/preferences")
    public UserPreferencesDto getPreferences() {
        Long userId = AuthUtil.getCurrentUserId();
        return authService.getPreferences(userId);
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public UserPreferencesDto updatePreferences(@Valid @RequestBody UserPreferencesDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return authService.updatePreferences(userId, dto);
    }

    /**
     * Delete user account permanently
     * Requires password confirmation
     */
    @DeleteMapping("/account")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        authService.deleteAccount(userId, request.getPassword());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Tài khoản đã được xóa thành công");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/login-history")
    public Page<AuditLogDto> getLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = AuthUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Filter by LOGIN action only
        Page<AuditLogDto> allLogs = auditLogService.getUserLogs(userId, pageable);
        // Filter to only LOGIN actions
        return new org.springframework.data.domain.PageImpl<>(
                allLogs.getContent().stream()
                        .filter(log -> log.getAction() == AuditLog.Action.LOGIN)
                        .collect(java.util.stream.Collectors.toList()),
                pageable,
                allLogs.getContent().stream()
                        .filter(log -> log.getAction() == AuditLog.Action.LOGIN)
                        .count()
        );
    }
}

