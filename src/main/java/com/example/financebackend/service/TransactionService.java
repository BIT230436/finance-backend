package com.example.financebackend.service;

import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.BudgetRepository;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import com.example.financebackend.repository.WalletShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WalletShareRepository walletShareRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetService budgetService;
    private final AchievementService achievementService;
    private NotificationService notificationService; // Lazy init to avoid circular dependency

    public TransactionService(TransactionRepository transactionRepository,
                             WalletRepository walletRepository,
                             CategoryRepository categoryRepository,
                             UserRepository userRepository,
                             WalletShareRepository walletShareRepository,
                             BudgetRepository budgetRepository,
                             BudgetService budgetService,
                             AchievementService achievementService) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.walletShareRepository = walletShareRepository;
        this.budgetRepository = budgetRepository;
        this.budgetService = budgetService;
        this.achievementService = achievementService;
    }

    // Setter injection with @Lazy to break circular dependency
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    private boolean hasWalletAccess(Long walletId, Long userId, com.example.financebackend.entity.WalletShare.Permission requiredPermission) {
        // Check ownership
        java.util.Optional<Wallet> wallet = walletRepository.findById(walletId);
        if (wallet.isPresent() && wallet.get().getUser().getId().equals(userId)) {
            return true; // Owner has all permissions
        }
        
        // Check shared permission
        java.util.Optional<com.example.financebackend.entity.WalletShare> share = walletShareRepository.findByWalletIdAndSharedWithUserId(walletId, userId);
        if (share.isPresent()) {
            com.example.financebackend.entity.WalletShare.Permission userPermission = share.get().getPermission();
            // OWNER > EDITOR > VIEWER
            if (requiredPermission == com.example.financebackend.entity.WalletShare.Permission.VIEWER) {
                return true; // Anyone with share can view
            } else if (requiredPermission == com.example.financebackend.entity.WalletShare.Permission.EDITOR) {
                return userPermission == com.example.financebackend.entity.WalletShare.Permission.EDITOR || userPermission == com.example.financebackend.entity.WalletShare.Permission.OWNER;
            } else if (requiredPermission == com.example.financebackend.entity.WalletShare.Permission.OWNER) {
                return userPermission == com.example.financebackend.entity.WalletShare.Permission.OWNER;
            }
        }
        
        return false;
    }

    public List<TransactionDto> findAllByUserId(Long userId) {
        // Get transactions from owned wallets
        List<TransactionDto> ownedTransactions = transactionRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        // Get transactions from shared wallets where user has at least VIEWER permission
        // Note: This requires checking each wallet, can be optimized with a custom query
        // For now, we return owned transactions and let shared wallet transactions be accessed via wallet filtering
        return ownedTransactions;
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> findRecentTransactions(Long userId, int limit) {
        return transactionRepository.findByUserId(userId).stream()
                .sorted((t1, t2) -> t2.getOccurredAt().compareTo(t1.getOccurredAt()))
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> findSimilarTransactions(Long userId, Long categoryId, Long walletId, 
                                                        BigDecimal amount, LocalDateTime occurredAt) {
        List<Transaction> similar = findSimilarTransactionsInternal(userId, categoryId, walletId, amount, occurredAt);
        return similar.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Tạo transaction nhanh với minimal fields (quick entry)
     */
    public TransactionDto createQuick(QuickTransactionRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Auto-fill wallet nếu không có - sử dụng default wallet
        Long walletId = request.getWalletId();
        if (walletId == null) {
            List<Wallet> userWallets = walletRepository.findByUserId(userId);
            Wallet defaultWallet = userWallets.stream()
                    .filter(w -> Boolean.TRUE.equals(w.getDefault()))
                    .findFirst()
                    .orElse(userWallets.isEmpty() ? null : userWallets.get(0));
            
            if (defaultWallet == null) {
                throw new IllegalArgumentException("Không tìm thấy ví mặc định. Vui lòng chọn ví.");
            }
            walletId = defaultWallet.getId();
        }

        // Validate và auto-fill category
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        // Auto-infer type từ category nếu không có
        Transaction.TransactionType type = request.getType();
        if (type == null) {
            type = category.getType() == Category.CategoryType.INCOME 
                    ? Transaction.TransactionType.INCOME 
                    : Transaction.TransactionType.EXPENSE;
        }

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }

        // Check wallet access
        if (!hasWalletAccess(walletId, userId, com.example.financebackend.entity.WalletShare.Permission.EDITOR)) {
            throw new IllegalArgumentException("Không tìm thấy ví hoặc bạn không có quyền tạo giao dịch");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));

        // Tạo transaction DTO với auto-filled values
        TransactionDto dto = new TransactionDto();
        dto.setWalletId(walletId);
        dto.setCategoryId(request.getCategoryId());
        dto.setAmount(request.getAmount());
        dto.setType(type);
        dto.setNote(request.getNote()); // Optional
        dto.setOccurredAt(LocalDateTime.now()); // Default to now

        return create(dto, userId);
    }

    public static class QuickTransactionRequest {
        private BigDecimal amount;
        private Long categoryId;
        private Long walletId; // Optional - will use default if not provided
        private String note; // Optional
        private Transaction.TransactionType type; // Optional - will infer from category

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

        public Long getWalletId() {
            return walletId;
        }

        public void setWalletId(Long walletId) {
            this.walletId = walletId;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public Transaction.TransactionType getType() {
            return type;
        }

        public void setType(Transaction.TransactionType type) {
            this.type = type;
        }
    }

    public List<TransactionDto> findAllByUserIdWithFilters(Long userId, Long categoryId, Long walletId,
                                                            Transaction.TransactionType type,
                                                            java.time.LocalDateTime startDate,
                                                            java.time.LocalDateTime endDate,
                                                            String keyword) {
        return transactionRepository.findByUserIdWithFilters(userId, categoryId, walletId, type, startDate, endDate, keyword)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Advanced search với amount range
     */
    @Transactional(readOnly = true)
    public List<TransactionDto> findAllByUserIdWithAdvancedFilters(Long userId, Long categoryId, Long walletId,
                                                                    Transaction.TransactionType type,
                                                                    LocalDateTime startDate,
                                                                    LocalDateTime endDate,
                                                                    String keyword,
                                                                    BigDecimal minAmount,
                                                                    BigDecimal maxAmount) {
        return transactionRepository.findByUserIdWithFilters(userId, categoryId, walletId, type, startDate, endDate, keyword)
                .stream()
                .filter(t -> {
                    if (minAmount != null && t.getAmount().compareTo(minAmount) < 0) {
                        return false;
                    }
                    if (maxAmount != null && t.getAmount().compareTo(maxAmount) > 0) {
                        return false;
                    }
                    return true;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public TransactionDto findByIdAndUserId(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));
        
        // Check if user owns the transaction OR has access to the wallet (shared)
        boolean canAccess = transaction.getUser().getId().equals(userId) 
                || hasWalletAccess(transaction.getWallet().getId(), userId, com.example.financebackend.entity.WalletShare.Permission.VIEWER);
        
        if (!canAccess) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch hoặc bạn không có quyền truy cập");
        }
        
        return toDto(transaction);
    }

    public TransactionDto create(TransactionDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Check wallet access (owner or EDITOR permission for shared wallet)
        if (!hasWalletAccess(dto.getWalletId(), userId, com.example.financebackend.entity.WalletShare.Permission.EDITOR)) {
            throw new IllegalArgumentException("Không tìm thấy ví hoặc bạn không có quyền tạo giao dịch");
        }
        
        Wallet wallet = walletRepository.findById(dto.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));

        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        if (!category.getType().name().equals(dto.getType().name())) {
            throw new IllegalArgumentException("Loại danh mục phải khớp với loại giao dịch");
        }

        // Validate amount
        if (dto.getAmount() == null) {
            throw new IllegalArgumentException("Số tiền không được để trống");
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }

        // Validate transaction date - cho phép future dates (để lên lịch giao dịch)
        LocalDateTime occurredAt = dto.getOccurredAt() != null ? dto.getOccurredAt() : LocalDateTime.now();
        
        // Duplicate detection - Kiểm tra transaction tương tự
        List<Transaction> similarTransactions = findSimilarTransactionsInternal(userId, dto.getCategoryId(), dto.getWalletId(), 
                                                                        dto.getAmount(), occurredAt);
        if (!similarTransactions.isEmpty()) {
            Transaction similar = similarTransactions.get(0);
            logger.warn("Similar transaction found: transactionId={}, amount={}, date={}, userId={}", 
                       similar.getId(), similar.getAmount(), similar.getOccurredAt(), userId);
            // Không throw exception, chỉ log warning - user có thể vẫn muốn tạo
        }
        
        // Validate currency match
        if (!wallet.getCurrency().equals(category.getUser().getDefaultCurrency())) {
            // Cảnh báo nhưng không chặn (có thể có nhiều loại tiền)
            // Trong tương lai có thể thêm currency conversion
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setWallet(wallet);
        transaction.setCategory(category);
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setNote(dto.getNote());
        transaction.setOccurredAt(occurredAt);
        transaction.setAttachmentUrl(dto.getAttachmentUrl());

        updateWalletBalance(wallet, dto.getAmount(), dto.getType());
        walletRepository.save(wallet);

        Transaction saved = transactionRepository.save(transaction);
        
        // Update budgets related to this transaction
        updateBudgetsForTransaction(saved);
        
        // Create notification for transaction
        createTransactionNotification(saved);
        
        // Check achievements
        achievementService.checkAchievements(userId, com.example.financebackend.entity.Achievement.AchievementType.FIRST_TRANSACTION);
        achievementService.checkAchievements(userId, com.example.financebackend.entity.Achievement.AchievementType.HUNDRED_TRANSACTIONS);
        
        // Check time-based achievements
        int hour = occurredAt.getHour();
        if (hour < 9) {
            achievementService.checkAchievements(userId, com.example.financebackend.entity.Achievement.AchievementType.EARLY_BIRD);
        } else if (hour >= 22) {
            achievementService.checkAchievements(userId, com.example.financebackend.entity.Achievement.AchievementType.NIGHT_OWL);
        }
        
        return toDto(saved);
    }

    public TransactionDto update(Long id, TransactionDto dto, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));
        
        // Check access (owner or has EDITOR permission on wallet)
        boolean canAccess = transaction.getUser().getId().equals(userId) 
                || hasWalletAccess(transaction.getWallet().getId(), userId, com.example.financebackend.entity.WalletShare.Permission.EDITOR);
        
        if (!canAccess) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch hoặc bạn không có quyền chỉnh sửa");
        }

        BigDecimal oldAmount = transaction.getAmount();
        Transaction.TransactionType oldType = transaction.getType();
        Wallet oldWallet = transaction.getWallet();

        // Check new wallet access
        if (!hasWalletAccess(dto.getWalletId(), userId, com.example.financebackend.entity.WalletShare.Permission.EDITOR)) {
            throw new IllegalArgumentException("Không tìm thấy ví hoặc bạn không có quyền sử dụng ví này");
        }
        
        Wallet wallet = walletRepository.findById(dto.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));

        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        if (!category.getType().name().equals(dto.getType().name())) {
            throw new IllegalArgumentException("Loại danh mục phải khớp với loại giao dịch");
        }

        revertWalletBalance(oldWallet, oldAmount, oldType);
        walletRepository.save(oldWallet);

        transaction.setWallet(wallet);
        transaction.setCategory(category);
        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setNote(dto.getNote());
        transaction.setOccurredAt(dto.getOccurredAt() != null ? dto.getOccurredAt() : transaction.getOccurredAt());
        transaction.setAttachmentUrl(dto.getAttachmentUrl());

        updateWalletBalance(wallet, dto.getAmount(), dto.getType());
        walletRepository.save(wallet);

        Transaction saved = transactionRepository.save(transaction);
        
        // Update budgets related to this transaction
        updateBudgetsForTransaction(saved);
        
        return toDto(saved);
    }

    public void delete(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch"));
        
        // Check access (owner or has EDITOR permission on wallet)
        boolean canAccess = transaction.getUser().getId().equals(userId) 
                || hasWalletAccess(transaction.getWallet().getId(), userId, com.example.financebackend.entity.WalletShare.Permission.EDITOR);
        
        if (!canAccess) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch hoặc bạn không có quyền xóa");
        }

        Wallet wallet = transaction.getWallet();
        // Get transaction category before deleting
        Category category = transaction.getCategory();
        LocalDateTime occurredAt = transaction.getOccurredAt();
        
        revertWalletBalance(wallet, transaction.getAmount(), transaction.getType());
        walletRepository.save(wallet);

        transactionRepository.delete(transaction);
        
        // Update budgets after deletion (recalculate based on remaining transactions)
        if (category != null && occurredAt != null) {
            updateBudgetsForCategoryAndDate(category.getId(), transaction.getUser().getId(), occurredAt);
        }
    }

    @Transactional
    public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount, Long userId) {
        try {
            logger.info("Transfer request: fromWalletId={}, toWalletId={}, amount={}, userId={}", 
                       fromWalletId, toWalletId, amount, userId);
            
            // Validate amount
            if (amount == null) {
                throw new IllegalArgumentException("Số tiền không được để trống");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số tiền chuyển phải lớn hơn 0");
            }
            
            // Check both wallets access (must be owner or EDITOR for shared wallets)
            if (!hasWalletAccess(fromWalletId, userId, com.example.financebackend.entity.WalletShare.Permission.EDITOR)) {
                throw new IllegalArgumentException("Không tìm thấy ví nguồn hoặc bạn không có quyền truy cập");
            }
            
            if (!hasWalletAccess(toWalletId, userId, com.example.financebackend.entity.WalletShare.Permission.EDITOR)) {
                throw new IllegalArgumentException("Không tìm thấy ví đích hoặc bạn không có quyền truy cập");
            }
            
            Wallet fromWallet = walletRepository.findById(fromWalletId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví nguồn"));

            Wallet toWallet = walletRepository.findById(toWalletId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví đích"));

            if (fromWalletId.equals(toWalletId)) {
                throw new IllegalArgumentException("Ví nguồn và ví đích không thể giống nhau");
            }

            // Validate currency - handle null cases
            String fromCurrency = fromWallet.getCurrency() != null ? fromWallet.getCurrency() : "VND";
            String toCurrency = toWallet.getCurrency() != null ? toWallet.getCurrency() : "VND";
            
            if (!fromCurrency.equals(toCurrency)) {
                throw new IllegalArgumentException("Ví phải có cùng loại tiền tệ");
            }

            BigDecimal fromBalance = fromWallet.getBalance() != null ? fromWallet.getBalance() : BigDecimal.ZERO;
            BigDecimal toBalance = toWallet.getBalance() != null ? toWallet.getBalance() : BigDecimal.ZERO;
            
            if (fromBalance.compareTo(amount) < 0) {
                throw new IllegalArgumentException(
                    String.format("Số dư không đủ. Số dư hiện tại: %,.0f, Số tiền cần chuyển: %,.0f", 
                                fromBalance.doubleValue(), amount.doubleValue()));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

            // Get or create "Chuyển tiền" category for transfer transactions
            Category transferExpenseCategory = getOrCreateTransferCategory(userId, Category.CategoryType.EXPENSE);
            Category transferIncomeCategory = getOrCreateTransferCategory(userId, Category.CategoryType.INCOME);

            Transaction expense = new Transaction();
            expense.setUser(user);
            expense.setWallet(fromWallet);
            expense.setCategory(transferExpenseCategory);
            expense.setAmount(amount);
            expense.setType(Transaction.TransactionType.EXPENSE);
            expense.setOccurredAt(LocalDateTime.now());
            expense.setNote(String.format("Chuyển tiền đến %s", toWallet.getName()));

            Transaction income = new Transaction();
            income.setUser(user);
            income.setWallet(toWallet);
            income.setCategory(transferIncomeCategory);
            income.setAmount(amount);
            income.setType(Transaction.TransactionType.INCOME);
            income.setOccurredAt(LocalDateTime.now());
            income.setNote(String.format("Nhận tiền từ %s", fromWallet.getName()));

            // Update wallet balances safely with null checks
            fromWallet.setBalance(fromBalance.subtract(amount));
            toWallet.setBalance(toBalance.add(amount));
            
            walletRepository.save(fromWallet);
            walletRepository.save(toWallet);

            Transaction savedExpense = transactionRepository.save(expense);
            Transaction savedIncome = transactionRepository.save(income);
            
            // Create notification for transfer
            createTransferNotification(fromWallet, toWallet, amount, userId);
            
            // Update budgets related to transfer transactions
            updateBudgetsForTransaction(savedExpense);
            updateBudgetsForTransaction(savedIncome);
            
            logger.info("Transfer completed: fromWalletId={}, toWalletId={}, amount={}, userId={}", 
                       fromWalletId, toWalletId, amount, userId);
        } catch (IllegalArgumentException e) {
            logger.error("Transfer failed (validation error): {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Transfer failed (unexpected error): fromWalletId={}, toWalletId={}, amount={}, userId={}", 
                        fromWalletId, toWalletId, amount, userId, e);
            throw new RuntimeException("Đã xảy ra lỗi khi chuyển tiền: " + e.getMessage(), e);
        }
    }

    private void updateWalletBalance(Wallet wallet, BigDecimal amount, Transaction.TransactionType type) {
        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        
        if (type == Transaction.TransactionType.INCOME) {
            wallet.setBalance(currentBalance.add(amount));
        } else {
            BigDecimal newBalance = currentBalance.subtract(amount);
            
            // Cho phép số dư âm nhưng cảnh báo nếu quá lớn (có thể là lỗi)
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                // Nếu số dư âm quá lớn (ví dụ: vượt quá 10 triệu VND), có thể là lỗi
                BigDecimal warningThreshold = new BigDecimal("-10000000");
                if (newBalance.compareTo(warningThreshold) < 0) {
                    throw new IllegalArgumentException(
                        String.format("Số dư sẽ trở thành %,.0f (âm quá lớn). Vui lòng kiểm tra lại số tiền.", 
                                    newBalance.doubleValue()));
                }
                // Cho phép số dư âm nhỏ (overdraft) nhưng log warning
                logger.warn("Wallet {} balance going negative: {} -> {}", 
                           wallet.getId(), currentBalance, newBalance);
            }
            
            wallet.setBalance(newBalance);
        }
    }

    private void revertWalletBalance(Wallet wallet, BigDecimal amount, Transaction.TransactionType type) {
        if (type == Transaction.TransactionType.INCOME) {
            wallet.setBalance(wallet.getBalance().subtract(amount));
        } else {
            wallet.setBalance(wallet.getBalance().add(amount));
        }
    }

    /**
     * Tìm transactions tương tự để detect duplicates (internal method)
     */
    private List<Transaction> findSimilarTransactionsInternal(Long userId, Long categoryId, Long walletId, 
                                                              BigDecimal amount, LocalDateTime occurredAt) {
        // Tìm transactions trong vòng 24 giờ với cùng category, wallet, và amount tương tự (±1%)
        LocalDateTime startTime = occurredAt.minusHours(24);
        LocalDateTime endTime = occurredAt.plusHours(24);
        
        BigDecimal amountMin = amount.multiply(new BigDecimal("0.99")); // -1%
        BigDecimal amountMax = amount.multiply(new BigDecimal("1.01"));  // +1%
        
        return transactionRepository.findByUserId(userId).stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                .filter(t -> t.getWallet() != null && t.getWallet().getId().equals(walletId))
                .filter(t -> {
                    LocalDateTime txDate = t.getOccurredAt();
                    return !txDate.isBefore(startTime) && !txDate.isAfter(endTime);
                })
                .filter(t -> {
                    BigDecimal txAmount = t.getAmount();
                    return txAmount.compareTo(amountMin) >= 0 && txAmount.compareTo(amountMax) <= 0;
                })
                .limit(5) // Chỉ lấy 5 transactions gần nhất
                .collect(Collectors.toList());
    }

    /**
     * Get or create "Chuyển tiền" category for transfer transactions
     */
    private Category getOrCreateTransferCategory(Long userId, Category.CategoryType type) {
        // Try to find existing "Chuyển tiền" category
        List<Category> categories = categoryRepository.findByUserIdAndType(userId, type);
        Category transferCategory = categories.stream()
                .filter(c -> "Chuyển tiền".equalsIgnoreCase(c.getName()))
                .findFirst()
                .orElse(null);
        
        if (transferCategory != null) {
            return transferCategory;
        }
        
        // Create new "Chuyển tiền" category if not exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        
        Category newCategory = new Category();
        newCategory.setUser(user);
        newCategory.setName("Chuyển tiền");
        newCategory.setType(type);
        newCategory.setColor("#9E9E9E"); // Gray color for transfer
        
        return categoryRepository.save(newCategory);
    }

    /**
     * Update budgets related to a transaction
     */
    private void updateBudgetsForTransaction(Transaction transaction) {
        if (transaction == null || transaction.getCategory() == null || transaction.getOccurredAt() == null) {
            return;
        }
        
        // Only update budgets for EXPENSE transactions (budgets are for expenses)
        if (transaction.getType() != Transaction.TransactionType.EXPENSE) {
            return;
        }
        
        Long categoryId = transaction.getCategory().getId();
        Long userId = transaction.getUser().getId();
        LocalDateTime occurredAt = transaction.getOccurredAt();
        
        updateBudgetsForCategoryAndDate(categoryId, userId, occurredAt);
    }

    /**
     * Update budgets for a specific category and date
     */
    private void updateBudgetsForCategoryAndDate(Long categoryId, Long userId, LocalDateTime occurredAt) {
        // Get all budgets for this user and category
        List<com.example.financebackend.entity.Budget> budgets = budgetRepository.findByUserId(userId);
        
        for (com.example.financebackend.entity.Budget budget : budgets) {
            // Only update budgets that match the category
            if (budget.getCategory() == null || !budget.getCategory().getId().equals(categoryId)) {
                continue;
            }
            
            // Check if transaction date is within budget date range
            java.time.LocalDate transactionDate = occurredAt.toLocalDate();
            if (!transactionDate.isBefore(budget.getStartDate()) && !transactionDate.isAfter(budget.getEndDate())) {
                // Update this budget's used amount
                budgetService.updateUsedAmount(budget);
            }
        }
    }

    private TransactionDto toDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setWalletId(transaction.getWallet().getId());
        if (transaction.getCategory() != null) {
            dto.setCategoryId(transaction.getCategory().getId());
        }
        dto.setNote(transaction.getNote());
        dto.setOccurredAt(transaction.getOccurredAt());
        dto.setAttachmentUrl(transaction.getAttachmentUrl());
        return dto;
    }

    /**
     * Create notification when transaction is created
     */
    private void createTransactionNotification(Transaction transaction) {
        if (notificationService == null) {
            return;
        }

        try {
            String typeIcon = transaction.getType() == Transaction.TransactionType.INCOME ? "💰" : "💸";
            String typeText = transaction.getType() == Transaction.TransactionType.INCOME ? "Thu" : "Chi";
            String categoryName = transaction.getCategory() != null ? transaction.getCategory().getName() : "Khác";
            
            String title = String.format("%s Giao dịch %s mới", typeIcon, typeText);
            String message = String.format(
                "Đã ghi nhận giao dịch %s: %,d VND - %s",
                typeText.toLowerCase(),
                transaction.getAmount().longValue(),
                categoryName
            );
            
            if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
                message += " (" + transaction.getNote() + ")";
            }

            notificationService.createSystemNotification(
                transaction.getUser().getId(),
                title,
                message
            );
        } catch (Exception e) {
            logger.error("Failed to create transaction notification: {}", e.getMessage());
        }
    }

    /**
     * Create notification when transferring money between wallets
     */
    private void createTransferNotification(Wallet fromWallet, Wallet toWallet, BigDecimal amount, Long userId) {
        if (notificationService == null) {
            return;
        }

        try {
            String title = "💱 Chuyển tiền giữa ví";
            String message = String.format(
                "Đã chuyển %,d VND từ '%s' sang '%s'",
                amount.longValue(),
                fromWallet.getName(),
                toWallet.getName()
            );

            notificationService.createSystemNotification(userId, title, message);
        } catch (Exception e) {
            logger.error("Failed to create transfer notification: {}", e.getMessage());
        }
    }
}