package com.example.financebackend.service;

import com.example.financebackend.entity.Budget;
import com.example.financebackend.entity.FinancialGoal;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Financial Health Score Service
 * 
 * Calculate financial health score (0-100) based on:
 * - Budget adherence (30%)
 * - Savings rate (25%)
 * - Net worth growth (25%)
 * - Consistency (20%)
 */
@Service
@Transactional(readOnly = true)
public class FinancialHealthScoreService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialHealthScoreService.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final WalletRepository walletRepository;
    private final FinancialGoalRepository financialGoalRepository;

    public FinancialHealthScoreService(UserRepository userRepository,
                                      TransactionRepository transactionRepository,
                                      BudgetRepository budgetRepository,
                                      WalletRepository walletRepository,
                                      FinancialGoalRepository financialGoalRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.walletRepository = walletRepository;
        this.financialGoalRepository = financialGoalRepository;
    }

    /**
     * Calculate financial health score for user
     */
    public Map<String, Object> calculateHealthScore(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Calculate các component scores
        double budgetScore = calculateBudgetAdherenceScore(userId);      // 30%
        double savingsScore = calculateSavingsRateScore(userId);         // 25%
        double growthScore = calculateNetWorthGrowthScore(userId);       // 25%
        double consistencyScore = calculateConsistencyScore(userId);     // 20%

        // Weighted total score
        double totalScore = (budgetScore * 0.30) + 
                          (savingsScore * 0.25) + 
                          (growthScore * 0.25) + 
                          (consistencyScore * 0.20);

        // Round to integer
        int finalScore = (int) Math.round(totalScore);
        
        // Determine health level
        String healthLevel = getHealthLevel(finalScore);
        String healthColor = getHealthColor(finalScore);
        
        // Generate recommendations
        List<String> recommendations = generateRecommendations(
            budgetScore, savingsScore, growthScore, consistencyScore);

        Map<String, Object> result = new HashMap<>();
        result.put("score", finalScore);
        result.put("healthLevel", healthLevel);
        result.put("healthColor", healthColor);
        result.put("components", Map.of(
            "budgetAdherence", Map.of("score", (int) budgetScore, "weight", "30%"),
            "savingsRate", Map.of("score", (int) savingsScore, "weight", "25%"),
            "netWorthGrowth", Map.of("score", (int) growthScore, "weight", "25%"),
            "consistency", Map.of("score", (int) consistencyScore, "weight", "20%")
        ));
        result.put("recommendations", recommendations);
        result.put("calculatedAt", LocalDateTime.now());

        logger.debug("Calculated health score for user {}: {}", userId, finalScore);
        return result;
    }

    /**
     * Budget Adherence Score (30%)
     * Tính dựa trên % budgets được tuân thủ trong 3 tháng gần nhất
     */
    private double calculateBudgetAdherenceScore(Long userId) {
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        
        List<Budget> recentBudgets = budgetRepository.findByUserId(userId).stream()
            .filter(b -> b.getEndDate().isAfter(threeMonthsAgo))
            .collect(Collectors.toList());

        if (recentBudgets.isEmpty()) {
            return 50; // Neutral score nếu chưa có budget
        }

        long adherentBudgets = recentBudgets.stream()
            .filter(b -> {
                BigDecimal used = b.getUsedAmount() != null ? b.getUsedAmount() : BigDecimal.ZERO;
                BigDecimal limit = b.getLimitAmount() != null ? b.getLimitAmount() : BigDecimal.ONE;
                return used.compareTo(limit) <= 0; // Within budget
            })
            .count();

        double adherenceRate = (double) adherentBudgets / recentBudgets.size();
        return adherenceRate * 100; // 0-100
    }

    /**
     * Savings Rate Score (25%)
     * Tính dựa trên % thu nhập được tiết kiệm mỗi tháng
     */
    private double calculateSavingsRateScore(Long userId) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        LocalDateTime now = LocalDateTime.now();

        List<Transaction> recentTransactions = transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getOccurredAt().isAfter(oneMonthAgo) && t.getOccurredAt().isBefore(now))
            .collect(Collectors.toList());

        BigDecimal income = recentTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = recentTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return 50; // Neutral nếu chưa có income
        }

        BigDecimal savings = income.subtract(expense);
        BigDecimal savingsRate = savings.divide(income, 4, RoundingMode.HALF_UP)
                                       .multiply(new BigDecimal("100"));

        // Convert to score: 20%+ savings = 100, 0% = 50, negative = 0
        double score = Math.min(100, Math.max(0, 50 + (savingsRate.doubleValue() * 2.5)));
        return score;
    }

    /**
     * Net Worth Growth Score (25%)
     * Tính dựa trên tổng balance của tất cả wallets so với 3 tháng trước
     */
    private double calculateNetWorthGrowthScore(Long userId) {
        // Current net worth (total balance across all wallets)
        BigDecimal currentNetWorth = walletRepository.findByUserId(userId).stream()
            .map(w -> w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Estimate net worth 3 months ago by subtracting recent net changes
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        BigDecimal recentNetChange = transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getOccurredAt().isAfter(threeMonthsAgo))
            .map(t -> {
                if (t.getType() == Transaction.TransactionType.INCOME) {
                    return t.getAmount();
                } else {
                    return t.getAmount().negate();
                }
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pastNetWorth = currentNetWorth.subtract(recentNetChange);

        if (pastNetWorth.compareTo(BigDecimal.ZERO) <= 0) {
            // If past net worth was zero or negative, give score based on current
            return currentNetWorth.compareTo(BigDecimal.ZERO) > 0 ? 75 : 50;
        }

        // Calculate growth rate
        BigDecimal growthRate = currentNetWorth.subtract(pastNetWorth)
                                              .divide(pastNetWorth, 4, RoundingMode.HALF_UP)
                                              .multiply(new BigDecimal("100"));

        // Convert to score: 10%+ growth = 100, 0% = 50, negative = 0
        double score = Math.min(100, Math.max(0, 50 + (growthRate.doubleValue() * 5)));
        return score;
    }

    /**
     * Consistency Score (20%)
     * Tính dựa trên số ngày ghi transaction trong 30 ngày gần nhất
     */
    private double calculateConsistencyScore(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        List<Transaction> recentTransactions = transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getOccurredAt().isAfter(thirtyDaysAgo))
            .collect(Collectors.toList());

        if (recentTransactions.isEmpty()) {
            return 25; // Low score nếu không có transactions
        }

        // Count unique days with transactions
        long uniqueDays = recentTransactions.stream()
            .map(t -> t.getOccurredAt().toLocalDate())
            .distinct()
            .count();

        // Score: 20+ days = 100, 10 days = 50, 0 days = 0
        double score = Math.min(100, (uniqueDays / 20.0) * 100);
        return score;
    }

    private String getHealthLevel(int score) {
        if (score >= 80) return "Xuất sắc";
        if (score >= 60) return "Tốt";
        if (score >= 40) return "Trung bình";
        if (score >= 20) return "Cần cải thiện";
        return "Yếu";
    }

    private String getHealthColor(int score) {
        if (score >= 80) return "#4CAF50"; // Green
        if (score >= 60) return "#8BC34A"; // Light green
        if (score >= 40) return "#FFC107"; // Yellow
        if (score >= 20) return "#FF9800"; // Orange
        return "#F44336"; // Red
    }

    private List<String> generateRecommendations(double budgetScore, double savingsScore, 
                                                 double growthScore, double consistencyScore) {
        List<String> recommendations = new java.util.ArrayList<>();

        if (budgetScore < 60) {
            recommendations.add("💼 Hãy tạo và tuân thủ ngân sách chặt chẽ hơn để kiểm soát chi tiêu");
        }

        if (savingsScore < 60) {
            recommendations.add("💰 Cố gắng tiết kiệm ít nhất 20% thu nhập mỗi tháng");
        }

        if (growthScore < 60) {
            recommendations.add("📈 Tập trung tăng thu nhập hoặc giảm chi tiêu để cải thiện tài sản ròng");
        }

        if (consistencyScore < 60) {
            recommendations.add("📝 Hãy ghi chép giao dịch đều đặn hơn để theo dõi tài chính tốt hơn");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("🎉 Tuyệt vời! Bạn đang quản lý tài chính rất tốt. Hãy tiếp tục duy trì!");
        }

        return recommendations;
    }
}

