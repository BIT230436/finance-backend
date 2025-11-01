package com.example.financebackend.service;

import com.example.financebackend.entity.RecurringTransaction;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.RecurringTransactionRepository;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cashflow Forecast Service
 * Dự đoán cashflow 30 ngày tới dựa trên:
 * - Recurring transactions (income + expenses)
 * - Historical spending patterns
 * - Current balance
 */
@Service
@Transactional(readOnly = true)
public class CashflowForecastService {

    private final WalletRepository walletRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;

    public CashflowForecastService(WalletRepository walletRepository,
                                  RecurringTransactionRepository recurringTransactionRepository,
                                  TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Get cashflow forecast for next 30 days
     */
    public Map<String, Object> getForecast(Long userId, Integer days) {
        if (days == null || days <= 0 || days > 90) {
            days = 30; // Default to 30 days
        }

        // Get current total balance
        BigDecimal currentBalance = walletRepository.findByUserId(userId).stream()
            .map(w -> w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get recurring transactions
        List<RecurringTransaction> recurringTxns = recurringTransactionRepository
            .findByUserId(userId).stream()
            .filter(rt -> Boolean.TRUE.equals(rt.getActive()))
            .collect(Collectors.toList());

        // Calculate average daily spending (last 30 days)
        BigDecimal avgDailySpending = calculateAverageDailySpending(userId);

        // Generate daily forecast
        List<Map<String, Object>> dailyForecast = new ArrayList<>();
        BigDecimal runningBalance = currentBalance;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < days; i++) {
            LocalDate date = today.plusDays(i);
            
            // Calculate expected income/expense for this day
            BigDecimal expectedIncome = BigDecimal.ZERO;
            BigDecimal expectedExpense = avgDailySpending; // Base spending
            
            // Add recurring transactions due on this date
            for (RecurringTransaction rt : recurringTxns) {
                if (isRecurringDueOnDate(rt, date)) {
                    if (rt.getType() == Transaction.TransactionType.INCOME) {
                        expectedIncome = expectedIncome.add(rt.getAmount() != null ? rt.getAmount() : BigDecimal.ZERO);
                    } else {
                        expectedExpense = expectedExpense.add(rt.getAmount() != null ? rt.getAmount() : BigDecimal.ZERO);
                    }
                }
            }
            
            BigDecimal netChange = expectedIncome.subtract(expectedExpense);
            runningBalance = runningBalance.add(netChange);
            
            String zone = getBalanceZone(runningBalance);
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date);
            dayData.put("expectedIncome", expectedIncome);
            dayData.put("expectedExpense", expectedExpense);
            dayData.put("netChange", netChange);
            dayData.put("predictedBalance", runningBalance);
            dayData.put("zone", zone); // GREEN, YELLOW, RED
            
            dailyForecast.add(dayData);
        }

        // Find warning days (balance < threshold)
        BigDecimal warningThreshold = new BigDecimal("1000000"); // 1M VND
        List<LocalDate> warningDays = dailyForecast.stream()
            .filter(day -> {
                BigDecimal balance = (BigDecimal) day.get("predictedBalance");
                return balance.compareTo(warningThreshold) < 0;
            })
            .map(day -> (LocalDate) day.get("date"))
            .collect(Collectors.toList());

        // Calculate minimum balance in forecast period
        BigDecimal minBalance = dailyForecast.stream()
            .map(day -> (BigDecimal) day.get("predictedBalance"))
            .min(BigDecimal::compareTo)
            .orElse(currentBalance);

        Map<String, Object> result = new HashMap<>();
        result.put("currentBalance", currentBalance);
        result.put("forecastDays", days);
        result.put("dailyForecast", dailyForecast);
        result.put("minPredictedBalance", minBalance);
        result.put("warningDays", warningDays);
        result.put("hasWarnings", !warningDays.isEmpty());
        result.put("avgDailySpending", avgDailySpending);
        
        return result;
    }

    private BigDecimal calculateAverageDailySpending(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        List<Transaction> recentExpenses = transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> t.getOccurredAt().isAfter(thirtyDaysAgo))
            .collect(Collectors.toList());

        if (recentExpenses.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalExpense = recentExpenses.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalExpense.divide(new BigDecimal("30"), 2, java.math.RoundingMode.HALF_UP);
    }

    private boolean isRecurringDueOnDate(RecurringTransaction rt, LocalDate date) {
        if (rt.getNextRunDate() == null) {
            return false;
        }

        // Check if this date matches the recurring schedule
        LocalDate current = rt.getNextRunDate();
        
        while (!current.isAfter(date)) {
            if (current.equals(date)) {
                return true;
            }
            
            // Move to next occurrence
            switch (rt.getFrequency()) {
                case DAILY:
                    current = current.plusDays(1);
                    break;
                case WEEKLY:
                    current = current.plusWeeks(1);
                    break;
                case MONTHLY:
                    current = current.plusMonths(1);
                    break;
                case YEARLY:
                    current = current.plusYears(1);
                    break;
            }
            
            // Prevent infinite loop
            if (current.isAfter(date.plusDays(1))) {
                break;
            }
        }
        
        return false;
    }

    private String getBalanceZone(BigDecimal balance) {
        if (balance.compareTo(new BigDecimal("5000000")) >= 0) {
            return "GREEN";  // Safe
        } else if (balance.compareTo(new BigDecimal("1000000")) >= 0) {
            return "YELLOW"; // Caution
        } else {
            return "RED";    // Warning
        }
    }
}

