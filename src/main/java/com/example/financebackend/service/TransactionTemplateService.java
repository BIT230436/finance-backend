package com.example.financebackend.service;

import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.dto.TransactionTemplateDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.entity.TransactionTemplate;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import java.math.BigDecimal;
import com.example.financebackend.repository.CategoryRepository;
import com.example.financebackend.repository.TransactionRepository;
import com.example.financebackend.repository.TransactionTemplateRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import com.example.financebackend.repository.WalletShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTemplateService.class);
    
    private final TransactionTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final WalletShareRepository walletShareRepository;

    public TransactionTemplateService(TransactionTemplateRepository templateRepository,
                                     UserRepository userRepository,
                                     WalletRepository walletRepository,
                                     CategoryRepository categoryRepository,
                                     TransactionRepository transactionRepository,
                                     WalletShareRepository walletShareRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.walletShareRepository = walletShareRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionTemplateDto> findAllByUserId(Long userId) {
        return templateRepository.findByUserIdOrderByUsageCountDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionTemplateDto findByIdAndUserId(Long id, Long userId) {
        TransactionTemplate template = templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy template"));
        return toDto(template);
    }

    public TransactionTemplateDto create(TransactionTemplateDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (templateRepository.existsByUserIdAndNameIgnoreCase(userId, dto.getName())) {
            throw new IllegalArgumentException("Tên template đã tồn tại");
        }

        Wallet wallet = walletRepository.findByIdAndUserId(dto.getWalletId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));

        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        if (!category.getType().name().equals(dto.getType().name())) {
            throw new IllegalArgumentException("Loại danh mục phải khớp với loại giao dịch");
        }

        TransactionTemplate template = new TransactionTemplate();
        template.setUser(user);
        template.setName(dto.getName());
        template.setWallet(wallet);
        template.setCategory(category);
        template.setAmount(dto.getAmount());
        template.setType(dto.getType());
        template.setNote(dto.getNote());
        template.setUsageCount(0);
        template.setCreatedAt(LocalDateTime.now());
        template.setLastUsedAt(LocalDateTime.now());

        TransactionTemplate saved = templateRepository.save(template);
        logger.info("Transaction template created: id={}, name={}, userId={}", saved.getId(), saved.getName(), userId);
        return toDto(saved);
    }

    public TransactionTemplateDto update(Long id, TransactionTemplateDto dto, Long userId) {
        TransactionTemplate template = templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy template"));

        if (!template.getName().equalsIgnoreCase(dto.getName()) 
                && templateRepository.existsByUserIdAndNameIgnoreCase(userId, dto.getName())) {
            throw new IllegalArgumentException("Tên template đã tồn tại");
        }

        Wallet wallet = walletRepository.findByIdAndUserId(dto.getWalletId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));

        Category category = categoryRepository.findByIdAndUserId(dto.getCategoryId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        if (!category.getType().name().equals(dto.getType().name())) {
            throw new IllegalArgumentException("Loại danh mục phải khớp với loại giao dịch");
        }

        template.setName(dto.getName());
        template.setWallet(wallet);
        template.setCategory(category);
        template.setAmount(dto.getAmount());
        template.setType(dto.getType());
        template.setNote(dto.getNote());

        TransactionTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    public void delete(Long id, Long userId) {
        TransactionTemplate template = templateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy template"));
        templateRepository.delete(template);
        logger.info("Transaction template deleted: id={}, userId={}", id, userId);
    }

    /**
     * Tạo transaction từ template
     */
    public TransactionDto createTransactionFromTemplate(Long templateId, Long userId, TransactionDto overrideDto) {
        TransactionTemplate template = templateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy template"));

        // Tạo transaction DTO từ template, override với values từ overrideDto nếu có
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setWalletId(overrideDto != null && overrideDto.getWalletId() != null 
                ? overrideDto.getWalletId() : template.getWallet().getId());
        transactionDto.setCategoryId(overrideDto != null && overrideDto.getCategoryId() != null 
                ? overrideDto.getCategoryId() : template.getCategory().getId());
        transactionDto.setAmount(overrideDto != null && overrideDto.getAmount() != null 
                ? overrideDto.getAmount() : template.getAmount());
        transactionDto.setType(overrideDto != null && overrideDto.getType() != null 
                ? overrideDto.getType() : template.getType());
        transactionDto.setNote(overrideDto != null && overrideDto.getNote() != null && !overrideDto.getNote().isEmpty()
                ? overrideDto.getNote() : template.getNote());
        transactionDto.setOccurredAt(overrideDto != null && overrideDto.getOccurredAt() != null 
                ? overrideDto.getOccurredAt() : LocalDateTime.now());

        // Tạo transaction entity trực tiếp để tránh circular dependency
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        Wallet wallet = walletRepository.findById(transactionDto.getWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));

        Category category = categoryRepository.findById(transactionDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setWallet(wallet);
        transaction.setCategory(category);
        transaction.setAmount(transactionDto.getAmount());
        transaction.setType(transactionDto.getType());
        transaction.setNote(transactionDto.getNote());
        transaction.setOccurredAt(transactionDto.getOccurredAt() != null ? transactionDto.getOccurredAt() : LocalDateTime.now());

        // Update wallet balance
        updateWalletBalance(wallet, transaction.getAmount(), transaction.getType());
        walletRepository.save(wallet);

        Transaction saved = transactionRepository.save(transaction);

        // Update template usage
        template.setUsageCount(template.getUsageCount() + 1);
        template.setLastUsedAt(LocalDateTime.now());
        templateRepository.save(template);

        logger.info("Transaction created from template: templateId={}, transactionId={}, userId={}", 
                   templateId, saved.getId(), userId);

        return toTransactionDto(saved);
    }

    private void updateWalletBalance(Wallet wallet, java.math.BigDecimal amount, Transaction.TransactionType type) {
        java.math.BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : java.math.BigDecimal.ZERO;
        
        if (type == Transaction.TransactionType.INCOME) {
            wallet.setBalance(currentBalance.add(amount));
        } else {
            java.math.BigDecimal newBalance = currentBalance.subtract(amount);
            
            if (newBalance.compareTo(java.math.BigDecimal.ZERO) < 0) {
                java.math.BigDecimal warningThreshold = new java.math.BigDecimal("-10000000");
                if (newBalance.compareTo(warningThreshold) < 0) {
                    throw new IllegalArgumentException(
                        String.format("Số dư sẽ trở thành %,.0f (âm quá lớn). Vui lòng kiểm tra lại số tiền.", 
                                    newBalance.doubleValue()));
                }
                logger.warn("Wallet {} balance going negative: {} -> {}", 
                           wallet.getId(), currentBalance, newBalance);
            }
            
            wallet.setBalance(newBalance);
        }
    }

    private TransactionDto toTransactionDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setWalletId(transaction.getWallet().getId());
        dto.setCategoryId(transaction.getCategory().getId());
        dto.setNote(transaction.getNote());
        dto.setOccurredAt(transaction.getOccurredAt());
        dto.setAttachmentUrl(transaction.getAttachmentUrl());
        return dto;
    }

    private TransactionTemplateDto toDto(TransactionTemplate template) {
        TransactionTemplateDto dto = new TransactionTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setWalletId(template.getWallet().getId());
        dto.setCategoryId(template.getCategory().getId());
        dto.setAmount(template.getAmount());
        dto.setType(template.getType());
        dto.setNote(template.getNote());
        dto.setUsageCount(template.getUsageCount());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setLastUsedAt(template.getLastUsedAt());
        return dto;
    }
}

