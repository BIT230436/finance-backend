package com.example.financebackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal; // Thêm

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // ... (Giữ nguyên constructor và các biến)
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@financeapp.com}")
    private String fromEmail;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // ... (Giữ nguyên hàm sendVerificationCodeEmail, sendPasswordResetEmail, sendWelcomeEmail)

    // --- THÊM HÀM BỊ THIẾU NÀY ---
    public void sendBudgetAlertEmail(String toEmail, String budgetName, double percentage, BigDecimal amount, BigDecimal budgetAmount) {
        if (!emailEnabled) {
            logger.warn("Email service is disabled. Skipping budget alert email to: {}", toEmail);
            return;
        }

        Context context = new Context();
        context.setVariable("budgetName", budgetName);
        context.setVariable("percentage", String.format("%.0f%%", percentage * 100)); // "80%"
        context.setVariable("amount", amount.toString()); // Số tiền đã chi
        context.setVariable("budgetAmount", budgetAmount.toString()); // Ngân sách
        context.setVariable("frontendUrl", frontendUrl);

        String htmlContent = templateEngine.process("email/budget-alert", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Cảnh báo ngân sách: Bạn đã vượt " + context.getVariable("percentage") + " ngân sách " + budgetName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Budget alert email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send budget alert email to: {}", toEmail, e);
            // Không ném lỗi vì đây là email thông báo
        }
    }

    // ... (Giữ nguyên các hàm sendDailyReminderEmail, sendSimpleEmail)
    public void sendVerificationCodeEmail(String toEmail, String otpCode, String subject) {
        // ...
    }
    public void sendPasswordResetEmail(String toEmail, String token) {
        // ...
    }
    public void sendWelcomeEmail(String toEmail, String name) {
        // ...
    }
    public void sendDailyReminderEmail(String toEmail, String name, long pendingCount) {
        // ...
    }
    public void sendSimpleEmail(String toEmail, String subject, String text) {
        // ...
    }
}
