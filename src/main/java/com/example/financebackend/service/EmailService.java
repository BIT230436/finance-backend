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

/**
 * Email service for sending various types of emails
 * - Password reset emails
 * - Notification emails
 * - Welcome emails
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

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

    /**
     * Send password reset email with token
     */
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        if (!emailEnabled) {
            logger.warn("Email service is disabled. Skipping password reset email to: {}", toEmail);
            logger.info("Password reset token for {}: {}", toEmail, token);
            return;
        }

        try {
            String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
            
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("resetLink", resetLink);
            context.setVariable("expirationTime", "1 giờ");

            String htmlContent = templateEngine.process("email/password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu - Finance App");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }

    /**
     * Send welcome email after registration
     */
    public void sendWelcomeEmail(String toEmail, String fullName) {
        if (!emailEnabled) {
            logger.warn("Email service is disabled. Skipping welcome email to: {}", toEmail);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("loginUrl", frontendUrl + "/auth/login");

            String htmlContent = templateEngine.process("email/welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Chào mừng đến với Finance App!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome emails - not critical
        }
    }

    /**
     * Send budget alert notification email
     */
    public void sendBudgetAlertEmail(String toEmail, String fullName, String categoryName, 
                                    double percentageUsed, String usedAmount, String limitAmount) {
        if (!emailEnabled) {
            logger.warn("Email service is disabled. Skipping budget alert email to: {}", toEmail);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("categoryName", categoryName);
            context.setVariable("percentageUsed", String.format("%.0f", percentageUsed));
            context.setVariable("usedAmount", usedAmount);
            context.setVariable("limitAmount", limitAmount);
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

            String htmlContent = templateEngine.process("email/budget-alert", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            
            String subject = percentageUsed >= 100 
                ? "⚠️ Cảnh báo: Ngân sách đã vượt quá!" 
                : "⚠️ Cảnh báo: Ngân sách sắp hết!";
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Budget alert email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send budget alert email to: {}", toEmail, e);
            // Don't throw exception - notification emails are not critical
        }
    }

    /**
     * Send daily transaction reminder email
     */
    public void sendDailyReminderEmail(String toEmail, String fullName) {
        if (!emailEnabled) {
            logger.warn("Email service is disabled. Skipping daily reminder email to: {}", toEmail);
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("transactionsUrl", frontendUrl + "/transactions");

            String htmlContent = templateEngine.process("email/daily-reminder", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("💡 Nhắc nhở: Đừng quên ghi lại giao dịch hôm nay!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Daily reminder email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send daily reminder email to: {}", toEmail, e);
            // Don't throw exception - reminder emails are not critical
        }
    }

    /**
     * Send simple text email (fallback)
     */
    public void sendSimpleEmail(String toEmail, String subject, String text) {
        if (!emailEnabled) {
            logger.warn("Email service is disabled. Skipping email to: {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            logger.info("Simple email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
}

