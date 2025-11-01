package com.example.financebackend.controller;

import com.example.financebackend.dto.TransactionDto;
import com.example.financebackend.dto.TransactionTemplateDto;
import com.example.financebackend.service.TransactionTemplateService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction-templates")
public class TransactionTemplateController {

    private final TransactionTemplateService templateService;

    public TransactionTemplateController(TransactionTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TransactionTemplateDto> list() {
        Long userId = AuthUtil.getCurrentUserId();
        return templateService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public TransactionTemplateDto get(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return templateService.findByIdAndUserId(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionTemplateDto create(@Valid @RequestBody TransactionTemplateDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return templateService.create(dto, userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionTemplateDto update(@PathVariable Long id, @Valid @RequestBody TransactionTemplateDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return templateService.update(id, dto, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        templateService.delete(id, userId);
    }

    @PostMapping("/{id}/create-transaction")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDto createTransactionFromTemplate(
            @PathVariable Long id,
            @RequestBody(required = false) TransactionDto overrideDto) {
        Long userId = AuthUtil.getCurrentUserId();
        return templateService.createTransactionFromTemplate(id, userId, overrideDto);
    }
}

