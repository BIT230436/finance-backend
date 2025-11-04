package com.example.financebackend.service;

import com.example.financebackend.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Objects;

@Service
public class VerificationTokenService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationTokenService.class);
    private final Cache otpCache;
    private final EmailService emailService;

    public VerificationTokenService(CacheManager cacheManager, EmailService emailService) {
        this.otpCache = Objects.requireNonNull(cacheManager.getCache(CacheConfig.OTP_CACHE));
        this.emailService = emailService;
    }

    /**
     * Tạo, lưu trữ và gửi mã OTP
     * @param email Email của người dùng
     * @param type Mục đích (Đăng ký / Đặt lại mật khẩu)
     * @return Mã OTP đã tạo
     */
    public String createAndSendOtp(String email, VerificationType type) {
        String otp = generateOtp();
        String cacheKey = getCacheKey(email, type);

        // Lưu mã OTP vào cache
        otpCache.put(cacheKey, otp);

        logger.info("Đã tạo và lưu OTP cho {} (loại: {}): {}", email, type, otp);

        // Gửi email
        try {
            if (type == VerificationType.REGISTRATION) {
                emailService.sendVerificationCodeEmail(email, otp, "Xác nhận đăng ký tài khoản");
            } else if (type == VerificationType.PASSWORD_RESET) {
                emailService.sendVerificationCodeEmail(email, otp, "Mã khôi phục mật khẩu");
            }
        } catch (Exception e) {
            logger.error("Không thể gửi email OTP đến {}: {}", email, e.getMessage());
            // Xóa mã OTP khỏi cache nếu gửi mail lỗi để tránh kẹt
            otpCache.evict(cacheKey);
            throw new RuntimeException("Không thể gửi email xác thực. Vui lòng thử lại.");
        }

        return otp;
    }

    /**
     * Xác thực mã OTP
     * @param email Email
     * @param code Mã OTP người dùng nhập
     * @param type Mục đích
     * @return true nếu hợp lệ, false nếu sai
     */
    public boolean validateOtp(String email, String code, VerificationType type) {
        String cacheKey = getCacheKey(email, type);
        String storedOtp = otpCache.get(cacheKey, String.class);

        if (storedOtp == null) {
            logger.warn("Không tìm thấy OTP trong cache cho {} (loại: {}). Mã có thể đã hết hạn.", email, type);
            return false; // Hết hạn hoặc không tồn tại
        }

        if (storedOtp.equals(code)) {
            logger.info("Xác thực OTP thành công cho {} (loại: {})", email, type);
            // Xóa mã OTP sau khi sử dụng thành công
            otpCache.evict(cacheKey);
            return true;
        }

        logger.warn("Xác thực OTP thất bại cho {} (loại: {}). Mã nhập: {}, Mã lưu: {}", email, type, code, storedOtp);
        return false;
    }

    // Tạo mã OTP 6 chữ số
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(1000000); // 0-999999
        return new DecimalFormat("000000").format(num); // Đảm bảo đủ 6 chữ số
    }

    // Tạo key lưu cache duy nhất
    private String getCacheKey(String email, VerificationType type) {
        return type.name() + "_" + email;
    }
}
