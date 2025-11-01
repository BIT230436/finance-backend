package com.example.financebackend.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller for OAuth2 endpoints when OAuth2 is not configured.
 * This prevents 404 errors and provides helpful error messages.
 * 
 * Only loaded when OAuth2 auto-configuration is excluded (when GOOGLE_CLIENT_ID is not set).
 */
@RestController
@RequestMapping("/oauth2")
@ConditionalOnProperty(
    name = "spring.autoconfigure.exclude",
    havingValue = "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration",
    matchIfMissing = false
)
public class OAuth2FallbackController {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @GetMapping("/authorization/google")
    public ResponseEntity<Map<String, Object>> handleOAuth2NotConfigured() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("errorCode", "OAUTH2_NOT_CONFIGURED");
        
        String clientIdEnv = System.getenv("GOOGLE_CLIENT_ID");
        if (!StringUtils.hasText(clientIdEnv)) {
            response.put("message", "OAuth2 chưa được cấu hình. Vui lòng set GOOGLE_CLIENT_ID và GOOGLE_CLIENT_SECRET environment variables.");
            response.put("hint", "Chạy SETUP_OAUTH2.ps1 (PowerShell) hoặc SETUP_OAUTH2.bat (CMD) để set environment variables, sau đó restart application.");
        } else {
            response.put("message", "OAuth2 environment variables đã được set nhưng application chưa được restart. Vui lòng restart application để OAuth2 hoạt động.");
            response.put("hint", "Restart application từ terminal đã set environment variables, hoặc set environment variables trong IDE run configuration.");
        }
        
        response.put("path", "/oauth2/authorization/google");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}

