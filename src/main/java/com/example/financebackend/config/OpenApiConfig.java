package com.example.financebackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI financeBackendOpenAPI() {
        // Define JWT security scheme
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Backend API")
                        .description("RESTful API for Personal Finance Management System\n\n" +
                                "## Features\n" +
                                "- 👤 User Authentication (JWT + OAuth2 + 2FA)\n" +
                                "- 💰 Wallet Management (Multi-currency, Shared Wallets)\n" +
                                "- 💸 Transaction Management (Income/Expense/Transfer)\n" +
                                "- 📁 Category Management\n" +
                                "- 💼 Budget Management (with alerts)\n" +
                                "- 🔍 Advanced Search & Filters\n" +
                                "- 📊 Reports & Analytics\n" +
                                "- 🎯 Financial Goals\n" +
                                "- 🔔 Notifications\n" +
                                "- 🔁 Recurring Transactions\n" +
                                "- 👥 Admin Functions\n\n" +
                                "## Authentication\n" +
                                "Most endpoints require JWT authentication. Use the `/api/auth/login` endpoint to get an access token, " +
                                "then click 'Authorize' button and enter: `Bearer <your-access-token>`")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Finance App Team")
                                .email("support@financeapp.com")
                                .url("https://financeapp.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.financeapp.com")
                                .description("Production Server (Coming Soon)")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authentication\n\n" +
                                                "1. Login via `/api/auth/login` or `/api/auth/register`\n" +
                                                "2. Copy the `accessToken` from response\n" +
                                                "3. Click 'Authorize' button above\n" +
                                                "4. Enter: `Bearer <your-access-token>`\n" +
                                                "5. Click 'Authorize' and 'Close'\n\n" +
                                                "Token expires after 24 hours. Use refresh token to get new access token.")));
    }
}

