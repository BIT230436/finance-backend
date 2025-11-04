package com.example.financebackend.controller;

import com.example.financebackend.dto.ChangePasswordRequest;
import com.example.financebackend.dto.DeleteAccountRequest;
import com.example.financebackend.dto.UpdateProfileRequest;
import com.example.financebackend.dto.UserPreferencesDto;
import com.example.financebackend.dto.UserDto;
// THÊM IMPORT NÀY
import com.example.financebackend.util.AuthUtil;
// TẠO SERVICE MỚI

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    // SỬ DỤNG SERVICE MỚI
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<UserDto> getProfile() {
        // SỬA LOGIC: Lấy userId từ context
        try {
            Long userId = AuthUtil.getCurrentUserId();
            UserDto userDto = userProfileService.getProfile(userId);
            return ResponseEntity.ok(userDto);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        // SỬA LOGIC: Lấy userId từ context
        try {
            Long userId = AuthUtil.getCurrentUserId();
            UserDto updatedUser = userProfileService.updateProfile(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // SỬA LOGIC: Lấy userId từ context
        try {
            Long userId = AuthUtil.getCurrentUserId();
            userProfileService.changePassword(userId, request);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesDto> getPreferences() {
        // SỬA LOGIC: Lấy userId từ context
        try {
            Long userId = AuthUtil.getCurrentUserId();
            UserPreferencesDto preferences = userProfileService.getPreferences(userId);
            return ResponseEntity.ok(preferences);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesDto> updatePreferences(@Valid @RequestBody UserPreferencesDto preferences) {
        // SỬA LOGIC: Lấy userId từ context
        try {
            Long userId = AuthUtil.getCurrentUserId();
            UserPreferencesDto updatedPreferences = userProfileService.updatePreferences(userId, preferences);
            return ResponseEntity.ok(updatedPreferences);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        // SỬA LOGIC: Lấy userId từ context
        try {
            Long userId = AuthUtil.getCurrentUserId();
            userProfileService.deleteAccount(userId, request.getPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
