package com.example.financebackend.service;

import com.example.financebackend.config.DataInitializer;
import com.example.financebackend.dto.AuthResponse;
import com.example.financebackend.dto.LoginRequest;
import com.example.financebackend.dto.RegisterRequest;
import com.example.financebackend.dto.VerifyRegistrationRequest;
import com.example.financebackend.entity.PasswordResetToken;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.PasswordResetTokenRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.util.JwtUtil;
import com.example.financebackend.util.TotpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

// Import thêm VerificationTokenService
import com.example.financebackend.service.VerificationTokenService;
import com.example.financebackend.service.VerificationType;

@Service
public class AuthService {

    // ... (Giữ nguyên các khai báo cũ)
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TotpUtil totpUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final DataInitializer dataInitializer;
    private final AuditLogService auditLogService;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Thêm VerificationTokenService
    private final VerificationTokenService verificationTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, TotpUtil totpUtil,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService, DataInitializer dataInitializer,
                       AuditLogService auditLogService,
                       VerificationTokenService verificationTokenService) { // Thêm vào constructor
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.totpUtil = totpUtil;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.dataInitializer = dataInitializer;
        this.auditLogService = auditLogService;
        this.verificationTokenService = verificationTokenService; // Gán
    }

    /**
     * BƯỚC 1: Yêu cầu đăng ký (Gửi mã OTP)
     */
    @Transactional
    public void requestRegistration(RegisterRequest registerRequest) {
        logger.info("Yêu cầu đăng ký cho email: {}", registerRequest.getEmail());
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalStateException("Email đã tồn tại");
        }
        // Gửi mã OTP
        verificationTokenService.createAndSendOtp(registerRequest.getEmail(), VerificationType.REGISTRATION);
        logger.info("Đã gửi OTP đăng ký đến: {}", registerRequest.getEmail());
    }

    /**
     * BƯỚC 2: Xác thực đăng ký (Tạo User)
     */
    @Transactional
    public AuthResponse verifyAndRegister(VerifyRegistrationRequest verifyRequest) {
        logger.info("Xác thực đăng ký cho email: {}", verifyRequest.getEmail());

        // Xác thực mã OTP
        boolean isValidOtp = verificationTokenService.validateOtp(
                verifyRequest.getEmail(),
                verifyRequest.getCode(),
                VerificationType.REGISTRATION
        );

        if (!isValidOtp) {
            throw new BadCredentialsException("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }

        // Mã OTP hợp lệ, tiến hành tạo User (Logic từ hàm register cũ)
        if (userRepository.existsByEmail(verifyRequest.getEmail())) {
            // Kiểm tra lại phòng trường hợp race condition
            throw new IllegalStateException("Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(verifyRequest.getEmail());
        user.setPassword(passwordEncoder.encode(verifyRequest.getPassword()));
        user.setFullName(verifyRequest.getFullName());
        user.setRole("USER");
        user.setEnabled(true); // Kích hoạt ngay vì đã xác thực email
        user.setProvider(User.AuthProvider.LOCAL);
        user.setTokenVersion(0L); // Khởi tạo token version

        User savedUser = userRepository.save(user);
        logger.info("Đã tạo user mới thành công, ID: {}", savedUser.getId());

        // Tạo danh mục mặc định
        dataInitializer.createDefaultCategories(savedUser);

        // Gửi email chào mừng
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());
        } catch (Exception e) {
            logger.warn("Gửi email chào mừng thất bại cho {}: {}", savedUser.getEmail(), e.getMessage());
        }

        // Tạo token
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole(), savedUser.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId(), savedUser.getEmail(), savedUser.getTokenVersion());

        return new AuthResponse(accessToken, refreshToken, savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole());
    }

    // Hàm register() CŨ (Giờ không dùng nữa, hoặc chỉ dùng cho BƯỚC 1)
    // @Transactional
    // public void register(RegisterRequest registerRequest) { ... }
    // -> Logic đã được chuyển vào requestRegistration và verifyAndRegister


    // --- (Giữ nguyên hàm login, nhưng logic captcha sẽ được gọi từ Controller) ---
    @Transactional
    public AuthResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        // ... (Giữ nguyên toàn bộ logic login, 2FA... của bạn)
        // Việc xác thực Captcha đã được thực hiện ở Controller
        logger.info("Bắt đầu đăng nhập cho: {}", loginRequest.getEmail());
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Đăng nhập thất bại (không tìm thấy user): {}", loginRequest.getEmail());
                    return new BadCredentialsException("Email hoặc mật khẩu không chính xác");
                });
        // ... (Phần còn lại của hàm login)
        if (!user.getProvider().equals(User.AuthProvider.LOCAL)) {
            logger.warn("Đăng nhập thất bại (sai provider): {}", loginRequest.getEmail());
            throw new BadCredentialsException("Tài khoản này được đăng nhập bằng: " + user.getProvider());
        }

        if (!user.isEnabled()) {
            logger.warn("Đăng nhập thất bại (chưa kích hoạt): {}", loginRequest.getEmail());
            throw new IllegalStateException("Tài khoản chưa được kích hoạt.");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("Đăng nhập thất bại (sai mật khẩu): {}", loginRequest.getEmail());
            auditLogService.log(AuditLog.Action.LOGIN_FAILURE, null, "Auth", null, "Đăng nhập thất bại: " + loginRequest.getEmail(), request.getRemoteAddr(), request.getHeader("User-Agent"));
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }

        // Xử lý 2FA
        if (user.isTotpEnabled()) {
            if (loginRequest.getTotpCode() == null || loginRequest.getTotpCode().isEmpty()) {
                logger.info("Yêu cầu mã 2FA cho: {}", loginRequest.getEmail());
                // Ném một exception đặc biệt để frontend biết cần hiển thị ô nhập 2FA
                throw new BadCredentialsException("REQUIRE_2FA");
            }

            if (!totpUtil.validateCode(user.getTotpSecret(), loginRequest.getTotpCode())) {
                logger.warn("Đăng nhập thất bại (sai mã 2FA): {}", loginRequest.getEmail());
                auditLogService.log(AuditLog.Action.LOGIN_FAILURE, user.getId(), "Auth", user.getId(), "Đăng nhập thất bại (sai mã 2FA)", request.getRemoteAddr(), request.getHeader("User-Agent"));
                throw new BadCredentialsException("Mã 2FA không chính xác");
            }
            logger.info("Xác thực 2FA thành công cho: {}", loginRequest.getEmail());
        }

        // Đăng nhập thành công
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getTokenVersion());

        auditLogService.log(AuditLog.Action.LOGIN_SUCCESS, user.getId(), "Auth", user.getId(), "Đăng nhập thành công", request.getRemoteAddr(), request.getHeader("User-Agent"));
        logger.info("Đăng nhập thành công cho: {}", user.getEmail());

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }


    // --- CẬP NHẬT LUỒNG QUÊN MẬT KHẨU ---

    /**
     * BƯỚC 1: Yêu cầu đặt lại mật khẩu (Gửi OTP)
     */
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản với email này."));

        // Chỉ cho phép reset nếu là tài khoản LOCAL
        if (user.getProvider() != User.AuthProvider.LOCAL) {
            throw new IllegalStateException("Không thể đặt lại mật khẩu cho tài khoản đăng nhập bằng " + user.getProvider());
        }

        // Gửi mã OTP
        verificationTokenService.createAndSendOtp(email, VerificationType.PASSWORD_RESET);
        logger.info("Đã gửi OTP đặt lại mật khẩu đến: {}", email);
    }

    // Hàm requestPasswordReset CŨ (Dùng link/token) -> Không dùng nữa
    // @Transactional
    // public void requestPasswordReset(String email) {
    //     ... (logic tạo token và gửi link)
    // }

    /**
     * BƯỚC 2: Xác nhận đặt lại mật khẩu (Bằng OTP)
     */
    @Transactional
    public void confirmPasswordReset(String email, String code, String newPassword) {
        logger.info("Xác thực đặt lại mật khẩu cho: {}", email);

        // Xác thực mã OTP
        boolean isValidOtp = verificationTokenService.validateOtp(email, code, VerificationType.PASSWORD_RESET);

        if (!isValidOtp) {
            throw new BadCredentialsException("Mã OTP không hợp lệ hoặc đã hết hạn.");
        }

        // Mã OTP hợp lệ, tiến hành đổi mật khẩu
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản.")); // Lỗi này không nên xảy ra

        user.setPassword(passwordEncoder.encode(newPassword));

        // QUAN TRỌNG: Tăng tokenVersion để vô hiệu hóa tất cả Refresh Token cũ
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);
        logger.info("Đặt lại mật khẩu thành công cho: {}", email);

        // Ghi log
        auditLogService.log(AuditLog.Action.PASSWORD_RESET_SUCCESS, user.getId(), "Auth", user.getId(), "Đặt lại mật khẩu thành công", null, null);
    }

    // Hàm confirmPasswordReset CŨ (Dùng link/token) -> Không dùng nữa
    // @Transactional
    // public void confirmPasswordReset(String token, String newPassword) {
    //     ... (logic xác thực token link)
    // }


    // --- (Các hàm khác như refreshToken, enable2FA, ... giữ nguyên) ---

    public AuthResponse refreshToken(String refreshToken) {
        // ... (Giữ nguyên logic cũ)
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtUtil.extractUsername(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);
        Long tokenVersion = jwtUtil.extractTokenVersion(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Không tìm thấy người dùng cho refresh token"));

        // Kiểm tra Token Version
        if (!user.getTokenVersion().equals(tokenVersion)) {
            logger.warn("Refresh token bị vô hiệu hóa (Token Version không khớp) cho user: {}", email);
            throw new BadCredentialsException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getTokenVersion());

        return new AuthResponse(newAccessToken, refreshToken, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    @Transactional
    public AuthResponse processOAuth2PostLogin(String email, String name, String imageUrl) {
        // ... (Giữ nguyên logic cũ)
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider() != User.AuthProvider.GOOGLE) {
                throw new IllegalStateException("Tài khoản đã tồn tại với phương thức đăng nhập khác.");
            }
            // Cập nhật thông tin nếu cần
            user.setFullName(name);
            user.setImageUrl(imageUrl);
            user = userRepository.save(user);
        } else {
            // Tạo user mới
            user = new User();
            user.setEmail(email);
            user.setFullName(name);
            user.setImageUrl(imageUrl);
            user.setProvider(User.AuthProvider.GOOGLE);
            user.setRole("USER");
            user.setEnabled(true);
            user.setTokenVersion(0L);
            user = userRepository.save(user);

            // Tạo danh mục mặc định
            dataInitializer.createDefaultCategories(user);

            // Gửi email chào mừng
            try {
                emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
            } catch (Exception e) {
                logger.warn("Gửi email chào mừng (OAuth2) thất bại cho {}: {}", user.getEmail(), e.getMessage());
            }
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), user.getTokenVersion());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), user.getTokenVersion());

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    // ... (Các hàm 2FA giữ nguyên)
    @Transactional
    public String generate2FASecret(Long userId) {
        // ...
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));

        String secret = totpUtil.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        return totpUtil.getQrCodeUri(secret, user.getEmail());
    }

    @Transactional
    public void enable2FA(Long userId, String code) {
        // ...
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));

        if (user.getTotpSecret() == null) {
            throw new IllegalStateException("Chưa tạo 2FA secret");
        }

        if (!totpUtil.validateCode(user.getTotpSecret(), code)) {
            throw new BadCredentialsException("Mã 2FA không chính xác");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);

        auditLogService.log(AuditLog.Action.MFA_ENABLED, userId, "Auth", userId, "Kích hoạt 2FA thành công", null, null);
    }

    @Transactional
    public void disable2FA(Long userId, String password) {
        // ...
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu không chính xác");
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null); // Xóa secret
        userRepository.save(user);

        auditLogService.log(AuditLog.Action.MFA_DISABLED, userId, "Auth", userId, "Vô hiệu hóa 2FA thành công", null, null);
    }
}
