package com.example.financebackend.controller;

import com.example.financebackend.dto.FinancialGoalDto;
import com.example.financebackend.service.FinancialGoalService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financial-goals")
public class FinancialGoalController {

    private final FinancialGoalService financialGoalService;

    public FinancialGoalController(FinancialGoalService financialGoalService) {
        this.financialGoalService = financialGoalService;
    }

    @GetMapping
    public List<FinancialGoalDto> list(@RequestParam(required = false) Boolean active) {
        Long userId = AuthUtil.getCurrentUserId();
        if (Boolean.TRUE.equals(active)) {
            return financialGoalService.findActiveByUserId(userId);
        }
        return financialGoalService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public FinancialGoalDto get(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return financialGoalService.findByIdAndUserId(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public FinancialGoalDto create(@Valid @RequestBody FinancialGoalDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return financialGoalService.create(dto, userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public FinancialGoalDto update(@PathVariable Long id, @Valid @RequestBody FinancialGoalDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return financialGoalService.update(id, dto, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        financialGoalService.delete(id, userId);
    }
}

