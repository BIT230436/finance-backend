package com.example.financebackend.controller;

import com.example.financebackend.service.BudgetRecommendationService;
import com.example.financebackend.service.BudgetRecommendationService.BudgetRecommendationDto;
import com.example.financebackend.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget-recommendations")
public class BudgetRecommendationController {

    private final BudgetRecommendationService budgetRecommendationService;

    public BudgetRecommendationController(BudgetRecommendationService budgetRecommendationService) {
        this.budgetRecommendationService = budgetRecommendationService;
    }

    @GetMapping("/category/{categoryId}")
    public BudgetRecommendationDto getRecommendation(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "3") int months) {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetRecommendationService.recommendBudget(categoryId, userId, months);
    }

    @GetMapping("/all")
    public List<BudgetRecommendationDto> getAllRecommendations(
            @RequestParam(defaultValue = "3") int months) {
        Long userId = AuthUtil.getCurrentUserId();
        return budgetRecommendationService.recommendBudgetsForAllCategories(userId, months);
    }
}

