package com.example.financebackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String OTP_CACHE = "otpCache";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(OTP_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Cấu hình mã OTP hết hạn sau 10 phút
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)); // Giới hạn 1000 mã OTP trong cache
        return cacheManager;
    }
}
