package com.example.financebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashflowDto {

    private LocalDate date;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;

    public CashflowDto() {
        // Default constructor for Jackson
    }

    public CashflowDto(LocalDate date, BigDecimal income, BigDecimal expense, BigDecimal balance) {
        this.date = date;
        this.income = income;
        this.expense = expense;
        this.balance = balance;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
