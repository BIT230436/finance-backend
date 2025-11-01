package com.example.financebackend.dto;

import com.example.financebackend.entity.Budget.Period;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BudgetDto {

    private Long id;

    @NotNull(message = "Danh mục là bắt buộc")
    private Long categoryId;

    @NotNull(message = "Chu kỳ là bắt buộc")
    private Period period;

    @NotNull(message = "Ngày bắt đầu là bắt buộc")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc là bắt buộc")
    private LocalDate endDate;

    @NotNull(message = "Hạn mức là bắt buộc")
    @DecimalMin(value = "0.01", message = "Hạn mức phải lớn hơn 0")
    private BigDecimal limitAmount;

    @DecimalMin(value = "0.0", message = "Ngưỡng cảnh báo phải >= 0")
    @DecimalMax(value = "1.0", message = "Ngưỡng cảnh báo phải <= 1")
    private BigDecimal alertThreshold;

    private BigDecimal usedAmount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
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

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public BigDecimal getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(BigDecimal alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }
}
