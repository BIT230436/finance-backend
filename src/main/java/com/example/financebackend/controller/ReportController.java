package com.example.financebackend.controller;

import com.example.financebackend.dto.CashflowDto;
import com.example.financebackend.dto.ReportSummaryDto;
import com.example.financebackend.dto.WalletSummaryDto;
import com.example.financebackend.service.ReportService;
import com.example.financebackend.util.AuthUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ReportSummaryDto getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long userId = AuthUtil.getCurrentUserId();
        return reportService.getSummary(userId, from, to);
    }

    @GetMapping("/cashflow")
    public List<CashflowDto> getCashflow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long userId = AuthUtil.getCurrentUserId();
        return reportService.getCashflow(userId, from, to);
    }

    @GetMapping("/wallet-summary")
    public List<WalletSummaryDto> getWalletSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Long userId = AuthUtil.getCurrentUserId();
        return reportService.getWalletSummary(userId, from, to);
    }
}