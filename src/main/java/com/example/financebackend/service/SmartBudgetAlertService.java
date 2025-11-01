package com.example.financebackend.service;

import com.example.financebackend.entity.Budget;
import com.example.financebackend.entity.Notification;
import com.example.financebackend.repository.BudgetRepository;
import com.example.financebackend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Smart Budget Alert Service với multi-level warnings
 * 
 * Alert Levels:
 * - GREEN (0-50%): All good, no alerts
 * - YELLOW (50-80%): Warning - "Bạn đã dùng 50% ngân sách"
 * - ORANGE (80-95%): Critical warning - "Cảnh báo: Đã dùng 80% ngân sách"
 * - RED (95-100%+): Alert - "Nguy hiểm: Sắp vượt ngân sách!"
 */
@Service
@Transactional
public class SmartBudgetAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SmartBudgetAlertService.class);

    private final BudgetRepository budgetRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public SmartBudgetAlertService(BudgetRepository budgetRepository,
                                  NotificationRepository notificationRepository,
                                  EmailService emailService) {
        this.budgetRepository = budgetRepository;
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    /**
     * Check và gửi alerts cho một budget
     */
    public void checkAndSendAlerts(Budget budget) {
        if (budget.getLimitAmount() == null || budget.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal used = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
        BigDecimal limit = budget.getLimitAmount();
        BigDecimal ratio = used.divide(limit, 4, RoundingMode.HALF_UP);
        double percentage = ratio.multiply(new BigDecimal("100")).doubleValue();

        logger.debug("Checking alerts for budget {}: {}% used", budget.getId(), percentage);

        // 100%+ Alert (RED)
        if (percentage >= 100 && !Boolean.TRUE.equals(budget.getAlertSent100())) {
            sendAlert(budget, AlertLevel.RED, percentage, used, limit);
            budget.setAlertSent100(true);
            budgetRepository.save(budget);
        }
        // 95% Alert (RED)
        else if (percentage >= 95 && !Boolean.TRUE.equals(budget.getAlertSent95())) {
            sendAlert(budget, AlertLevel.RED_WARNING, percentage, used, limit);
            budget.setAlertSent95(true);
            budgetRepository.save(budget);
        }
        // 80% Alert (ORANGE)
        else if (percentage >= 80 && !Boolean.TRUE.equals(budget.getAlertSent80())) {
            sendAlert(budget, AlertLevel.ORANGE, percentage, used, limit);
            budget.setAlertSent80(true);
            budgetRepository.save(budget);
        }
        // 50% Alert (YELLOW)
        else if (percentage >= 50 && !Boolean.TRUE.equals(budget.getAlertSent50())) {
            sendAlert(budget, AlertLevel.YELLOW, percentage, used, limit);
            budget.setAlertSent50(true);
            budgetRepository.save(budget);
        }
    }

    /**
     * Reset alert flags khi budget period mới bắt đầu
     */
    public void resetAlertFlags(Budget budget) {
        budget.setAlertSent50(false);
        budget.setAlertSent80(false);
        budget.setAlertSent95(false);
        budget.setAlertSent100(false);
        budgetRepository.save(budget);
        logger.info("Reset alert flags for budget {}", budget.getId());
    }

    private void sendAlert(Budget budget, AlertLevel level, double percentage, BigDecimal used, BigDecimal limit) {
        String title = getAlertTitle(level, percentage);
        String message = getAlertMessage(level, budget.getCategory().getName(), percentage, used, limit);
        
        // Create in-app notification
        Notification notification = new Notification();
        notification.setUser(budget.getUser());
        notification.setType(Notification.NotificationType.BUDGET_WARNING);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityId(budget.getId());
        notification.setRelatedEntityType("budget");
        notificationRepository.save(notification);

        logger.info("Sent {} alert for budget {}: {}%", level, budget.getId(), percentage);

        // Send email notification
        try {
            emailService.sendBudgetAlertEmail(
                budget.getUser().getEmail(),
                budget.getUser().getFullName(),
                budget.getCategory().getName(),
                percentage,
                used.toPlainString(),
                limit.toPlainString()
            );
        } catch (Exception e) {
            logger.error("Failed to send budget alert email: {}", e.getMessage(), e);
        }
    }

    private String getAlertTitle(AlertLevel level, double percentage) {
        switch (level) {
            case RED:
                return "🚨 Ngân sách đã vượt quá!";
            case RED_WARNING:
                return "⚠️ Nguy hiểm: Sắp vượt ngân sách!";
            case ORANGE:
                return "⚠️ Cảnh báo nghiêm trọng";
            case YELLOW:
                return "💡 Thông báo ngân sách";
            default:
                return "📊 Cập nhật ngân sách";
        }
    }

    private String getAlertMessage(AlertLevel level, String categoryName, double percentage, 
                                   BigDecimal used, BigDecimal limit) {
        String percentStr = String.format("%.0f", percentage);
        
        switch (level) {
            case RED:
                return String.format(
                    "Ngân sách '%s' đã vượt quá! Đã sử dụng %s%% (%s / %s). " +
                    "Hãy cân nhắc giảm chi tiêu hoặc điều chỉnh ngân sách.",
                    categoryName, percentStr, formatMoney(used), formatMoney(limit)
                );
            case RED_WARNING:
                return String.format(
                    "Ngân sách '%s' đã dùng %s%% (%s / %s). " +
                    "Chỉ còn %s. Hãy cẩn thận với chi tiêu!",
                    categoryName, percentStr, formatMoney(used), formatMoney(limit),
                    formatMoney(limit.subtract(used))
                );
            case ORANGE:
                return String.format(
                    "Ngân sách '%s' đã dùng %s%% (%s / %s). " +
                    "Còn %s để chi tiêu. Đã đến lúc cân nhắc giảm chi!",
                    categoryName, percentStr, formatMoney(used), formatMoney(limit),
                    formatMoney(limit.subtract(used))
                );
            case YELLOW:
                return String.format(
                    "Bạn đã sử dụng %s%% ngân sách '%s' (%s / %s). " +
                    "Còn %s cho đến cuối kỳ. Hãy theo dõi chi tiêu nhé!",
                    percentStr, categoryName, formatMoney(used), formatMoney(limit),
                    formatMoney(limit.subtract(used))
                );
            default:
                return String.format(
                    "Ngân sách '%s': %s%% (%s / %s)",
                    categoryName, percentStr, formatMoney(used), formatMoney(limit)
                );
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        
        // Format with thousand separators
        long amountLong = amount.longValue();
        return String.format("%,d", amountLong).replace(",", ".");
    }

    enum AlertLevel {
        GREEN,          // 0-50%: All good
        YELLOW,         // 50-80%: Warning
        ORANGE,         // 80-95%: Critical warning
        RED_WARNING,    // 95-100%: Danger
        RED             // 100%+: Exceeded
    }
}

