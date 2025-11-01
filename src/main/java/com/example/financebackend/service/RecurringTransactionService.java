package com.example.financebackend.service;

import com.example.financebackend.dto.RecurringTransactionDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.RecurringTransaction;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.RecurringTransactionRepository;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecurringTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(RecurringTransactionService.class);
    
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringTransactionRepository,
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<RecurringTransactionDto> findAllByUserId(Long userId) {
        return recurringTransactionRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RecurringTransactionDto findByIdAndUserId(Long id, Long userId) {
        RecurringTransaction recurring = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch định kỳ"));
        return toDto(recurring);
    }

    public RecurringTransactionDto create(RecurringTransactionDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        Wallet wallet = walletRepository.findByIdAndUserId(dto.getWalletId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));
        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
        if (!category.getType().name().equals(dto.getType().name())) {
            throw new IllegalArgumentException("Loại danh mục phải khớp với loại giao dịch");
        }

        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setUser(user);
        recurring.setWallet(wallet);
        recurring.setCategory(category);
        recurring.setAmount(dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO);
        recurring.setType(dto.getType());
        recurring.setFrequency(dto.getFrequency());
        recurring.setStartDate(dto.getStartDate());
        recurring.setEndDate(dto.getEndDate());
        recurring.setNextRunDate(dto.getStartDate());
        recurring.setActive(dto.getActive() != null ? dto.getActive() : true);
        recurring.setNote(dto.getNote());

        RecurringTransaction saved = recurringTransactionRepository.save(recurring);
        return toDto(saved);
    }

    public RecurringTransactionDto update(Long id, RecurringTransactionDto dto, Long userId) {
        RecurringTransaction recurring = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch định kỳ"));

        Wallet wallet = walletRepository.findByIdAndUserId(dto.getWalletId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));
        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
        if (!category.getType().name().equals(dto.getType().name())) {
            throw new IllegalArgumentException("Loại danh mục phải khớp với loại giao dịch");
        }

        recurring.setWallet(wallet);
        recurring.setCategory(category);
        recurring.setAmount(dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO);
        recurring.setType(dto.getType());
        recurring.setFrequency(dto.getFrequency());
        recurring.setStartDate(dto.getStartDate());
        recurring.setEndDate(dto.getEndDate());
        if (dto.getActive() != null) {
            recurring.setActive(dto.getActive());
        }
        recurring.setNote(dto.getNote());

        // Recalculate nextRunDate if frequency or startDate changed
        if (!recurring.getNextRunDate().equals(dto.getStartDate()) && recurring.getNextRunDate().isBefore(dto.getStartDate())) {
            recurring.setNextRunDate(dto.getStartDate());
        }

        RecurringTransaction saved = recurringTransactionRepository.save(recurring);
        return toDto(saved);
    }

    public void delete(Long id, Long userId) {
        RecurringTransaction recurring = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch định kỳ"));
        recurringTransactionRepository.delete(recurring);
    }

    public RecurringTransactionDto toggleActive(Long id, Long userId) {
        RecurringTransaction recurring = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch định kỳ"));
        recurring.setActive(!recurring.getActive());
        RecurringTransaction saved = recurringTransactionRepository.save(recurring);
        return toDto(saved);
    }

    private RecurringTransactionDto toDto(RecurringTransaction recurring) {
        RecurringTransactionDto dto = new RecurringTransactionDto();
        dto.setId(recurring.getId());
        dto.setWalletId(recurring.getWallet().getId());
        dto.setCategoryId(recurring.getCategory().getId());
        dto.setAmount(recurring.getAmount() != null ? recurring.getAmount() : BigDecimal.ZERO);
        dto.setType(recurring.getType());
        dto.setFrequency(recurring.getFrequency());
        dto.setStartDate(recurring.getStartDate());
        dto.setEndDate(recurring.getEndDate());
        dto.setNextRunDate(recurring.getNextRunDate());
        dto.setActive(recurring.getActive() != null ? recurring.getActive() : false);
        dto.setNote(recurring.getNote());
        dto.setCreatedAt(recurring.getCreatedAt());
        return dto;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> recurringTxns = recurringTransactionRepository
                .findByActiveTrueAndNextRunDateLessThanEqual(today);

        for (RecurringTransaction recurring : recurringTxns) {
            if (recurring.getEndDate() != null && today.isAfter(recurring.getEndDate())) {
                recurring.setActive(false);
                recurringTransactionRepository.save(recurring);
                continue;
            }

            createTransactionFromRecurring(recurring);
            updateNextRunDate(recurring);
        }
    }

    private void createTransactionFromRecurring(RecurringTransaction recurring) {
        try {
            BigDecimal amount = recurring.getAmount() != null ? recurring.getAmount() : BigDecimal.ZERO;
            Wallet wallet = recurring.getWallet();
            BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

            // Validate balance cho expense transactions
            if (recurring.getType() == Transaction.TransactionType.EXPENSE) {
                if (currentBalance.compareTo(amount) < 0) {
                    logger.warn("Recurring transaction {} skipped: insufficient balance. Wallet: {}, Required: {}, Available: {}", 
                               recurring.getId(), wallet.getId(), amount, currentBalance);
                    // Không throw exception, chỉ skip transaction này và log warning
                    // User có thể muốn theo dõi recurring transactions ngay cả khi không đủ tiền
                    return; // Skip this transaction
                }
            }

            Transaction transaction = new Transaction();
            transaction.setUser(recurring.getUser());
            transaction.setWallet(wallet);
            transaction.setCategory(recurring.getCategory());
            transaction.setAmount(amount);
            transaction.setType(recurring.getType());
            transaction.setNote(recurring.getNote() != null ? recurring.getNote() : "Giao dịch định kỳ");
            transaction.setOccurredAt(LocalDateTime.now());

            transactionRepository.save(transaction);

            // Update wallet balance
            if (recurring.getType() == Transaction.TransactionType.INCOME) {
                wallet.setBalance(currentBalance.add(amount));
            } else {
                wallet.setBalance(currentBalance.subtract(amount));
            }
            walletRepository.save(wallet);
            
            logger.info("Processed recurring transaction {}: amount={}, type={}, walletId={}", 
                       recurring.getId(), amount, recurring.getType(), wallet.getId());
        } catch (Exception e) {
            logger.error("Error processing recurring transaction {}: {}", recurring.getId(), e.getMessage(), e);
            // Không throw exception để không block các recurring transactions khác
        }
    }

    private void updateNextRunDate(RecurringTransaction recurring) {
        LocalDate nextDate = recurring.getNextRunDate();
        switch (recurring.getFrequency()) {
            case DAILY:
                nextDate = nextDate.plusDays(1);
                break;
            case WEEKLY:
                nextDate = nextDate.plusWeeks(1);
                break;
            case MONTHLY:
                nextDate = nextDate.plusMonths(1);
                break;
            case YEARLY:
                nextDate = nextDate.plusYears(1);
                break;
            default:
                throw new IllegalArgumentException("Tần suất không hợp lệ: " + recurring.getFrequency());
        }
        recurring.setNextRunDate(nextDate);
        recurringTransactionRepository.save(recurring);
    }
}
