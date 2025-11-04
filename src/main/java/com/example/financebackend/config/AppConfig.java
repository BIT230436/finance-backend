package com.example.financebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Tạo một Bean RestTemplate để CaptchaService có thể gọi API Google
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
