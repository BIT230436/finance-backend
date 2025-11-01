package com.example.financebackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

/**
 * Main Spring Boot Application.
 */
@SpringBootApplication
@EnableScheduling
public class FinanceBackendApplication {

    private static final Logger logger = LoggerFactory.getLogger(FinanceBackendApplication.class);

    public static void main(String[] args) {
        // Check if Google client-id is set via environment variable
        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        
        if (!StringUtils.hasText(clientId)) {
            // If not set, exclude OAuth2 auto-configuration to prevent BeanCreationException
            logger.info("GOOGLE_CLIENT_ID environment variable not set. Excluding OAuth2 auto-configuration.");
            logger.info("To enable OAuth2, set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables,");
            logger.info("or run SETUP_OAUTH2.ps1 (PowerShell) / SETUP_OAUTH2.bat (CMD) script.");
            System.setProperty("spring.autoconfigure.exclude", 
                "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration");
        } else {
            logger.info("GOOGLE_CLIENT_ID found. OAuth2 login will be enabled.");
            // Ensure OAuth2 auto-configuration is not excluded
            System.clearProperty("spring.autoconfigure.exclude");
        }
        
        SpringApplication.run(FinanceBackendApplication.class, args);
    }

}
