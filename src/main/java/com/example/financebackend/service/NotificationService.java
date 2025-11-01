package com.example.financebackend.service;

import com.example.financebackend.dto.NotificationDto;
import com.example.financebackend.entity.Budget;
import com.example.financebackend.entity.Notification;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.BudgetRepository;
import com.example.financebackend.repository.NotificationRepository;
import com.example.financebackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              BudgetRepository budgetRepository,
                              BudgetService budgetService,
                              EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.budgetRepository = budgetRepository;
        this.budgetService = budgetService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền truy cập thông báo này");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findUnreadByUserId(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    public void createBudgetWarning(Budget budget) {
        BigDecimal used = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
        BigDecimal limit = budget.getLimitAmount() != null ? budget.getLimitAmount() : BigDecimal.ZERO;
        
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            return; // Skip if limit is zero or negative
        }

        BigDecimal ratio = used.divide(limit, 4, java.math.RoundingMode.HALF_UP);
        BigDecimal threshold = budget.getAlertThreshold() != null ? budget.getAlertThreshold() : new BigDecimal("0.80");

        Notification notification = new Notification();
        notification.setUser(budget.getUser());
        
        if (ratio.compareTo(BigDecimal.ONE) >= 0) {
            // Budget exceeded
            notification.setType(Notification.NotificationType.BUDGET_EXCEEDED);
            notification.setTitle("Ngân sách đã vượt quá");
            notification.setMessage(String.format(
                    "Ngân sách '%s' đã vượt quá hạn mức. Đã sử dụng: %.0f%% (%s / %s)",
                    budget.getCategory().getName(),
                    ratio.multiply(new BigDecimal("100")).doubleValue(),
                    used.toPlainString(),
                    limit.toPlainString()
            ));
        } else if (ratio.compareTo(threshold) >= 0) {
            // Budget warning
            notification.setType(Notification.NotificationType.BUDGET_WARNING);
            notification.setTitle("Cảnh báo ngân sách");
            notification.setMessage(String.format(
                    "Ngân sách '%s' sắp hết. Đã sử dụng: %.0f%% (%s / %s)",
                    budget.getCategory().getName(),
                    ratio.multiply(new BigDecimal("100")).doubleValue(),
                    used.toPlainString(),
                    limit.toPlainString()
            ));
        } else {
            return; // Below threshold, no notification needed
        }

        notification.setRelatedEntityId(budget.getId());
        notification.setRelatedEntityType("budget");
        notificationRepository.save(notification);
        
        // Send email notification
        try {
            emailService.sendBudgetAlertEmail(
                budget.getUser().getEmail(),
                budget.getUser().getFullName(),
                budget.getCategory().getName(),
                ratio.multiply(new BigDecimal("100")).doubleValue(),
                used.toPlainString(),
                limit.toPlainString()
            );
        } catch (Exception e) {
            // Log error but don't fail the notification creation
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                .error("Failed to send budget alert email to: {}", budget.getUser().getEmail(), e);
        }
    }

    public void createDailyReminder(User user) {
        // Check if user has transactions today
        LocalDate today = LocalDate.now();
        // This check would require TransactionRepository, but for now we'll create the reminder
        // In production, check actual transaction count for today
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(Notification.NotificationType.DAILY_REMINDER);
        notification.setTitle("Nhắc nhở ghi giao dịch");
        notification.setMessage("Đừng quên ghi lại các giao dịch thu/chi của ngày hôm nay!");
        
        notificationRepository.save(notification);
        
        // Send email reminder
        try {
            emailService.sendDailyReminderEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            // Log error but don't fail the notification creation
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                .error("Failed to send daily reminder email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Create welcome notification for new users
     */
    public void createWelcomeNotification(User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(Notification.NotificationType.SYSTEM);
        notification.setTitle("🎉 Chào mừng đến với Finance App!");
        notification.setMessage(String.format(
            "Xin chào %s! Chúc mừng bạn đã đăng ký thành công. " +
            "Hãy bắt đầu quản lý tài chính của bạn ngay hôm nay bằng cách tạo ví và ghi lại giao dịch đầu tiên.",
            user.getFullName()
        ));
        
        notificationRepository.save(notification);
    }

    /**
     * Create system notification for any user
     */
    public void createSystemNotification(Long userId, String title, String message) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(Notification.NotificationType.SYSTEM);
        notification.setTitle(title);
        notification.setMessage(message);
        
        notificationRepository.save(notification);
    }

    private NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setRead(notification.getRead() != null ? notification.getRead() : false);
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRelatedEntityType(notification.getRelatedEntityType());
        return dto;
    }
}

