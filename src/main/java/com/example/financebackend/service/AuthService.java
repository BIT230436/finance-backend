package com.example.financebackend.service;

import com.example.financebackend.dto.AuthResponse;
import com.example.financebackend.dto.ChangePasswordRequest;
import com.example.financebackend.dto.LoginRequest;
import com.example.financebackend.dto.PasswordResetConfirmRequest;
import com.example.financebackend.dto.PasswordResetRequest;
import com.example.financebackend.dto.RegisterRequest;
import com.example.financebackend.dto.UpdateProfileRequest;
import com.example.financebackend.dto.UserDto;
import com.example.financebackend.dto.UserPreferencesDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.PasswordResetToken;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.PasswordResetTokenRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import com.example.financebackend.util.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import com.example.financebackend.util.TotpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TotpUtil totpUtil;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final EmailService emailService;
    private NotificationService notificationService; // Lazy init to avoid circular dependency

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                      PasswordResetTokenRepository passwordResetTokenRepository, TotpUtil totpUtil,
                      CategoryRepository categoryRepository, WalletRepository walletRepository,
                      EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.totpUtil = totpUtil;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.emailService = emailService;
    }

    // Setter injection with @Lazy to break circular dependency
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        
        // Tự động tạo default categories và wallet cho user mới
        createDefaultCategories(saved);
        createDefaultWallet(saved);
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", saved.getEmail(), e);
            // Don't fail registration if email fails
        }

        // Create welcome notification
        try {
            if (notificationService != null) {
                notificationService.createWelcomeNotification(saved);
            }
        } catch (Exception e) {
            logger.error("Failed to create welcome notification for: {}", saved.getEmail(), e);
            // Don't fail registration if notification fails
        }
        
        logger.info("User registered successfully: userId={}, email={}", saved.getId(), saved.getEmail());
        return generateAuthResponse(saved);
    }
    
    /**
     * Tạo các danh mục mặc định cho user mới
     */
    private void createDefaultCategories(User user) {
        List<CategoryData> defaultCategories = Arrays.asList(
            // Income categories
            new CategoryData("Lương", Category.CategoryType.INCOME, "#4CAF50"),
            new CategoryData("Thưởng", Category.CategoryType.INCOME, "#8BC34A"),
            new CategoryData("Đầu tư", Category.CategoryType.INCOME, "#CDDC39"),
            new CategoryData("Kinh doanh", Category.CategoryType.INCOME, "#FFEB3B"),
            new CategoryData("Khác", Category.CategoryType.INCOME, "#9E9E9E"),
            
            // Expense categories
            new CategoryData("Ăn uống", Category.CategoryType.EXPENSE, "#FF5722"),
            new CategoryData("Mua sắm", Category.CategoryType.EXPENSE, "#E91E63"),
            new CategoryData("Di chuyển", Category.CategoryType.EXPENSE, "#3F51B5"),
            new CategoryData("Giải trí", Category.CategoryType.EXPENSE, "#9C27B0"),
            new CategoryData("Y tế", Category.CategoryType.EXPENSE, "#F44336"),
            new CategoryData("Giáo dục", Category.CategoryType.EXPENSE, "#2196F3"),
            new CategoryData("Hóa đơn", Category.CategoryType.EXPENSE, "#FF9800"),
            new CategoryData("Nhà ở", Category.CategoryType.EXPENSE, "#795548"),
            new CategoryData("Gia đình", Category.CategoryType.EXPENSE, "#FFC107"),
            new CategoryData("Khác", Category.CategoryType.EXPENSE, "#607D8B")
        );
        
        for (CategoryData catData : defaultCategories) {
            Category category = new Category();
            category.setUser(user);
            category.setName(catData.name);
            category.setType(catData.type);
            category.setColor(catData.color);
            categoryRepository.save(category);
        }
        
        logger.debug("Created {} default categories for user {}", defaultCategories.size(), user.getId());
    }
    
    /**
     * Tạo ví mặc định cho user mới
     */
    private void createDefaultWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setName("Ví tiền mặt");
        wallet.setType(Wallet.WalletType.CASH);
        wallet.setCurrency(user.getDefaultCurrency() != null ? user.getDefaultCurrency() : "VND");
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setDefault(true);
        
        Wallet saved = walletRepository.save(wallet);
        logger.debug("Created default wallet for user {}: walletId={}", user.getId(), saved.getId());
    }
    
    /**
     * Helper class cho default categories
     */
    private static class CategoryData {
        final String name;
        final Category.CategoryType type;
        final String color;
        
        CategoryData(String name, Category.CategoryType type, String color) {
            this.name = name;
            this.type = type;
            this.color = color;
        }
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (!user.getEnabled()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        return generateAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtUtil.extractUsername(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);
        Long tokenVersion = jwtUtil.extractTokenVersion(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (!user.getEnabled()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
        }

        // Check if token version matches (to detect if user logged out from all devices)
        Long userTokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
        if (!tokenVersion.equals(userTokenVersion)) {
            throw new IllegalArgumentException("Token đã bị vô hiệu hóa. Vui lòng đăng nhập lại");
        }

        return generateAuthResponse(user);
    }

    public AuthResponse generateAuthResponse(User user) {
        Long tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), tokenVersion);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail(), tokenVersion);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());

        return response;
    }

    public void logoutAllDevices(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        
        // Password is optional - if provided, validate it; if not, allow logout since user is already authenticated via JWT
        if (password != null && !password.trim().isEmpty()) {
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu không đúng");
            }
        }
        
        // Increment token version to invalidate all existing tokens
        Long newVersion = user.getTokenVersion() != null ? user.getTokenVersion() + 1 : 1L;
        user.setTokenVersion(newVersion);
        userRepository.save(user);
        
        logger.info("User {} logged out from all devices. New token version: {}", userId, newVersion);
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        // Find user - don't reveal if email exists or not for security
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        
        if (userOptional.isEmpty()) {
            // For security: don't reveal that email doesn't exist
            // Just log and return silently
            logger.info("Password reset requested for non-existent email: {}", request.getEmail());
            return;
        }
        
        User user = userOptional.get();

        // Invalidate old unused tokens for this user
        passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.getUsed())
                .forEach(token -> {
                    token.setUsed(true);
                    passwordResetTokenRepository.save(token);
                });

        // Generate new token
        String token = generateSecureToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        passwordResetTokenRepository.save(resetToken);

        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
            logger.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", user.getEmail(), e);
            // Still return success for security (don't reveal if email exists)
        }
    }

    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        if (token.isExpired()) {
            throw new IllegalArgumentException("Token đặt lại mật khẩu đã hết hạn");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Map<String, String> enable2FA(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        String secret = totpUtil.generateSecret();
        String qrCodeUrl = totpUtil.generateQrCodeUrl(user.getEmail(), secret);
        user.setTwoFactorSecret(secret);
        userRepository.save(user);
        
        Map<String, String> result = new HashMap<>();
        result.put("secret", secret);
        result.put("qrCodeUrl", qrCodeUrl);
        return result;
    }

    public void verify2FASetup(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (user.getTwoFactorSecret() == null) {
            throw new IllegalArgumentException("Xác thực 2 yếu tố chưa được thiết lập");
        }
        if (!totpUtil.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new IllegalArgumentException("Mã xác thực 2 yếu tố không hợp lệ");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    public boolean verify2FA(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (!user.getTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            return false;
        }
        return totpUtil.verifyCode(user.getTwoFactorSecret(), code);
    }

    public void disable2FA(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không đúng");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
    }

    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return toDto(user);
    }

    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        
        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().toLowerCase().trim();
            
            // Check if email is changed and already exists
            if (!user.getEmail().equalsIgnoreCase(newEmail) 
                    && userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
            
            user.setEmail(newEmail);
        }
        
        // Update fullName if provided
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            String fullName = request.getFullName().trim();
            
            if (fullName.length() < 2) {
                throw new IllegalArgumentException("Họ tên phải có ít nhất 2 ký tự");
            }
            if (fullName.length() > 100) {
                throw new IllegalArgumentException("Họ tên không được vượt quá 100 ký tự");
            }
            
            user.setFullName(fullName);
        }
        
        User saved = userRepository.save(user);
        return toDto(saved);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserPreferencesDto getPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        UserPreferencesDto dto = new UserPreferencesDto();
        dto.setDefaultCurrency(user.getDefaultCurrency() != null ? user.getDefaultCurrency() : "VND");
        dto.setCurrencyFormat(user.getCurrencyFormat() != null ? user.getCurrencyFormat() : "dot");
        dto.setDateFormat(user.getDateFormat() != null ? user.getDateFormat() : "dd/MM/yyyy");
        dto.setLanguage(user.getLanguage() != null ? user.getLanguage() : "vi");
        return dto;
    }

    public UserPreferencesDto updatePreferences(Long userId, UserPreferencesDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        
        if (dto.getDefaultCurrency() != null) {
            user.setDefaultCurrency(dto.getDefaultCurrency());
        }
        if (dto.getCurrencyFormat() != null) {
            user.setCurrencyFormat(dto.getCurrencyFormat());
        }
        if (dto.getDateFormat() != null) {
            user.setDateFormat(dto.getDateFormat());
        }
        if (dto.getLanguage() != null) {
            user.setLanguage(dto.getLanguage());
        }
        
        User saved = userRepository.save(user);
        UserPreferencesDto response = new UserPreferencesDto();
        response.setDefaultCurrency(saved.getDefaultCurrency());
        response.setCurrencyFormat(saved.getCurrencyFormat());
        response.setDateFormat(saved.getDateFormat());
        response.setLanguage(saved.getLanguage());
        return response;
    }

    /**
     * Delete user account permanently
     * Deletes all related data: wallets, transactions, budgets, goals, etc.
     */
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không đúng");
        }
        
        logger.warn("DELETING USER ACCOUNT: userId={}, email={}", userId, user.getEmail());
        
        // Delete user - CASCADE will delete related data
        userRepository.delete(user);
        
        logger.info("User account deleted successfully: userId={}", userId);
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.getEnabled());
        dto.setTwoFactorEnabled(user.getTwoFactorEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}