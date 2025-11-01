package com.example.financebackend.service;

import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comparative Analysis Service
 * So sánh chi tiêu qua các periods:
 * - Month-over-month
 * - Year-over-year
 * - vs. Average (3 months)
 */
@Service
@Transactional(readOnly = true)
public class ComparativeAnalysisService {

    private final TransactionRepository transactionRepository;

    public ComparativeAnalysisService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Compare current month với previous month
     */
    public Map<String, Object> compareMonthOverMonth(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Current month
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime currentMonthEnd = now;
        
        // Previous month
        LocalDateTime prevMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime prevMonthEnd = currentMonthStart.minusSeconds(1);

        Map<String, BigDecimal> currentMonth = calculatePeriodStats(userId, currentMonthStart, currentMonthEnd);
        Map<String, BigDecimal> previousMonth = calculatePeriodStats(userId, prevMonthStart, prevMonthEnd);

        // Calculate variances
        Map<String, Object> result = new HashMap<>();
        result.put("currentMonth", Map.of(
            "period", currentMonthStart.toLocalDate() + " to " + currentMonthEnd.toLocalDate(),
            "income", currentMonth.get("income"),
            "expense", currentMonth.get("expense"),
            "net", currentMonth.get("net"),
            "categoryBreakdown", getCategoryBreakdown(userId, currentMonthStart, currentMonthEnd)
        ));
        
        result.put("previousMonth", Map.of(
            "period", prevMonthStart.toLocalDate() + " to " + prevMonthEnd.toLocalDate(),
            "income", previousMonth.get("income"),
            "expense", previousMonth.get("expense"),
            "net", previousMonth.get("net"),
            "categoryBreakdown", getCategoryBreakdown(userId, prevMonthStart, prevMonthEnd)
        ));

        result.put("variance", Map.of(
            "income", calculateVariance(previousMonth.get("income"), currentMonth.get("income")),
            "expense", calculateVariance(previousMonth.get("expense"), currentMonth.get("expense")),
            "net", calculateVariance(previousMonth.get("net"), currentMonth.get("net"))
        ));

        return result;
    }

    /**
     * Compare with 3-month average
     */
    public Map<String, Object> compareWithAverage(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Current month
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime currentMonthEnd = now;
        
        // Last 3 months (excluding current)
        LocalDateTime threeMonthsAgo = currentMonthStart.minusMonths(3);

        Map<String, BigDecimal> currentMonth = calculatePeriodStats(userId, currentMonthStart, currentMonthEnd);
        Map<String, BigDecimal> lastThreeMonths = calculatePeriodStats(userId, threeMonthsAgo, currentMonthStart.minusSeconds(1));

        // Calculate average (divide by 3)
        BigDecimal avgIncome = lastThreeMonths.get("income").divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpense = lastThreeMonths.get("expense").divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        BigDecimal avgNet = lastThreeMonths.get("net").divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("currentMonth", Map.of(
            "income", currentMonth.get("income"),
            "expense", currentMonth.get("expense"),
            "net", currentMonth.get("net")
        ));
        
        result.put("threeMonthAverage", Map.of(
            "income", avgIncome,
            "expense", avgExpense,
            "net", avgNet
        ));

        result.put("variance", Map.of(
            "income", calculateVariance(avgIncome, currentMonth.get("income")),
            "expense", calculateVariance(avgExpense, currentMonth.get("expense")),
            "net", calculateVariance(avgNet, currentMonth.get("net"))
        ));

        return result;
    }

    /**
     * Year-over-year comparison
     */
    public Map<String, Object> compareYearOverYear(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Current year to date
        LocalDateTime currentYearStart = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime currentYearEnd = now;
        
        // Same period last year
        LocalDateTime lastYearStart = currentYearStart.minusYears(1);
        LocalDateTime lastYearEnd = currentYearEnd.minusYears(1);

        Map<String, BigDecimal> currentYear = calculatePeriodStats(userId, currentYearStart, currentYearEnd);
        Map<String, BigDecimal> lastYear = calculatePeriodStats(userId, lastYearStart, lastYearEnd);

        Map<String, Object> result = new HashMap<>();
        result.put("currentYear", Map.of(
            "period", currentYearStart.getYear() + " (YTD)",
            "income", currentYear.get("income"),
            "expense", currentYear.get("expense"),
            "net", currentYear.get("net")
        ));
        
        result.put("lastYear", Map.of(
            "period", lastYearStart.getYear() + " (same period)",
            "income", lastYear.get("income"),
            "expense", lastYear.get("expense"),
            "net", lastYear.get("net")
        ));

        result.put("variance", Map.of(
            "income", calculateVariance(lastYear.get("income"), currentYear.get("income")),
            "expense", calculateVariance(lastYear.get("expense"), currentYear.get("expense")),
            "net", calculateVariance(lastYear.get("net"), currentYear.get("net"))
        ));

        return result;
    }

    /**
     * Calculate statistics for a period
     */
    private Map<String, BigDecimal> calculatePeriodStats(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
            .filter(t -> !t.getOccurredAt().isBefore(start) && !t.getOccurredAt().isAfter(end))
            .collect(Collectors.toList());

        BigDecimal income = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal net = income.subtract(expense);

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("income", income);
        stats.put("expense", expense);
        stats.put("net", net);
        
        return stats;
    }

    /**
     * Get category breakdown for period
     */
    private Map<String, BigDecimal> getCategoryBreakdown(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
            .filter(t -> !t.getOccurredAt().isBefore(start) && !t.getOccurredAt().isAfter(end))
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.toList());

        Map<String, BigDecimal> breakdown = new HashMap<>();
        
        for (Transaction tx : transactions) {
            Category category = tx.getCategory();
            if (category != null) {
                String categoryName = category.getName();
                BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                breakdown.merge(categoryName, amount, BigDecimal::add);
            }
        }

        return breakdown;
    }

    /**
     * Calculate variance percentage
     */
    private Map<String, Object> calculateVariance(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return Map.of(
                "absolute", current,
                "percentage", 0,
                "direction", "neutral"
            );
        }

        BigDecimal difference = current.subtract(previous);
        BigDecimal percentageChange = difference.divide(previous, 4, RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal("100"));

        String direction = percentageChange.compareTo(BigDecimal.ZERO) > 0 ? "increase" : 
                          percentageChange.compareTo(BigDecimal.ZERO) < 0 ? "decrease" : "neutral";

        return Map.of(
            "absolute", difference,
            "percentage", percentageChange.doubleValue(),
            "direction", direction
        );
    }
}

