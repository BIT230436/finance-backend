package com.example.financebackend.service;

import com.example.financebackend.dto.FinancialGoalDto;
import com.example.financebackend.entity.FinancialGoal;
import com.example.financebackend.entity.User;
import com.example.financebackend.entity.Wallet;
import com.example.financebackend.repository.FinancialGoalRepository;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinancialGoalService {

    private final FinancialGoalRepository financialGoalRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public FinancialGoalService(FinancialGoalRepository financialGoalRepository,
                                UserRepository userRepository,
                                WalletRepository walletRepository) {
        this.financialGoalRepository = financialGoalRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    public List<FinancialGoalDto> findAllByUserId(Long userId) {
        return financialGoalRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<FinancialGoalDto> findActiveByUserId(Long userId) {
        return financialGoalRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public FinancialGoalDto findByIdAndUserId(Long id, Long userId) {
        FinancialGoal goal = financialGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục tiêu tài chính"));
        return toDto(goal);
    }

    public FinancialGoalDto create(FinancialGoalDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        FinancialGoal goal = new FinancialGoal();
        goal.setUser(user);
        goal.setName(dto.getName());
        goal.setDescription(dto.getDescription());
        goal.setTargetAmount(dto.getTargetAmount() != null ? dto.getTargetAmount() : BigDecimal.ZERO);
        goal.setCurrentAmount(dto.getCurrentAmount() != null ? dto.getCurrentAmount() : BigDecimal.ZERO);
        goal.setTargetDate(dto.getTargetDate());
        goal.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : java.time.LocalDate.now());
        goal.setActive(dto.getActive() != null ? dto.getActive() : true);

        if (dto.getWalletId() != null) {
            Wallet wallet = walletRepository.findByIdAndUserId(dto.getWalletId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));
            goal.setWallet(wallet);
        }

        FinancialGoal saved = financialGoalRepository.save(goal);
        return toDto(saved);
    }

    public FinancialGoalDto update(Long id, FinancialGoalDto dto, Long userId) {
        FinancialGoal goal = financialGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục tiêu tài chính"));

        goal.setName(dto.getName());
        goal.setDescription(dto.getDescription());
        goal.setTargetAmount(dto.getTargetAmount() != null ? dto.getTargetAmount() : goal.getTargetAmount());
        if (dto.getCurrentAmount() != null) {
            goal.setCurrentAmount(dto.getCurrentAmount());
        }
        goal.setTargetDate(dto.getTargetDate());
        if (dto.getStartDate() != null) {
            goal.setStartDate(dto.getStartDate());
        }
        if (dto.getActive() != null) {
            goal.setActive(dto.getActive());
        }

        if (dto.getWalletId() != null) {
            Wallet wallet = walletRepository.findByIdAndUserId(dto.getWalletId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví"));
            goal.setWallet(wallet);
        } else {
            goal.setWallet(null);
        }

        FinancialGoal saved = financialGoalRepository.save(goal);
        return toDto(saved);
    }

    public void delete(Long id, Long userId) {
        FinancialGoal goal = financialGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mục tiêu tài chính"));
        financialGoalRepository.delete(goal);
    }

    private FinancialGoalDto toDto(FinancialGoal goal) {
        FinancialGoalDto dto = new FinancialGoalDto();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setDescription(goal.getDescription());
        dto.setTargetAmount(goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO);
        dto.setCurrentAmount(goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO);
        dto.setTargetDate(goal.getTargetDate());
        dto.setStartDate(goal.getStartDate());
        dto.setActive(goal.getActive() != null ? goal.getActive() : false);
        dto.setCreatedAt(goal.getCreatedAt());
        if (goal.getWallet() != null) {
            dto.setWalletId(goal.getWallet().getId());
        }
        return dto;
    }
}

