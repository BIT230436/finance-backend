package com.example.financebackend.controller;

import com.example.financebackend.dto.CategoryDto;
import com.example.financebackend.entity.Category;
import com.example.financebackend.service.CategoryService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryDto> list(@RequestParam(required = false) Category.CategoryType type) {
        Long userId = AuthUtil.getCurrentUserId();
        if (type != null) {
            return categoryService.findByUserIdAndType(userId, type);
        }
        return categoryService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public CategoryDto get(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        return categoryService.findByIdAndUserId(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CategoryDto create(@Valid @RequestBody CategoryDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return categoryService.create(dto, userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CategoryDto update(@PathVariable Long id, @Valid @RequestBody CategoryDto dto) {
        Long userId = AuthUtil.getCurrentUserId();
        return categoryService.update(id, dto, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        categoryService.delete(id, userId);
    }
}
