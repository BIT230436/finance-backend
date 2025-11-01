package com.example.financebackend.service;

import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletShareRepository walletShareRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private com.example.financebackend.service.AchievementService achievementService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository,
                walletRepository,
                categoryRepository,
                userRepository,
                walletShareRepository,
                budgetRepository,
                budgetService,
                achievementService
        );
    }

    @Test
    void create_IncomeTransaction_ShouldIncreaseWalletBalance() {
        // Arrange
        Long userId = 1L;
        TransactionDto dto = new TransactionDto();
        dto.setWalletId(1L);
        dto.setCategoryId(1L);
        dto.setAmount(new BigDecimal("100000"));
        dto.setType(Transaction.TransactionType.INCOME);
        dto.setNote("Salary");

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setBalance(new BigDecimal("500000"));
        wallet.setUser(user);

        Category category = new Category();
        category.setId(1L);
        category.setType(Category.CategoryType.INCOME);
        category.setUser(user);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);
        savedTransaction.setAmount(dto.getAmount());
        savedTransaction.setType(dto.getType());
        savedTransaction.setWallet(wallet);
        savedTransaction.setCategory(category);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        TransactionDto result = transactionService.create(dto, userId);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("100000"), result.getAmount());
        // Wallet balance should increase: 500000 + 100000 = 600000
        assertEquals(new BigDecimal("600000"), wallet.getBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void create_ExpenseTransaction_ShouldDecreaseWalletBalance() {
        // Arrange
        Long userId = 1L;
        TransactionDto dto = new TransactionDto();
        dto.setWalletId(1L);
        dto.setCategoryId(2L);
        dto.setAmount(new BigDecimal("50000"));
        dto.setType(Transaction.TransactionType.EXPENSE);
        dto.setNote("Coffee");

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setBalance(new BigDecimal("500000"));
        wallet.setUser(user);

        Category category = new Category();
        category.setId(2L);
        category.setType(Category.CategoryType.EXPENSE);
        category.setUser(user);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);
        savedTransaction.setAmount(dto.getAmount());
        savedTransaction.setType(dto.getType());
        savedTransaction.setWallet(wallet);
        savedTransaction.setCategory(category);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findByIdAndUserId(2L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        TransactionDto result = transactionService.create(dto, userId);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("50000"), result.getAmount());
        // Wallet balance should decrease: 500000 - 50000 = 450000
        assertEquals(new BigDecimal("450000"), wallet.getBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void create_WithCategoryTypeMismatch_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        TransactionDto dto = new TransactionDto();
        dto.setWalletId(1L);
        dto.setCategoryId(1L);
        dto.setAmount(new BigDecimal("100000"));
        dto.setType(Transaction.TransactionType.INCOME);

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUser(user);

        Category category = new Category();
        category.setId(1L);
        category.setType(Category.CategoryType.EXPENSE); // Mismatch!
        category.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(categoryRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(category));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.create(dto, userId)
        );

        assertEquals("Loại danh mục phải khớp với loại giao dịch", exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void delete_ShouldRevertWalletBalance() {
        // Arrange
        Long transactionId = 1L;
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setBalance(new BigDecimal("450000"));
        wallet.setUser(user);

        Category category = new Category();
        category.setId(1L);
        category.setUser(user);

        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        transaction.setAmount(new BigDecimal("50000"));
        transaction.setType(Transaction.TransactionType.EXPENSE);
        transaction.setWallet(wallet);
        transaction.setCategory(category);
        transaction.setUser(user);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        transactionService.delete(transactionId, userId);

        // Assert
        // Balance should be reverted: 450000 + 50000 = 500000 (reverting expense)
        assertEquals(new BigDecimal("500000"), wallet.getBalance());
        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).delete(transaction);
    }
}

