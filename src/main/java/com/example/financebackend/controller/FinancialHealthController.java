package com.example.financebackend.controller;

import com.example.financebackend.service.FinancialHealthScoreService;
import com.example.financebackend.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Financial Health Controller
 * API để xem điểm sức khỏe tài chính
 */
@RestController
@RequestMapping("/api/financial-health")
public class FinancialHealthController {

    private final FinancialHealthScoreService healthScoreService;

    public FinancialHealthController(FinancialHealthScoreService healthScoreService) {
        this.healthScoreService = healthScoreService;
    }

    /**
     * Get financial health score
     * Returns score (0-100) và recommendations
     */
    @GetMapping("/score")
    public Map<String, Object> getHealthScore() {
        Long userId = AuthUtil.getCurrentUserId();
        return healthScoreService.calculateHealthScore(userId);
    }
}

