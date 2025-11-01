package com.example.financebackend.service;

import com.example.financebackend.dto.CashflowDto;
import com.example.financebackend.dto.ReportSummaryDto;
import com.example.financebackend.dto.WalletSummaryDto;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public ReportSummaryDto getSummary(Long userId, LocalDateTime from, LocalDateTime to) {
        // Get all transactions for user with date filtering
        List<Transaction> allTransactions = transactionRepository.findByUserId(userId);
        List<Transaction> transactions = allTransactions.stream()
                .filter(tx -> {
                    if (tx.getOccurredAt() == null) {
                        return false; // Skip transactions without date
                    }
                    LocalDateTime occurredAt = tx.getOccurredAt();
                    return (from == null || !occurredAt.isBefore(from)) 
                        && (to == null || !occurredAt.isAfter(to));
                })
                .collect(Collectors.toList());

        BigDecimal totalIncome = transactions.stream()
                .filter(tx -> tx.getType() == Transaction.TransactionType.INCOME && tx.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(tx -> tx.getType() == Transaction.TransactionType.EXPENSE && tx.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, CategorySummary> categoryMap = new HashMap<>();
        for (Transaction tx : transactions) {
            // Null safety checks
            if (tx.getCategory() == null || tx.getAmount() == null || tx.getType() == null) {
                continue; // Skip invalid transactions
            }
            
            String key = tx.getCategory().getId() + "_" + tx.getType().name();
            categoryMap.computeIfAbsent(key, k -> new CategorySummary(
                    tx.getCategory().getId(),
                    tx.getCategory().getName() != null ? tx.getCategory().getName() : "Unknown",
                    tx.getType().name()
            )).addAmount(tx.getAmount());
        }

        ReportSummaryDto dto = new ReportSummaryDto();
        dto.setTotalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO);
        dto.setTotalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO);
        dto.setBalance((totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO));
        dto.setTransactionCount((long) transactions.size());
        
        // Filter out categories with zero amount và ensure proper formatting
        List<ReportSummaryDto.CategorySummaryDto> categorySummaries = categoryMap.values().stream()
                .filter(cs -> cs.amount != null && cs.amount.compareTo(BigDecimal.ZERO) > 0) // Only include non-zero amounts
                .map(cs -> {
                    ReportSummaryDto.CategorySummaryDto catDto = new ReportSummaryDto.CategorySummaryDto();
                    catDto.setCategoryId(cs.categoryId);
                    catDto.setCategoryName(cs.categoryName != null ? cs.categoryName : "Unknown");
                    catDto.setAmount(cs.amount != null ? cs.amount : BigDecimal.ZERO);
                    catDto.setType(cs.type != null ? cs.type : "EXPENSE");
                    return catDto;
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount())) // Sort by amount descending
                .collect(Collectors.toList());
        
        dto.setCategorySummaries(categorySummaries);

        return dto;
    }

    private static class CategorySummary {
        Long categoryId;
        String categoryName;
        BigDecimal amount = BigDecimal.ZERO;
        String type;

        CategorySummary(Long categoryId, String categoryName, String type) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.type = type;
        }

        void addAmount(BigDecimal amount) {
            if (amount != null) {
                this.amount = this.amount.add(amount);
            }
        }
    }

    public List<CashflowDto> getCashflow(Long userId, LocalDateTime from, LocalDateTime to) {
        // Get all transactions for user with date filtering
        List<Transaction> allTransactions = transactionRepository.findByUserId(userId);
        List<Transaction> transactions = allTransactions.stream()
                .filter(tx -> {
                    if (tx.getOccurredAt() == null) {
                        return false; // Skip transactions without date
                    }
                    LocalDateTime occurredAt = tx.getOccurredAt();
                    return (from == null || !occurredAt.isBefore(from))
                        && (to == null || !occurredAt.isAfter(to));
                })
                .collect(Collectors.toList());

        Map<LocalDate, CashflowData> cashflowMap = new HashMap<>();
        for (Transaction tx : transactions) {
            // Null safety checks
            if (tx.getOccurredAt() == null || tx.getType() == null || tx.getAmount() == null) {
                continue; // Skip invalid transactions
            }
            
            LocalDate date = tx.getOccurredAt().toLocalDate();
            CashflowData data = cashflowMap.computeIfAbsent(date, k -> new CashflowData(date));

            BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            if (tx.getType() == Transaction.TransactionType.INCOME) {
                data.income = data.income.add(amount);
            } else {
                data.expense = data.expense.add(amount);
            }
        }

        // Sort by date và tính cumulative balance
        List<CashflowDto> cashflowList = cashflowMap.values().stream()
                .sorted((a, b) -> a.date.compareTo(b.date))
                .map(data -> {
                    BigDecimal income = data.income != null ? data.income : BigDecimal.ZERO;
                    BigDecimal expense = data.expense != null ? data.expense : BigDecimal.ZERO;
                    BigDecimal net = income.subtract(expense);
                    return new CashflowDto(data.date, income, expense, net);
                })
                .collect(Collectors.toList());
        
        // Tính cumulative balance (running balance) - balance là tổng số dư từ đầu đến ngày đó
        BigDecimal runningBalance = BigDecimal.ZERO;
        for (CashflowDto dto : cashflowList) {
            BigDecimal net = dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO;
            runningBalance = runningBalance.add(net);
            dto.setBalance(runningBalance);
        }
        
        // Ensure we have at least some data points for chart rendering
        if (cashflowList.isEmpty() && from != null && to != null) {
            // Return at least one data point to prevent empty chart
            LocalDate startDate = from.toLocalDate();
            cashflowList.add(new CashflowDto(startDate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        
        return cashflowList;
    }

    private static class CashflowData {
        LocalDate date;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        CashflowData(LocalDate date) {
            this.date = date;
        }
    }

    public List<WalletSummaryDto> getWalletSummary(Long userId, LocalDateTime from, LocalDateTime to) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
                .filter(tx -> {
                    LocalDateTime occurredAt = tx.getOccurredAt();
                    return (from == null || !occurredAt.isBefore(from))
                        && (to == null || !occurredAt.isAfter(to));
                })
                .collect(Collectors.toList());

        Map<Long, WalletSummary> walletMap = new HashMap<>();
        for (Transaction tx : transactions) {
            // Null safety checks
            if (tx.getWallet() == null || tx.getType() == null || tx.getAmount() == null) {
                continue; // Skip invalid transactions
            }
            
            Long walletId = tx.getWallet().getId();
            WalletSummary summary = walletMap.computeIfAbsent(walletId, k -> new WalletSummary(
                    walletId,
                    tx.getWallet().getName() != null ? tx.getWallet().getName() : "Unknown",
                    tx.getWallet().getCurrency() != null ? tx.getWallet().getCurrency() : "VND"
            ));

            BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
            if (tx.getType() == Transaction.TransactionType.INCOME) {
                summary.totalIncome = summary.totalIncome.add(amount);
            } else {
                summary.totalExpense = summary.totalExpense.add(amount);
            }
            summary.transactionCount++;
        }

        return walletMap.values().stream()
                .map(ws -> {
                    BigDecimal totalIncome = ws.totalIncome != null ? ws.totalIncome : BigDecimal.ZERO;
                    BigDecimal totalExpense = ws.totalExpense != null ? ws.totalExpense : BigDecimal.ZERO;
                    BigDecimal balance = totalIncome.subtract(totalExpense);
                    return new WalletSummaryDto(
                            ws.walletId,
                            ws.walletName,
                            ws.currency,
                            totalIncome,
                            totalExpense,
                            balance,
                            ws.transactionCount
                    );
                })
                .collect(Collectors.toList());
    }

    private static class WalletSummary {
        Long walletId;
        String walletName;
        String currency;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Long transactionCount = 0L;

        WalletSummary(Long walletId, String walletName, String currency) {
            this.walletId = walletId;
            this.walletName = walletName;
            this.currency = currency;
        }
    }
}