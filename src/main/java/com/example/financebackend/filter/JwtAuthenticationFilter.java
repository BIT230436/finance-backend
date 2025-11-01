package com.example.financebackend.filter;

import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT filter for OAuth2 endpoints - OAuth2 flow handles authentication differently
        String requestPath = request.getRequestURI();
        if (requestPath != null && (
            requestPath.startsWith("/oauth2/") ||
            requestPath.startsWith("/login/oauth2/") ||
            requestPath.startsWith("/api/oauth2/") ||
            requestPath.equals("/login") ||
            requestPath.startsWith("/login?")
        )) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.extractUserId(token);
                Long tokenVersion = jwtUtil.extractTokenVersion(token);

                // Check token version to ensure token hasn't been invalidated by logout-all
                var user = userRepository.findById(userId);
                if (user.isPresent()) {
                    var userEntity = user.get();
                    
                    // Check if user is enabled
                    if (!userEntity.getEnabled()) {
                        logger.warn("User is disabled: userId={}", userId);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    Long userTokenVersion = userEntity.getTokenVersion() != null ? userEntity.getTokenVersion() : 0L;
                    if (tokenVersion != null && tokenVersion.equals(userTokenVersion)) {
                        String email = jwtUtil.extractUsername(token);
                        String role = jwtUtil.extractRole(token);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Authentication set for user {}: {}", userId, email);
                    } else {
                        // Token version mismatch
                        logger.warn("Token version mismatch for user {}: tokenVersion={}, userTokenVersion={}", 
                                userId, tokenVersion, userTokenVersion);
                        SecurityContextHolder.clearContext();
                        sendUnauthorizedResponse(response, "Token đã bị vô hiệu hóa. Vui lòng đăng nhập lại.");
                        return;
                    }
                } else {
                    // User not found - token may contain invalid userId
                    logger.error("User not found for userId: {}. Token may be invalid or user was deleted. Request path: {}", 
                            userId, request.getRequestURI());
                    SecurityContextHolder.clearContext();
                    // Return 401 Unauthorized instead of 403
                    sendUnauthorizedResponse(response, "Token không hợp lệ hoặc người dùng không tồn tại. Vui lòng đăng nhập lại.");
                    return;
                }
            } else {
                // Invalid token
                logger.warn("Invalid JWT token for path: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Token không hợp lệ. Vui lòng đăng nhập lại.");
                return;
            }
        } catch (Exception e) {
            // Log exception for debugging
            logger.error("Error processing JWT token for path: {}", request.getRequestURI(), e);
            SecurityContextHolder.clearContext();
            sendUnauthorizedResponse(response, "Lỗi xử lý token. Vui lòng đăng nhập lại.");
            return;
        }

        filterChain.doFilter(request, response);
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write("{\"status\":401,\"errorCode\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}");
            writer.flush();
        }
    }
}
