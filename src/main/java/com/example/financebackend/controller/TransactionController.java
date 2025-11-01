package com.example.financebackend.controller;

import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.service.CategorySuggestionService;
import com.example.financebackend.service.TransactionService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CategorySuggestionService categorySuggestionService;

    public TransactionController(TransactionService transactionService,
                                CategorySuggestionService categorySuggestionService) {
        this.transactionService = transactionService;
        this.categorySuggestionService = categorySuggestionService;
    }

    @GetMapping
    public List<TransactionDto> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String datePreset) {
        Long userId = AuthUtil.getCurrentUserId();
        
        // Handle date presets (This week, This month, This year, etc.)
        if (datePreset != null && (startDate == null || endDate == null)) {
            LocalDateTime[] dateRange = parseDatePreset(datePreset);
            if (dateRange != null) {
                startDate = dateRange[0];
                endDate = dateRange[1];
            }
        }
        
        return transactionService.findAllByUserIdWithAdvancedFilters(
                userId, categoryId, walletId, type, startDate, endDate, keyword, minAmount, maxAmount);
    }
    
    private LocalDateTime[] parseDatePreset(String preset) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start, end;
        
        switch (preset.toLowerCase()) {
            case "today":
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case "yesterday":
                java.time.LocalDate yesterday = now.toLocalDate().minusDays(1);
                start = yesterday.atStartOfDay();
                end = yesterday.atTime(23, 59, 59);
                break;
            case "thisweek":
                java.time.DayOfWeek dayOfWeek = now.getDayOfWeek();
                int daysToSubtract = dayOfWeek.getValue() - 1; // Monday = 0
                start = now.toLocalDate().minusDays(daysToSubtract).atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case "thismonth":
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case "thisyear":
                start = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case "last30days":
                start = now.minusDays(30).toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            case "last3months":
                start = now.minusMonths(3).toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().atTime(23, 59, 59);
                break;
            default:
                return null;
        }
        
        return new LocalDateTime[]{start, end};
    }

    @GetMapping("/{id}")
    public TransactionDto get(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return transactionService.findByIdAndUserId(id, userId);
    }

    @GetMapping("/suggestions/categories")
    public List<CategorySuggestionResponse> getCategorySuggestions(
            @RequestParam(required = false) String note,
            @RequestParam(required = false) Transaction.TransactionType type) {
        Long userId = AuthUtil.getCurrentUserId();
        if (type == null) {
            type = Transaction.TransactionType.EXPENSE; // Default to expense
        }
        return categorySuggestionService.suggestCategories(note, type, userId).stream()
                .map(s -> {
                    CategorySuggestionResponse response = new CategorySuggestionResponse();
                    response.setCategoryId(s.getCategoryId());
                    response.setCategoryName(s.getCategoryName());
                    response.setScore(s.getScore());
                    response.setConfidence(s.getConfidence());
                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/suggestions/amounts")
    public List<java.math.BigDecimal> getAmountSuggestions(@RequestParam Long categoryId) {
        Long userId = AuthUtil.getCurrentUserId();
        return categorySuggestionService.suggestAmounts(categoryId, userId);
    }

    @GetMapping("/recent")
    public List<TransactionDto> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = AuthUtil.getCurrentUserId();
        return transactionService.findRecentTransactions(userId, limit);
    }

    @GetMapping("/duplicates")
    public List<DuplicateTransactionResponse> checkDuplicates(
            @RequestParam Long categoryId,
            @RequestParam Long walletId,
            @RequestParam java.math.BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        Long userId = AuthUtil.getCurrentUserId();
        LocalDateTime checkDate = date != null ? date : LocalDateTime.now();
        List<TransactionDto> duplicates = transactionService.findSimilarTransactions(userId, categoryId, walletId, amount, checkDate);
        return duplicates.stream()
                .map(t -> {
                    DuplicateTransactionResponse response = new DuplicateTransactionResponse();
                    response.setTransactionId(t.getId());
                    response.setAmount(t.getAmount());
                    response.setNote(t.getNote());
                    response.setOccurredAt(t.getOccurredAt());
                    response.setCategoryId(t.getCategoryId());
                    response.setWalletId(t.getWalletId());
                    return response;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDto create(@Valid @RequestBody TransactionDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return transactionService.create(dto, userId);
    }

    @PostMapping("/quick")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDto createQuick(@RequestBody com.example.financebackend.service.TransactionService.QuickTransactionRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return transactionService.createQuick(request, userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDto update(@PathVariable Long id, @Valid @RequestBody TransactionDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return transactionService.update(id, dto, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        transactionService.delete(id, userId);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void transfer(@Valid @RequestBody TransferRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        transactionService.transfer(
                request.getFromWalletId(),
                request.getToWalletId(),
                request.getAmount(),
                userId
        );
    }

    public static class TransferRequest {
        @NotNull(message = "Ví nguồn là bắt buộc")
        private Long fromWalletId;

        @NotNull(message = "Ví đích là bắt buộc")
        private Long toWalletId;

        @NotNull(message = "Số tiền là bắt buộc")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        private BigDecimal amount;

        public Long getFromWalletId() {
            return fromWalletId;
        }

        public void setFromWalletId(Long fromWalletId) {
            this.fromWalletId = fromWalletId;
        }

        public Long getToWalletId() {
            return toWalletId;
        }

        public void setToWalletId(Long toWalletId) {
            this.toWalletId = toWalletId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class CategorySuggestionResponse {
        private Long categoryId;
        private String categoryName;
        private Integer score;
        private String confidence;

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

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }
    }

    public static class DuplicateTransactionResponse {
        private Long transactionId;
        private java.math.BigDecimal amount;
        private String note;
        private LocalDateTime occurredAt;
        private Long categoryId;
        private Long walletId;

        public Long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(Long transactionId) {
            this.transactionId = transactionId;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }

        public void setOccurredAt(LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public Long getWalletId() {
            return walletId;
        }

        public void setWalletId(Long walletId) {
            this.walletId = walletId;
        }
    }
}