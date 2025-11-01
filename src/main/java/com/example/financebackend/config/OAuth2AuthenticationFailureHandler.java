package com.example.financebackend.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication failure handler for OAuth2 login.
 * Redirects to /api/oauth2/error instead of default /login?error
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        logger.error("OAuth2 authentication failed: {}", exception.getMessage());
        logger.error("Request URI: {}", request.getRequestURI());
        logger.error("Error type: {}", exception.getClass().getSimpleName());
        logger.error("Full exception: ", exception);
        
        // Log request parameters for debugging
        if (request.getQueryString() != null) {
            logger.error("Query string: {}", request.getQueryString());
        }
        
        // Check if it's an invalid token response error (usually means client secret is wrong)
        String errorMessage = exception.getMessage();
        if (errorMessage != null && errorMessage.contains("invalid_token_response")) {
            logger.error("OAuth2 token exchange failed - possible causes:");
            logger.error("1. Client Secret is incorrect or expired");
            logger.error("2. Redirect URI doesn't match Google Console configuration");
            logger.error("3. Client ID is incorrect");
            logger.error("Please verify GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables");
            logger.error("Verify redirect URI in Google Console: http://localhost:8080/login/oauth2/code/google");
        }
        
        // Redirect to custom error endpoint
        response.sendRedirect("/api/oauth2/error");
    }
}

