package com.example.financebackend.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication success handler for OAuth2 login.
 * Ensures proper redirect to success endpoint and preserves session.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    public OAuth2AuthenticationSuccessHandler() {
        super("/api/oauth2/success");
        // Always use default success URL to prevent redirect loops
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        logger.info("OAuth2 authentication successful!");
        String authType = authentication != null ? authentication.getClass().getSimpleName() : "null";
        logger.info("Authentication type: {}", authType);
        String principalType = authentication != null && authentication.getPrincipal() != null ? 
                   authentication.getPrincipal().getClass().getSimpleName() : "null";
        logger.info("Principal type: {}", principalType);
        
        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String email = (String) oauth2User.getAttribute("email");
            logger.info("OAuth2User email: {}", email);
        }
        
        // Ensure session exists and is preserved
        jakarta.servlet.http.HttpSession session = request.getSession(true);
        String sessionId = session.getId();
        logger.info("Request session ID: {}", sessionId);
        logger.info("Session is new: {}", session.isNew());
        
        // Set session attributes to ensure session is preserved
        session.setAttribute("OAUTH2_AUTHENTICATION", authentication);
        session.setMaxInactiveInterval(30 * 60); // 30 minutes
        
        logger.info("Session attributes set. Redirecting to: /api/oauth2/success");
        
        // Call parent to handle redirect
        super.onAuthenticationSuccess(request, response, authentication);
    }
}

