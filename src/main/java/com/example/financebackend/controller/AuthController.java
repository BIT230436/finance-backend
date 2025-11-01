package com.example.financebackend.controller;

import com.example.financebackend.dto.AuthResponse;
import com.example.financebackend.dto.LoginRequest;
import com.example.financebackend.dto.PasswordResetConfirmRequest;
import com.example.financebackend.dto.PasswordResetRequest;
import com.example.financebackend.dto.RegisterRequest;
import com.example.financebackend.dto.UserDto;
import com.example.financebackend.service.AuthService;
import com.example.financebackend.service.PermissionService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PermissionService permissionService;

    public AuthController(AuthService authService, PermissionService permissionService) {
        this.authService = authService;
        this.permissionService = permissionService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refreshToken(@RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken());
    }

    @PostMapping("/password/reset")
    public Map<String, String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Nếu email tồn tại, liên kết đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra email của bạn.");
        return response;
    }

    @PostMapping("/password/reset/confirm")
    @ResponseStatus(HttpStatus.OK)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
    }

    @GetMapping("/2fa/status")
    public TwoFactorStatusResponse get2FAStatus() {
        Long userId = AuthUtil.getCurrentUserId();
        UserDto user = authService.getProfile(userId);
        TwoFactorStatusResponse response = new TwoFactorStatusResponse();
        response.setEnabled(user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled());
        return response;
    }

    @PostMapping("/2fa/enable")
    public Map<String, Object> enable2FA() {
        Long userId = AuthUtil.getCurrentUserId();
        Map<String, String> result = authService.enable2FA(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("secret", result.get("secret"));
        response.put("qrCodeUrl", result.get("qrCodeUrl"));
        response.put("enabled", false); // Not fully enabled until verified
        response.put("message", "Scan QR code với Google Authenticator hoặc nhập secret key thủ công");
        
        return response;
    }

    @PostMapping("/2fa/verify")
    @ResponseStatus(HttpStatus.OK)
    public void verify2FASetup(@RequestBody TwoFactorVerifyRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        authService.verify2FASetup(userId, request.getCode());
    }

    @DeleteMapping("/2fa/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable2FA(@RequestBody Disable2FARequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        authService.disable2FA(userId, request.getPassword());
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAllDevices(@RequestBody(required = false) LogoutAllRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        String password = (request != null && request.getPassword() != null) ? request.getPassword() : null;
        authService.logoutAllDevices(userId, password);
    }

    /**
     * Get permissions for current user
     * Frontend can use this to determine what UI elements to show/hide
     */
    @GetMapping("/permissions")
    public Map<String, Object> getPermissions() {
        Long userId = AuthUtil.getCurrentUserId();
        return permissionService.getPermissions(userId);
    }

    /**
     * Check if current user has a specific permission
     * Example: GET /api/auth/permissions/check?feature=canCreateTransactions
     */
    @GetMapping("/permissions/check")
    public Map<String, Object> checkPermission(@RequestParam String feature) {
        Long userId = AuthUtil.getCurrentUserId();
        Map<String, Object> permissions = permissionService.getPermissions(userId);
        Map<String, Boolean> features = (Map<String, Boolean>) permissions.get("features");
        
        Map<String, Object> result = new HashMap<>();
        result.put("feature", feature);
        result.put("allowed", features != null && Boolean.TRUE.equals(features.get(feature)));
        result.put("role", permissions.get("role"));
        return result;
    }

    public static class LogoutAllRequest {
        private Long userId;
        private String password;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Disable2FARequest {
        private Long userId;
        private String password;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class TwoFactorVerifyRequest {
        private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class TwoFactorStatusResponse {
        private Boolean enabled;
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static class TwoFactorSetupResponse {
        private String secret;
        private Boolean success;
        public TwoFactorSetupResponse(String secret, Boolean success) {
            this.secret = secret;
            this.success = success;
        }
        public String getSecret() { return secret; }
        public Boolean getSuccess() { return success; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}