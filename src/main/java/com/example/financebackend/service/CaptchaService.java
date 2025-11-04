package com.example.financebackend.service;

import com.example.financebackend.dto.CaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class CaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Value("${google.recaptcha.secret-key:}")
    private String recaptchaSecretKey;

    private final RestTemplate restTemplate;

    public CaptchaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Phương thức này cần được gọi trong AppConfig để tạo Bean
    // @Bean
    // public RestTemplate restTemplate() {
    //     return new RestTemplate();
    // }
    // Tốt hơn là tạo 1 tệp AppConfig.java
    // @Configuration
    // public class AppConfig {
    //     @Bean
    //     public RestTemplate restTemplate() {
    //         return new RestTemplate();
    //     }
    // }
    // Tạm thời, chúng ta giả định bạn đã có Bean RestTemplate.
    // Nếu chưa, bạn cần thêm Bean như trên.

    public void validateCaptcha(String captchaToken) {
        if (recaptchaSecretKey == null || recaptchaSecretKey.isEmpty()) {
            logger.warn("reCAPTCHA secret key chưa được cấu hình. Bỏ qua xác thực CAPTCHA.");
            // Trong môi trường dev, bạn có thể cho qua. Trong prod, bạn nên ném lỗi.
            // throw new IllegalStateException("Xác thực CAPTCHA bị lỗi: secret key không được cấu hình.");
            return; // Tạm thời cho qua nếu key không có
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", recaptchaSecretKey);
        body.add("response", captchaToken);

        try {
            CaptchaResponse response = restTemplate.postForObject(RECAPTCHA_VERIFY_URL, body, CaptchaResponse.class);

            if (response == null || !response.isSuccess()) {
                logger.warn("Xác thực reCAPTCHA thất bại. Phản hồi: {}", response);
                throw new IllegalStateException("Xác thực CAPTCHA thất bại. Vui lòng thử lại.");
            }
            logger.info("Xác thực reCAPTCHA thành công.");

        } catch (Exception e) {
            logger.error("Lỗi khi gọi API Google reCAPTCHA: {}", e.getMessage());
            throw new IllegalStateException("Không thể xác thực CAPTCHA. Vui lòng thử lại sau.");
        }
    }
}
