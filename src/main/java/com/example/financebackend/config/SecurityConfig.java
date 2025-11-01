package com.example.financebackend.config;

import com.example.financebackend.filter.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired(required = false)
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
    
    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Check if Google client ID is configured
        boolean hasValidClientId = StringUtils.hasText(googleClientId) 
            && !googleClientId.isEmpty() 
            && !googleClientId.equals("${GOOGLE_CLIENT_ID:}");
        
        // OAuth2 login requires session to store authentication state during OAuth2 flow
        // But we want STATELESS for JWT-based API requests
        // Use IF_REQUIRED so session is created only when needed (for OAuth2)
        SessionCreationPolicy sessionPolicy = hasValidClientId 
            ? SessionCreationPolicy.IF_REQUIRED 
            : SessionCreationPolicy.STATELESS;
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> {
                session.sessionCreationPolicy(sessionPolicy);
                // Configure session cookie to persist across redirects
                if (sessionPolicy == SessionCreationPolicy.IF_REQUIRED) {
                    session.maximumSessions(1)
                        .maxSessionsPreventsLogin(false);
                }
            })
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/oauth2/**").permitAll()
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login", "/login/**", "/login/oauth2/**").permitAll()
                // Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Allow static resources (favicon, etc.)
                .requestMatchers("/favicon.ico", "/error", "/*.ico", "/*.png", "/*.jpg").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // Disable default login page - we handle OAuth2 login programmatically
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        // Only enable OAuth2 login if Google client ID is configured
        if (hasValidClientId) {
            // Check if client secret is also configured
            boolean hasValidClientSecret = StringUtils.hasText(googleClientSecret) 
                && !googleClientSecret.isEmpty() 
                && !googleClientSecret.equals("${GOOGLE_CLIENT_SECRET:}");
            
            if (!hasValidClientSecret) {
                logger.error("OAuth2 client-secret is missing or empty!");
                logger.error("Current client-secret value: '{}'", googleClientSecret);
                logger.error("Set GOOGLE_CLIENT_SECRET environment variable and restart application.");
            } else {
                try {
                    // OAuth2 login configuration
                    http.oauth2Login(oauth2 -> {
                        // Configure success handler to ensure proper session handling
                        if (oauth2AuthenticationSuccessHandler != null) {
                            oauth2.successHandler(oauth2AuthenticationSuccessHandler);
                            logger.info("OAuth2 custom success handler configured");
                        } else {
                            // Fallback to default
                            oauth2.defaultSuccessUrl("/api/oauth2/success", true);
                        }
                        oauth2.permitAll();
                        
                        // Configure authorization endpoint with custom request resolver
                        // This adds prompt=select_account to force account selection screen
                        if (clientRegistrationRepository != null) {
                            oauth2.authorizationEndpoint(authorization -> {
                                authorization.authorizationRequestResolver(
                                    new CustomOAuth2AuthorizationRequestResolver(
                                        clientRegistrationRepository,
                                        "/oauth2/authorization"
                                    )
                                );
                            });
                            logger.info("OAuth2 custom authorization request resolver configured - will force account selection");
                        }
                        
                        // Use custom failure handler if available
                        if (oauth2AuthenticationFailureHandler != null) {
                            oauth2.failureHandler(oauth2AuthenticationFailureHandler);
                        } else {
                            oauth2.failureUrl("/api/oauth2/error");
                        }
                    });
                    logger.info("OAuth2 login enabled for Google");
                    logger.info("Client ID: {}...", googleClientId.substring(0, Math.min(30, googleClientId.length())));
                    logger.info("Client Secret length: {} characters", googleClientSecret.length());
                    logger.info("Client Secret starts with: {}...", googleClientSecret.substring(0, Math.min(10, googleClientSecret.length())));
                    logger.info("Client Secret ends with: ...{}", googleClientSecret.substring(Math.max(0, googleClientSecret.length() - 4)));
                    logger.info("Redirect URI (auto-constructed): http://localhost:8080/login/oauth2/code/google");
                    logger.info("IMPORTANT: Verify redirect URI in Google Console matches exactly: http://localhost:8080/login/oauth2/code/google");
                    logger.info("IMPORTANT: Verify Client Secret ends with: ...11T8 (from Google Console)");
                } catch (Exception e) {
                    // OAuth2 not configured properly, continue without it
                    logger.error("Failed to configure OAuth2 login, continuing without it: {}", e.getMessage(), e);
                }
            }
        } else {
            logger.warn("OAuth2 login disabled - Google client ID not configured or empty.");
            logger.warn("Current client-id value: '{}'", googleClientId);
            logger.warn("Set GOOGLE_CLIENT_ID environment variable and restart application to enable.");
        }

        // JWT filter for API requests (only applies to authenticated requests, not OAuth2 flow)
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
