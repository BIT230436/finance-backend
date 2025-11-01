package com.example.financebackend.controller;

import com.example.financebackend.dto.WalletDto;
import com.example.financebackend.service.WalletService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);
    
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public List<WalletDto> list() {
        Long userId = AuthUtil.getCurrentUserId();
        return walletService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public WalletDto get(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return walletService.findByIdAndUserId(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public WalletDto create(@Valid @RequestBody WalletDto dto) {
        logger.info("Received create wallet request: {}", dto.getName());
        try {
            Long userId = AuthUtil.getCurrentUserId();
            logger.info("Current user ID: {}", userId);
            WalletDto result = walletService.create(dto, userId);
            logger.info("Wallet created successfully: id={}", result.getId());
            return result;
        } catch (Exception e) {
            logger.error("Error creating wallet: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public WalletDto update(@PathVariable Long id, @Valid @RequestBody WalletDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return walletService.update(id, dto, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        walletService.delete(id, userId);
    }
}
