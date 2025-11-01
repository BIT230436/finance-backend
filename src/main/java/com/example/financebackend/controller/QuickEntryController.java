package com.example.financebackend.controller;

import com.example.financebackend.dto.CategoryDto;
import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.dto.WalletDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.service.CategoryService;
import com.example.financebackend.service.TransactionService;
import com.example.financebackend.service.WalletService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quick Entry Controller
 * Optimized for mobile với 3-tap entry
 */
@RestController
@RequestMapping("/api/quick-entry")
public class QuickEntryController {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final CategoryService categoryService;

    public QuickEntryController(TransactionService transactionService,
                               WalletService walletService,
                               CategoryService categoryService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.categoryService = categoryService;
    }

    /**
     * Get quick entry defaults (wallet + recently used categories)
     * Frontend có thể cache này để tăng tốc
     */
    @GetMapping("/defaults")
    public Map<String, Object> getQuickEntryDefaults() {
        Long userId = AuthUtil.getCurrentUserId();
        
        // Get default wallet
        List<WalletDto> wallets = walletService.findAllByUserId(userId);
        WalletDto defaultWallet = wallets.stream()
            .filter(w -> Boolean.TRUE.equals(w.getIsDefault()))
            .findFirst()
            .orElse(wallets.isEmpty() ? null : wallets.get(0));

        // Get recently used categories (last 10 transactions)
        List<TransactionDto> recentTransactions = transactionService.findRecentTransactions(userId, 10);
        List<Long> recentCategoryIds = recentTransactions.stream()
            .map(TransactionDto::getCategoryId)
            .distinct()
            .limit(5)
            .collect(java.util.stream.Collectors.toList());

        List<CategoryDto> recentCategories = recentCategoryIds.stream()
            .map(id -> {
                try {
                    return categoryService.findByIdAndUserId(id, userId);
                } catch (Exception e) {
                    return null;
                }
            })
            .filter(c -> c != null)
            .collect(java.util.stream.Collectors.toList());

        // Get all categories grouped by type
        List<CategoryDto> expenseCategories = categoryService.findByUserIdAndType(userId, Category.CategoryType.EXPENSE);
        List<CategoryDto> incomeCategories = categoryService.findByUserIdAndType(userId, Category.CategoryType.INCOME);

        Map<String, Object> result = new HashMap<>();
        result.put("defaultWallet", defaultWallet);
        result.put("allWallets", wallets);
        result.put("recentCategories", recentCategories);
        result.put("expenseCategories", expenseCategories);
        result.put("incomeCategories", incomeCategories);
        
        return result;
    }

    /**
     * Super quick entry - chỉ cần amount và categoryId
     * Tự động fill wallet, type, date
     */
    @PostMapping("/super-quick")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDto superQuickEntry(@Valid @RequestBody SuperQuickRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        
        TransactionService.QuickTransactionRequest quickRequest = new TransactionService.QuickTransactionRequest();
        quickRequest.setAmount(request.getAmount());
        quickRequest.setCategoryId(request.getCategoryId());
        quickRequest.setNote(request.getNote());
        // walletId và type sẽ được auto-fill trong TransactionService
        
        return transactionService.createQuick(quickRequest, userId);
    }

    /**
     * Voice entry - parse text thành transaction
     * Example: "Spent 50k on food" → amount=50000, category=Ăn uống
     */
    @PostMapping("/voice")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDto voiceEntry(@RequestBody VoiceEntryRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        
        // Parse voice text (simplified version)
        ParsedVoiceEntry parsed = parseVoiceEntry(request.getText());
        
        TransactionService.QuickTransactionRequest quickRequest = new TransactionService.QuickTransactionRequest();
        quickRequest.setAmount(parsed.amount);
        quickRequest.setCategoryId(parsed.categoryId);
        quickRequest.setNote(request.getText());
        quickRequest.setType(parsed.type);
        
        return transactionService.createQuick(quickRequest, userId);
    }

    /**
     * Batch quick entry - thêm nhiều transactions cùng lúc
     * Useful cho catch-up logging
     */
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Map<String, Object> batchEntry(@Valid @RequestBody List<SuperQuickRequest> requests) {
        Long userId = AuthUtil.getCurrentUserId();
        
        List<TransactionDto> created = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();
        
        for (int i = 0; i < requests.size(); i++) {
            try {
                SuperQuickRequest req = requests.get(i);
                TransactionService.QuickTransactionRequest quickRequest = new TransactionService.QuickTransactionRequest();
                quickRequest.setAmount(req.getAmount());
                quickRequest.setCategoryId(req.getCategoryId());
                quickRequest.setNote(req.getNote());
                
                TransactionDto result = transactionService.createQuick(quickRequest, userId);
                created.add(result);
            } catch (Exception e) {
                errors.add("Transaction #" + i + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("errors", errors);
        result.put("successCount", created.size());
        result.put("errorCount", errors.size());
        
        return result;
    }

    private ParsedVoiceEntry parseVoiceEntry(String text) {
        // Simplified voice parsing
        // In production, use NLP service (Google Cloud Speech-to-Text, etc.)
        
        ParsedVoiceEntry entry = new ParsedVoiceEntry();
        entry.amount = BigDecimal.ZERO;
        entry.categoryId = null; // Will need to be set by user or AI suggestion
        entry.type = Transaction.TransactionType.EXPENSE; // Default
        
        // TODO: Implement sophisticated NLP parsing
        // For now, return defaults
        
        return entry;
    }

    public static class SuperQuickRequest {
        @NotNull(message = "Số tiền là bắt buộc")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        private BigDecimal amount;

        @NotNull(message = "Danh mục là bắt buộc")
        private Long categoryId;

        private String note;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class VoiceEntryRequest {
        @NotNull(message = "Text là bắt buộc")
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    private static class ParsedVoiceEntry {
        BigDecimal amount;
        Long categoryId;
        Transaction.TransactionType type;
    }
}

