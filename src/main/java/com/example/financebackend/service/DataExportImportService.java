package com.example.financebackend.service;

import com.example.financebackend.dto.*;
import com.example.financebackend.entity.*;
import com.example.financebackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DataExportImportService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final RecurringTransactionService recurringTransactionService;
    private final FinancialGoalService financialGoalService;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final FinancialGoalRepository financialGoalRepository;

    public DataExportImportService(UserRepository userRepository,
                                  WalletService walletService,
                                  CategoryService categoryService,
                                  TransactionService transactionService,
                                  BudgetService budgetService,
                                  RecurringTransactionService recurringTransactionService,
                                  FinancialGoalService financialGoalService,
                                  WalletRepository walletRepository,
                                  CategoryRepository categoryRepository,
                                  TransactionRepository transactionRepository,
                                  BudgetRepository budgetRepository,
                                  RecurringTransactionRepository recurringTransactionRepository,
                                  FinancialGoalRepository financialGoalRepository) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.recurringTransactionService = recurringTransactionService;
        this.financialGoalService = financialGoalService;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.financialGoalRepository = financialGoalRepository;
    }

    @Transactional(readOnly = true)
    public UserDataExportDto exportUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        UserDataExportDto export = new UserDataExportDto();
        export.setUserId(user.getId());
        export.setEmail(user.getEmail());
        export.setFullName(user.getFullName());
        export.setExportedAt(LocalDateTime.now());
        
        // Export wallets
        export.setWallets(walletService.findAllByUserId(userId));
        
        // Export categories
        export.setCategories(categoryService.findAllByUserId(userId));
        
        // Export transactions
        export.setTransactions(transactionService.findAllByUserId(userId));
        
        // Export budgets
        export.setBudgets(budgetService.findAllByUserId(userId));
        
        // Export recurring transactions
        export.setRecurringTransactions(recurringTransactionService.findAllByUserId(userId));
        
        // Export financial goals
        export.setFinancialGoals(financialGoalService.findAllByUserId(userId));

        return export;
    }

    @Transactional
    public void importUserData(UserDataExportDto importData, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Import wallets
        if (importData.getWallets() != null) {
            for (WalletDto walletDto : importData.getWallets()) {
                try {
                    walletService.create(walletDto, userId);
                } catch (Exception e) {
                    // Skip if wallet already exists
                }
            }
        }

        // Import categories
        if (importData.getCategories() != null) {
            for (CategoryDto categoryDto : importData.getCategories()) {
                try {
                    categoryService.create(categoryDto, userId);
                } catch (Exception e) {
                    // Skip if category already exists
                }
            }
        }

        // Import transactions (after wallets and categories)
        if (importData.getTransactions() != null) {
            for (TransactionDto transactionDto : importData.getTransactions()) {
                try {
                    transactionService.create(transactionDto, userId);
                } catch (Exception e) {
                    // Skip if transaction cannot be created (e.g., wallet/category not found)
                }
            }
        }

        // Import budgets
        if (importData.getBudgets() != null) {
            for (BudgetDto budgetDto : importData.getBudgets()) {
                try {
                    budgetService.create(budgetDto, userId);
                } catch (Exception e) {
                    // Skip if budget cannot be created
                }
            }
        }

        // Import recurring transactions
        if (importData.getRecurringTransactions() != null) {
            for (RecurringTransactionDto recurringDto : importData.getRecurringTransactions()) {
                try {
                    recurringTransactionService.create(recurringDto, userId);
                } catch (Exception e) {
                    // Skip if recurring transaction cannot be created
                }
            }
        }

        // Import financial goals
        if (importData.getFinancialGoals() != null) {
            for (FinancialGoalDto goalDto : importData.getFinancialGoals()) {
                try {
                    financialGoalService.create(goalDto, userId);
                } catch (Exception e) {
                    // Skip if goal cannot be created
                }
            }
        }
    }
}

