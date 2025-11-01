package com.example.financebackend.service;

import com.example.financebackend.entity.Budget;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.BudgetRepository;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScheduledNotificationService {

    private final NotificationService notificationService;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;

    public ScheduledNotificationService(NotificationService notificationService,
                                       BudgetRepository budgetRepository,
                                       TransactionRepository transactionRepository,
                                       UserRepository userRepository,
                                       BudgetService budgetService) {
        this.notificationService = notificationService;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
    }

    // Run daily at 8 AM to check budget alerts
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkBudgetAlerts() {
        LocalDate today = LocalDate.now();
        
        // Get all active budgets that are currently in their period
        List<Budget> activeBudgets = budgetRepository.findAll().stream()
                .filter(budget -> {
                    LocalDate startDate = budget.getStartDate();
                    LocalDate endDate = budget.getEndDate();
                    return !today.isBefore(startDate) && !today.isAfter(endDate);
                })
                .collect(Collectors.toList());

        for (Budget budget : activeBudgets) {
            // Update used amount
            budgetService.updateUsedAmount(budget);
            
            // Check if notification should be created
            BigDecimal used = budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO;
            BigDecimal limit = budget.getLimitAmount() != null ? budget.getLimitAmount() : BigDecimal.ZERO;
            
            if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal ratio = used.divide(limit, 4, RoundingMode.HALF_UP);
            BigDecimal threshold = budget.getAlertThreshold() != null ? budget.getAlertThreshold() : new BigDecimal("0.80");

            // Check if we already sent notification today for this budget
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            boolean hasNotificationToday = notificationService.getUserNotifications(budget.getUser().getId()).stream()
                    .anyMatch(n -> n.getRelatedEntityId() != null 
                            && n.getRelatedEntityId().equals(budget.getId())
                            && n.getCreatedAt().isAfter(todayStart));

            if (!hasNotificationToday && ratio.compareTo(threshold) >= 0) {
                notificationService.createBudgetWarning(budget);
            }
        }
    }

    // Run daily at 9 PM to remind users to log transactions
    @Scheduled(cron = "0 0 21 * * ?")
    public void sendDailyReminders() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);

        // Get all active users
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::getEnabled)
                .collect(Collectors.toList());

        for (User user : activeUsers) {
            // Check if user has transactions today
            List<Transaction> todayTransactions = transactionRepository.findByUserId(user.getId()).stream()
                    .filter(tx -> {
                        LocalDateTime occurredAt = tx.getOccurredAt();
                        return !occurredAt.isBefore(todayStart) && !occurredAt.isAfter(todayEnd);
                    })
                    .collect(Collectors.toList());

            // Only send reminder if no transactions today
            if (todayTransactions.isEmpty()) {
                // Check if we already sent reminder today
                LocalDateTime reminderStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
                boolean hasReminderToday = notificationService.getUserNotifications(user.getId()).stream()
                        .anyMatch(n -> n.getType() == com.example.financebackend.entity.Notification.NotificationType.DAILY_REMINDER
                                && n.getCreatedAt().isAfter(reminderStart));

                if (!hasReminderToday) {
                    notificationService.createDailyReminder(user);
                }
            }
        }
    }
}

