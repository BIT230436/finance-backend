package com.example.financebackend.service;

import com.example.financebackend.dto.BudgetDto;
import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.entity.Budget;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.BudgetRepository;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.UserRepository;
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
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SmartBudgetAlertService smartBudgetAlertService;
    private NotificationService notificationService; // Lazy init

    public BudgetService(BudgetRepository budgetRepository,
                        CategoryRepository categoryRepository,
                        TransactionRepository transactionRepository,
                        UserRepository userRepository,
                        SmartBudgetAlertService smartBudgetAlertService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.smartBudgetAlertService = smartBudgetAlertService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public List<BudgetDto> findAllByUserId(Long userId) {
        return budgetRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public BudgetDto findByIdAndUserId(Long id, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ngân sách"));
        return toDto(budget);
    }

    public BudgetDto create(BudgetDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        if (!category.getType().name().equals("EXPENSE")) {
            throw new IllegalArgumentException("Ngân sách chỉ có thể được tạo cho danh mục chi tiêu");
        }

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Validate limit amount
        if (dto.getLimitAmount() == null || dto.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hạn mức ngân sách phải lớn hơn 0");
        }

        // Tự động tính period nếu chưa được set hoặc là CUSTOM
        Budget.Period period = dto.getPeriod() != null ? dto.getPeriod() : Budget.Period.CUSTOM;
        if (period == Budget.Period.CUSTOM) {
            // Tự động detect period dựa trên start/end date
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate());
            if (daysBetween >= 25 && daysBetween <= 35) {
                period = Budget.Period.MONTHLY;
            } else if (daysBetween >= 5 && daysBetween <= 9) {
                period = Budget.Period.WEEKLY;
            }
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setPeriod(period);
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
        budget.setLimitAmount(dto.getLimitAmount());
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setAlertThreshold(dto.getAlertThreshold() != null ? dto.getAlertThreshold() : new BigDecimal("0.80"));

        Budget saved = budgetRepository.save(budget);
        updateUsedAmount(saved);
        
        // Create notification
        createBudgetCreatedNotification(saved);
        
        return toDto(saved);
    }

    public BudgetDto update(Long id, BudgetDto dto, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ngân sách"));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        budget.setCategory(category);
        budget.setPeriod(dto.getPeriod());
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
        budget.setLimitAmount(dto.getLimitAmount());
        if (dto.getAlertThreshold() != null) {
            budget.setAlertThreshold(dto.getAlertThreshold());
        }

        Budget saved = budgetRepository.save(budget);
        updateUsedAmount(saved);
        return toDto(saved);
    }

    public void delete(Long id, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ngân sách"));
        budgetRepository.delete(budget);
    }

    public List<TransactionDto> getTransactionsByBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ngân sách"));
        
        LocalDateTime startDateTime = budget.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = budget.getEndDate().atTime(23, 59, 59);
        
        List<Transaction> transactions = transactionRepository.findByBudget(
                userId,
                budget.getCategory().getId(),
                startDateTime,
                endDateTime
        );
        
        return transactions.stream()
                .map(this::transactionToDto)
                .collect(Collectors.toList());
    }

    private TransactionDto transactionToDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setWalletId(transaction.getWallet().getId());
        dto.setCategoryId(transaction.getCategory().getId());
        dto.setNote(transaction.getNote());
        dto.setOccurredAt(transaction.getOccurredAt());
        dto.setAttachmentUrl(transaction.getAttachmentUrl());
        return dto;
    }

    public List<BudgetDto> getAlerts(Long userId) {
        return budgetRepository.findByUserId(userId).stream()
                .map(this::updateUsedAmount)
                .filter(budget -> {
                    BigDecimal ratio = budget.getUsedAmount().divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP);
                    return ratio.compareTo(budget.getAlertThreshold()) >= 0;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Package-private to allow ScheduledNotificationService to use it
    Budget updateUsedAmount(Budget budget) {
        LocalDateTime startDateTime = budget.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = budget.getEndDate().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findByUserId(budget.getUser().getId()).stream()
                .filter(tx -> tx.getCategory().getId().equals(budget.getCategory().getId()))
                .filter(tx -> tx.getType() == Transaction.TransactionType.EXPENSE)
                .filter(tx -> {
                    LocalDateTime occurredAt = tx.getOccurredAt();
                    return !occurredAt.isBefore(startDateTime) && !occurredAt.isAfter(endDateTime);
                })
                .collect(Collectors.toList());

        BigDecimal usedAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        budget.setUsedAmount(usedAmount);
        budgetRepository.save(budget);
        
        // Check and send smart alerts
        smartBudgetAlertService.checkAndSendAlerts(budget);
        
        return budget;
    }

    private BudgetDto toDto(Budget budget) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        if (budget.getCategory() != null) {
            dto.setCategoryId(budget.getCategory().getId());
        }
        dto.setPeriod(budget.getPeriod());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        dto.setLimitAmount(budget.getLimitAmount() != null ? budget.getLimitAmount() : BigDecimal.ZERO);
        dto.setUsedAmount(budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO);
        dto.setAlertThreshold(budget.getAlertThreshold() != null ? budget.getAlertThreshold() : new BigDecimal("0.80"));
        return dto;
    }

    private void createBudgetCreatedNotification(Budget budget) {
        if (notificationService == null) return;
        
        try {
            String title = "📊 Ngân sách mới đã được tạo";
            String message = String.format(
                "Ngân sách cho '%s' với hạn mức %,d VND từ %s đến %s",
                budget.getCategory().getName(),
                budget.getLimitAmount().longValue(),
                budget.getStartDate(),
                budget.getEndDate()
            );
            
            notificationService.createSystemNotification(budget.getUser().getId(), title, message);
        } catch (Exception e) {
            // Log but don't fail
        }
    }
}