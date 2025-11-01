package com.example.financebackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class UserDataExportDto {

    private Long userId;
    private String email;
    private String fullName;
    private LocalDateTime exportedAt;
    private List<WalletDto> wallets;
    private List<CategoryDto> categories;
    private List<TransactionDto> transactions;
    private List<BudgetDto> budgets;
    private List<RecurringTransactionDto> recurringTransactions;
    private List<FinancialGoalDto> financialGoals;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }

    public List<WalletDto> getWallets() {
        return wallets;
    }

    public void setWallets(List<WalletDto> wallets) {
        this.wallets = wallets;
    }

    public List<CategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories;
    }

    public List<TransactionDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDto> transactions) {
        this.transactions = transactions;
    }

    public List<BudgetDto> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<BudgetDto> budgets) {
        this.budgets = budgets;
    }

    public List<RecurringTransactionDto> getRecurringTransactions() {
        return recurringTransactions;
    }

    public void setRecurringTransactions(List<RecurringTransactionDto> recurringTransactions) {
        this.recurringTransactions = recurringTransactions;
    }

    public List<FinancialGoalDto> getFinancialGoals() {
        return financialGoals;
    }

    public void setFinancialGoals(List<FinancialGoalDto> financialGoals) {
        this.financialGoals = financialGoals;
    }
}

