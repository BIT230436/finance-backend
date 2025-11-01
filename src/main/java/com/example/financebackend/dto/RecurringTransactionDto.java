package com.example.financebackend.dto;

import com.example.financebackend.entity.RecurringTransaction;
import com.example.financebackend.entity.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecurringTransactionDto {

    private Long id;

    @NotNull(message = "Ví là bắt buộc")
    private Long walletId;

    @NotNull(message = "Danh mục là bắt buộc")
    private Long categoryId;

    @NotNull(message = "Số tiền là bắt buộc")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Loại giao dịch là bắt buộc")
    private Transaction.TransactionType type;

    @NotNull(message = "Tần suất là bắt buộc")
    private RecurringTransaction.Frequency frequency;

    @NotNull(message = "Ngày bắt đầu là bắt buộc")
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate nextRunDate;

    private Boolean active;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Transaction.TransactionType getType() {
        return type;
    }

    public void setType(Transaction.TransactionType type) {
        this.type = type;
    }

    public RecurringTransaction.Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(RecurringTransaction.Frequency frequency) {
        this.frequency = frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public void setNextRunDate(LocalDate nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

