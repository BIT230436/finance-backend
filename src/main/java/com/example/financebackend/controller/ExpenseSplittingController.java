package com.example.financebackend.controller;

import com.example.financebackend.service.ExpenseSplittingService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Expense Splitting Controller
 * API để chia bill với bạn bè
 */
@RestController
@RequestMapping("/api/split-expenses")
public class ExpenseSplittingController {

    private final ExpenseSplittingService splittingService;

    public ExpenseSplittingController(ExpenseSplittingService splittingService) {
        this.splittingService = splittingService;
    }

    /**
     * Create new split expense
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Map<String, Object> createSplitExpense(@Valid @RequestBody CreateSplitExpenseRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        
        return splittingService.createSplitExpense(
            request.getDescription(),
            request.getTotalAmount(),
            request.getParticipantUserIds(),
            request.getCustomAmounts(),
            userId
        );
    }

    /**
     * Get user's split expenses (as creator or participant)
     */
    @GetMapping
    public List<Map<String, Object>> getUserSplitExpenses() {
        Long userId = AuthUtil.getCurrentUserId();
        return splittingService.getUserSplitExpenses(userId);
    }

    /**
     * Get pending payments (amounts user owes)
     */
    @GetMapping("/pending")
    public List<Map<String, Object>> getPendingPayments() {
        Long userId = AuthUtil.getCurrentUserId();
        return splittingService.getPendingPayments(userId);
    }

    /**
     * Mark as paid
     */
    @PutMapping("/{splitExpenseId}/mark-paid")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Map<String, Object> markAsPaid(@PathVariable Long splitExpenseId) {
        Long userId = AuthUtil.getCurrentUserId();
        return splittingService.markAsPaid(splitExpenseId, userId);
    }

    public static class CreateSplitExpenseRequest {
        @NotBlank(message = "Mô tả là bắt buộc")
        private String description;

        @NotNull(message = "Tổng số tiền là bắt buộc")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        private BigDecimal totalAmount;

        @NotNull(message = "Danh sách người tham gia là bắt buộc")
        private List<Long> participantUserIds;

        private Map<Long, BigDecimal> customAmounts; // Optional: custom split amounts

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public List<Long> getParticipantUserIds() {
            return participantUserIds;
        }

        public void setParticipantUserIds(List<Long> participantUserIds) {
            this.participantUserIds = participantUserIds;
        }

        public Map<Long, BigDecimal> getCustomAmounts() {
            return customAmounts;
        }

        public void setCustomAmounts(Map<Long, BigDecimal> customAmounts) {
            this.customAmounts = customAmounts;
        }
    }
}

