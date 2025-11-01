package com.example.financebackend.controller;

import com.example.financebackend.dto.BudgetDto;
import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.service.BudgetService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetDto> list() {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public BudgetDto get(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetService.findByIdAndUserId(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BudgetDto create(@Valid @RequestBody BudgetDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetService.create(dto, userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BudgetDto update(@PathVariable Long id, @Valid @RequestBody BudgetDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetService.update(id, dto, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        budgetService.delete(id, userId);
    }

    @GetMapping("/alerts")
    public List<BudgetDto> getAlerts() {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetService.getAlerts(userId);
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionDto> getBudgetTransactions(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetService.getTransactionsByBudget(id, userId);
    }
}