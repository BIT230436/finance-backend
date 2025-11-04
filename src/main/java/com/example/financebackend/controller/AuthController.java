package com.example.financebackend.controller;

import com.example.financebackend.dto.*;
import com.example.financebackend.service.AuthService;
// Import 2 service mới
import com.example.financebackend.service.CaptchaService;
import com.example.financebackend.service.VerificationTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    // Thêm 2 service
    private final CaptchaService captchaService;
    private final VerificationTokenService verificationTokenService;

    public AuthController(AuthService authService,
                          CaptchaService captchaService,
                          VerificationTokenService verificationTokenService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.verificationTokenService = verificationTokenService;
    }

    /**
     * CẬP NHẬT: Bước 1 của Đăng ký - Chỉ gửi mã OTP
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> requestRegistration(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.requestRegistration(registerRequest);
            return ResponseEntity.ok(Map.of("message", "Mã xác thực đã được gửi đến email của bạn."));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * MỚI: Bước 2 của Đăng ký - Xác thực mã OTP và tạo tài khoản
     */
    @PostMapping("/register/verify")
    public ResponseEntity<AuthResponse> verifyAndRegister(@RequestBody VerifyRegistrationRequest verifyRequest) {
        try {
            AuthResponse authResponse = authService.verifyAndRegister(verifyRequest);
            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi xác thực đăng ký.");
        }
    }

    /**
     * CẬP NHẬT: Thêm xác thực reCAPTCHA
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // BƯỚC 1: Xác thực Captcha
            captchaService.validateCaptcha(loginRequest.getCaptchaToken());

            // BƯỚC 2: Tiến hành đăng nhập (logic cũ)
            AuthResponse authResponse = authService.login(loginRequest, request);
            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            // Check for REQUIRE_2FA exception
            if ("REQUIRE_2FA".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse(null, null, null, null, null, null, "REQUIRE_2FA"));
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi đăng nhập.");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        // ... (Giữ nguyên logic cũ)
        String refreshToken = request.get("refreshToken");
        try {
            AuthResponse authResponse = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    /**
     * CẬP NHẬT: Bước 1 Quên mật khẩu - Gửi mã OTP
     */
    @PostMapping("/password/request-reset")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        try {
            authService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Mã khôi phục đã được gửi đến email của bạn."));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi gửi email khôi phục.");
        }
    }

    /**
     * CẬP NHẬT: Bước 2 Quên mật khẩu - Xác thực bằng mã OTP
     */
    @PostMapping("/password/confirm-reset")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        try {
            authService.confirmPasswordReset(request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công."));
        } catch (BadCredentialsException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi đặt lại mật khẩu.");
        }
    }

    // --- (Các endpoint 2FA giữ nguyên) ---

    @PostMapping("/2fa/generate")
    public ResponseEntity<Map<String, String>> generate2FASecret(HttpServletRequest request) {
        // ... (Giữ nguyên logic cũ)
        Long userId = (Long) request.getAttribute("userId");
        String qrCodeUri = authService.generate2FASecret(userId);
        return ResponseEntity.ok(Map.of("qrCodeUri", qrCodeUri));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Map<String, String>> enable2FA(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        // ... (Giữ nguyên logic cũ)
        Long userId = (Long) request.getAttribute("userId");
        String code = requestBody.get("code");
        try {
            authService.enable2FA(userId, code);
            return ResponseEntity.ok(Map.of("message", "Kích hoạt 2FA thành công"));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, String>> disable2FA(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        // ... (Giữ nguyên logic cũ)
        Long userId = (Long) request.getAttribute("userId");
        String password = requestBody.get("password");
        try {
            authService.disable2FA(userId, password);
            return ResponseEntity.ok(Map.of("message", "Vô hiệu hóa 2FA thành công"));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
