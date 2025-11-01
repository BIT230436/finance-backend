package com.example.financebackend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthUtil.class);
    
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.error("Authentication is null in SecurityContext");
            throw new IllegalStateException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập lại.");
        }
        
        if (authentication.getPrincipal() == null) {
            logger.error("Principal is null in Authentication");
            throw new IllegalStateException("Không tìm thấy thông tin xác thực. Vui lòng đăng nhập lại.");
        }
        
        // Principal được set trong JwtAuthenticationFilter là userId (Long)
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        
        logger.error("Principal is not Long type. Principal type: {}, Principal value: {}", 
                principal.getClass().getName(), principal);
        throw new IllegalStateException("Không thể lấy userId từ authentication. Vui lòng đăng nhập lại.");
    }
}

