package com.example.financebackend.controller;

import com.example.financebackend.service.ComparativeAnalysisService;
import com.example.financebackend.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Comparative Analysis Controller
 * API để so sánh chi tiêu qua các periods
 */
@RestController
@RequestMapping("/api/comparative-analysis")
public class ComparativeAnalysisController {

    private final ComparativeAnalysisService analysisService;

    public ComparativeAnalysisController(ComparativeAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Compare current month vs previous month
     */
    @GetMapping("/month-over-month")
    public Map<String, Object> compareMonthOverMonth() {
        Long userId = AuthUtil.getCurrentUserId();
        return analysisService.compareMonthOverMonth(userId);
    }

    /**
     * Compare current month vs 3-month average
     */
    @GetMapping("/vs-average")
    public Map<String, Object> compareWithAverage() {
        Long userId = AuthUtil.getCurrentUserId();
        return analysisService.compareWithAverage(userId);
    }

    /**
     * Compare current year vs last year (YoY)
     */
    @GetMapping("/year-over-year")
    public Map<String, Object> compareYearOverYear() {
        Long userId = AuthUtil.getCurrentUserId();
        return analysisService.compareYearOverYear(userId);
    }
}

