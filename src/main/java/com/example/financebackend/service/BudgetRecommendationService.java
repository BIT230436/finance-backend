package com.example.financebackend.service;

import com.example.financebackend.entity.Budget;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.repository.BudgetRepository;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BudgetRecommendationService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    public BudgetRecommendationService(TransactionRepository transactionRepository,
                                      CategoryRepository categoryRepository,
                                      BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
    }

    /**
     * Suggest budget cho category dựa trên spending history
     */
    public BudgetRecommendationDto recommendBudget(Long categoryId, Long userId, int months) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Chỉ có thể đề xuất budget cho danh mục chi tiêu");
        }

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(months);

        // Get all transactions cho category trong khoảng thời gian
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> !t.getOccurredAt().isBefore(startDate) && !t.getOccurredAt().isAfter(endDate))
                .collect(Collectors.toList());

        if (transactions.isEmpty()) {
            BudgetRecommendationDto dto = new BudgetRecommendationDto();
            dto.setCategoryId(categoryId);
            dto.setCategoryName(category.getName());
            dto.setRecommendedAmount(BigDecimal.ZERO);
            dto.setAverageSpending(BigDecimal.ZERO);
            dto.setMaxSpending(BigDecimal.ZERO);
            dto.setMinSpending(BigDecimal.ZERO);
            dto.setTransactionCount(0);
            dto.setMessage("Không có dữ liệu chi tiêu trong " + months + " tháng qua");
            return dto;
        }

        // Calculate statistics
        BigDecimal totalSpending = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageSpending = totalSpending.divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP);

        BigDecimal maxSpending = transactions.stream()
                .map(Transaction::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal minSpending = transactions.stream()
                .map(Transaction::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Recommended budget: average + 10% buffer
        BigDecimal recommendedAmount = averageSpending.multiply(new BigDecimal("1.10")).setScale(0, RoundingMode.HALF_UP);

        // Check existing budget
        List<Budget> existingBudgets = budgetRepository.findByUserId(userId).stream()
                .filter(b -> b.getCategory() != null && b.getCategory().getId().equals(categoryId))
                .collect(Collectors.toList());

        BudgetRecommendationDto dto = new BudgetRecommendationDto();
        dto.setCategoryId(categoryId);
        dto.setCategoryName(category.getName());
        dto.setRecommendedAmount(recommendedAmount);
        dto.setAverageSpending(averageSpending);
        dto.setMaxSpending(maxSpending);
        dto.setMinSpending(minSpending);
        dto.setTransactionCount(transactions.size());
        dto.setMonthsAnalyzed(months);
        
        if (!existingBudgets.isEmpty()) {
            Budget currentBudget = existingBudgets.get(0);
            dto.setCurrentBudget(currentBudget.getLimitAmount());
            dto.setCurrentUsage(currentBudget.getUsedAmount());
            BigDecimal difference = recommendedAmount.subtract(currentBudget.getLimitAmount());
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                dto.setMessage(String.format("Đề xuất tăng budget từ %,.0f lên %,.0f (+%,.0f)", 
                        currentBudget.getLimitAmount(), recommendedAmount, difference));
            } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                dto.setMessage(String.format("Đề xuất giảm budget từ %,.0f xuống %,.0f (%,.0f)", 
                        currentBudget.getLimitAmount(), recommendedAmount, difference.abs()));
            } else {
                dto.setMessage("Budget hiện tại phù hợp với chi tiêu thực tế");
            }
        } else {
            dto.setMessage(String.format("Bạn thường chi trung bình %,.0f/tháng cho '%s', đề xuất budget %,.0f/tháng", 
                    averageSpending, category.getName(), recommendedAmount));
        }

        return dto;
    }

    /**
     * Recommend budgets cho tất cả expense categories
     */
    public List<BudgetRecommendationDto> recommendBudgetsForAllCategories(Long userId, int months) {
        List<Category> expenseCategories = categoryRepository.findByUserIdAndType(userId, Category.CategoryType.EXPENSE);
        List<BudgetRecommendationDto> recommendations = new ArrayList<>();

        for (Category category : expenseCategories) {
            try {
                BudgetRecommendationDto recommendation = recommendBudget(category.getId(), userId, months);
                recommendations.add(recommendation);
            } catch (Exception e) {
                // Skip categories that fail (e.g., no transactions)
            }
        }

        // Sort by recommended amount (descending)
        recommendations.sort((a, b) -> b.getRecommendedAmount().compareTo(a.getRecommendedAmount()));

        return recommendations;
    }

    public static class BudgetRecommendationDto {
        private Long categoryId;
        private String categoryName;
        private BigDecimal recommendedAmount;
        private BigDecimal averageSpending;
        private BigDecimal maxSpending;
        private BigDecimal minSpending;
        private Integer transactionCount;
        private Integer monthsAnalyzed;
        private BigDecimal currentBudget;
        private BigDecimal currentUsage;
        private String message;

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public BigDecimal getRecommendedAmount() {
            return recommendedAmount;
        }

        public void setRecommendedAmount(BigDecimal recommendedAmount) {
            this.recommendedAmount = recommendedAmount;
        }

        public BigDecimal getAverageSpending() {
            return averageSpending;
        }

        public void setAverageSpending(BigDecimal averageSpending) {
            this.averageSpending = averageSpending;
        }

        public BigDecimal getMaxSpending() {
            return maxSpending;
        }

        public void setMaxSpending(BigDecimal maxSpending) {
            this.maxSpending = maxSpending;
        }

        public BigDecimal getMinSpending() {
            return minSpending;
        }

        public void setMinSpending(BigDecimal minSpending) {
            this.minSpending = minSpending;
        }

        public Integer getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(Integer transactionCount) {
            this.transactionCount = transactionCount;
        }

        public Integer getMonthsAnalyzed() {
            return monthsAnalyzed;
        }

        public void setMonthsAnalyzed(Integer monthsAnalyzed) {
            this.monthsAnalyzed = monthsAnalyzed;
        }

        public BigDecimal getCurrentBudget() {
            return currentBudget;
        }

        public void setCurrentBudget(BigDecimal currentBudget) {
            this.currentBudget = currentBudget;
        }

        public BigDecimal getCurrentUsage() {
            return currentUsage;
        }

        public void setCurrentUsage(BigDecimal currentUsage) {
            this.currentUsage = currentUsage;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

