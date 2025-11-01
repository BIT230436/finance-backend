package com.example.financebackend.controller;

import com.example.financebackend.dto.AuthResponse;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import com.example.financebackend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);
    
    private final UserRepository userRepository;
    private final AuthService authService;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${frontend.oauth2.success.path:/auth/callback}")
    private String frontendSuccessPath;
    
    @Value("${frontend.oauth2.error.path:/auth/error}")
    private String frontendErrorPath;

    public OAuth2Controller(UserRepository userRepository, AuthService authService,
                           CategoryRepository categoryRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
    }

    @GetMapping("/success")
    public RedirectView handleGoogleOAuthSuccess(@AuthenticationPrincipal OAuth2User oauth2User,
                                                  jakarta.servlet.http.HttpServletRequest request) {
        logger.info("OAuth2 success callback received. OAuth2User: {}", oauth2User != null ? "present" : "null");
        
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        logger.info("Request session ID: {}", session != null ? session.getId() : "no session");
        logger.info("Session exists: {}", session != null);
        
        // If OAuth2User is null, try to get it from session
        if (oauth2User == null && session != null) {
            logger.warn("OAuth2User is null from @AuthenticationPrincipal, trying to get from session");
            
            // Try to get authentication from session
            Object authFromSession = session.getAttribute("OAUTH2_AUTHENTICATION");
            if (authFromSession instanceof org.springframework.security.core.Authentication) {
                org.springframework.security.core.Authentication auth = 
                    (org.springframework.security.core.Authentication) authFromSession;
                if (auth.getPrincipal() instanceof OAuth2User) {
                    oauth2User = (OAuth2User) auth.getPrincipal();
                    logger.info("Retrieved OAuth2User from session");
                }
            }
            
            // Try to get from SecurityContext in session
            if (oauth2User == null) {
                Object securityContext = session.getAttribute("SPRING_SECURITY_CONTEXT");
                logger.info("SecurityContext from session: {}", securityContext != null ? "exists" : "null");
                
                if (securityContext instanceof org.springframework.security.core.context.SecurityContext) {
                    org.springframework.security.core.context.SecurityContext ctx = 
                        (org.springframework.security.core.context.SecurityContext) securityContext;
                    org.springframework.security.core.Authentication auth = ctx.getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof OAuth2User) {
                        oauth2User = (OAuth2User) auth.getPrincipal();
                        logger.info("Retrieved OAuth2User from SecurityContext in session");
                    }
                }
            }
        }
        
        if (oauth2User == null) {
            logger.error("OAuth2User is null in success handler - this should not happen if OAuth2 flow completed successfully");
            logger.error("Possible causes: 1) Session expired or cleared, 2) OAuth2 flow interrupted, 3) Browser cleared cookies during flow");
            logger.error("Session exists: {}", session != null);
            
            return redirectToFrontendError("OAuth2 user không tồn tại. Vui lòng thử đăng nhập lại.");
        }
        
        String rawEmail = oauth2User.getAttribute("email");
        if (rawEmail == null || rawEmail.trim().isEmpty()) {
            logger.error("Cannot get email from OAuth2 provider");
            return redirectToFrontendError("Không thể lấy email từ OAuth2 provider");
        }
        final String email = rawEmail.toLowerCase().trim();
        
        String name = oauth2User.getAttribute("name");
        final String finalName = (name == null || name.trim().isEmpty()) ? email : name;

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            logger.info("Creating new OAuth2 user: email={}, name={}", email, finalName);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(finalName);
            newUser.setPasswordHash(""); // OAuth users don't have password
            newUser.setRole(User.Role.USER);
            newUser.setEnabled(true);
            User saved = userRepository.save(newUser);
            
            // Tự động tạo default categories và wallet cho OAuth user mới
            createDefaultCategories(saved);
            createDefaultWallet(saved);
            
            logger.info("OAuth2 user created successfully: userId={}, email={}", saved.getId(), email);
            return saved;
        });

        // Use AuthService to generate tokens with tokenVersion
        logger.info("OAuth2 login successful: userId={}, email={}", user.getId(), email);
        AuthResponse authResponse = authService.generateAuthResponse(user);
        
        // Redirect to frontend with tokens in query parameters
        String redirectUrl = buildFrontendSuccessUrl(authResponse);
        logger.info("Redirecting to frontend: {}", redirectUrl);
        return new RedirectView(redirectUrl);
    }
    
    /**
     * Build frontend success URL with tokens
     */
    private String buildFrontendSuccessUrl(AuthResponse authResponse) {
        try {
            StringBuilder url = new StringBuilder();
            url.append(frontendUrl);
            if (!frontendSuccessPath.startsWith("/")) {
                url.append("/");
            }
            url.append(frontendSuccessPath);
            
            // Add tokens as query parameters
            url.append("?accessToken=").append(URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8));
            url.append("&refreshToken=").append(URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8));
            url.append("&tokenType=").append(URLEncoder.encode(authResponse.getTokenType(), StandardCharsets.UTF_8));
            url.append("&userId=").append(authResponse.getUserId());
            url.append("&email=").append(URLEncoder.encode(authResponse.getEmail(), StandardCharsets.UTF_8));
            url.append("&fullName=").append(URLEncoder.encode(authResponse.getFullName(), StandardCharsets.UTF_8));
            url.append("&role=").append(URLEncoder.encode(authResponse.getRole(), StandardCharsets.UTF_8));
            
            return url.toString();
        } catch (Exception e) {
            logger.error("Error building frontend success URL", e);
            return frontendUrl + frontendErrorPath + "?error=REDIRECT_ERROR&message=" + 
                   URLEncoder.encode("Lỗi redirect đến frontend", StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Redirect to frontend error page
     */
    private RedirectView redirectToFrontendError(String errorMessage) {
        try {
            String errorUrl = frontendUrl;
            if (!frontendErrorPath.startsWith("/")) {
                errorUrl += "/";
            }
            errorUrl += frontendErrorPath;
            errorUrl += "?error=OAUTH2_LOGIN_FAILED&message=" + 
                       URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            
            logger.warn("Redirecting to frontend error page: {}", errorUrl);
            return new RedirectView(errorUrl);
        } catch (Exception e) {
            logger.error("Error redirecting to frontend error page", e);
            // Fallback: redirect to frontend base URL
            return new RedirectView(frontendUrl + frontendErrorPath);
        }
    }
    
    @GetMapping("/error")
    public RedirectView handleOAuth2Error(@RequestParam(required = false) String error,
                                         @RequestParam(required = false) String error_description) {
        logger.warn("OAuth2 login failed or error occurred");
        logger.warn("Error parameter: {}", error);
        logger.warn("Error description: {}", error_description);
        
        // Provide more specific error message based on error type
        String message = "Đăng nhập bằng Google thất bại. Vui lòng thử lại.";
        if (error != null) {
            switch (error) {
                case "access_denied":
                    message = "Bạn đã từ chối đăng nhập bằng Google.";
                    break;
                case "invalid_request":
                    message = "Yêu cầu không hợp lệ. Vui lòng kiểm tra cấu hình OAuth2.";
                    break;
                case "invalid_token_response":
                    message = "Lỗi xác thực với Google. Vui lòng kiểm tra Client ID và Client Secret.";
                    break;
                default:
                    message = "Đăng nhập bằng Google thất bại: " + error;
            }
        }
        
        // Redirect to frontend error page
        try {
            String errorUrl = frontendUrl;
            if (!frontendErrorPath.startsWith("/")) {
                errorUrl += "/";
            }
            errorUrl += frontendErrorPath;
            errorUrl += "?error=" + URLEncoder.encode(error != null ? error : "OAUTH2_LOGIN_FAILED", StandardCharsets.UTF_8);
            errorUrl += "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
            if (error_description != null) {
                errorUrl += "&errorDescription=" + URLEncoder.encode(error_description, StandardCharsets.UTF_8);
            }
            
            logger.warn("Redirecting to frontend error page: {}", errorUrl);
            return new RedirectView(errorUrl);
        } catch (Exception e) {
            logger.error("Error redirecting to frontend error page", e);
            // Fallback: redirect to frontend base URL
            return new RedirectView(frontendUrl + frontendErrorPath);
        }
    }

    /**
     * Tạo các danh mục mặc định cho OAuth user mới
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
        
        logger.debug("Created {} default categories for OAuth user {}", defaultCategories.size(), user.getId());
    }

    /**
     * Tạo wallet mặc định cho OAuth user mới
     */
    private void createDefaultWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setName("Ví tiền mặt");
        wallet.setType(Wallet.WalletType.CASH);
        wallet.setCurrency(user.getDefaultCurrency() != null ? user.getDefaultCurrency() : "VND");
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setDefault(true);
        walletRepository.save(wallet);
        
        logger.debug("Created default wallet for OAuth user {}", user.getId());
    }

    private static class CategoryData {
        String name;
        Category.CategoryType type;
        String color;

        CategoryData(String name, Category.CategoryType type, String color) {
            this.name = name;
            this.type = type;
            this.color = color;
        }
    }
}
