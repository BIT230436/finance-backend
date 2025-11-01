package com.example.financebackend.controller;

import com.example.financebackend.service.CashflowForecastService;
import com.example.financebackend.util.AuthUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Cashflow Forecast Controller
 * API để dự đoán cashflow trong tương lai
 */
@RestController
@RequestMapping("/api/cashflow-forecast")
public class CashflowForecastController {

    private final CashflowForecastService forecastService;

    public CashflowForecastController(CashflowForecastService forecastService) {
        this.forecastService = forecastService;
    }

    /**
     * Get cashflow forecast for next N days
     * Default: 30 days
     */
    @GetMapping
    public Map<String, Object> getForecast(@RequestParam(required = false) Integer days) {
        Long userId = AuthUtil.getCurrentUserId();
        return forecastService.getForecast(userId, days);
    }
}

