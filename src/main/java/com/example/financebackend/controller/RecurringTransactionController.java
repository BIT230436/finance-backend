package com.example.financebackend.controller;

import com.example.financebackend.dto.RecurringTransactionDto;
import com.example.financebackend.service.RecurringTransactionService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @GetMapping
    public List<RecurringTransactionDto> list() {
        Long userId = AuthUtil.getCurrentUserId();
        return recurringTransactionService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public RecurringTransactionDto get(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return recurringTransactionService.findByIdAndUserId(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public RecurringTransactionDto create(@Valid @RequestBody RecurringTransactionDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return recurringTransactionService.create(dto, userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public RecurringTransactionDto update(@PathVariable Long id, @Valid @RequestBody RecurringTransactionDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return recurringTransactionService.update(id, dto, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        recurringTransactionService.delete(id, userId);
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public RecurringTransactionDto toggleActive(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return recurringTransactionService.toggleActive(id, userId);
    }
}

