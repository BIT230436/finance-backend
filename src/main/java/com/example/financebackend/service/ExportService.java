package com.example.financebackend.service;

import com.example.financebackend.dto.ReportSummaryDto;
import com.example.financebackend.entity.Transaction;
import com.example.financebackend.repository.TransactionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private final TransactionRepository transactionRepository;
    private final ReportService reportService;

    public ExportService(TransactionRepository transactionRepository, ReportService reportService) {
        this.transactionRepository = transactionRepository;
        this.reportService = reportService;
    }

    public byte[] exportToExcel(Long userId, LocalDateTime from, LocalDateTime to) throws IOException {
        ReportSummaryDto summary = reportService.getSummary(userId, from, to);
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
                .filter(tx -> {
                    LocalDateTime occurredAt = tx.getOccurredAt();
                    return (from == null || !occurredAt.isBefore(from))
                        && (to == null || !occurredAt.isAfter(to));
                })
                .collect(Collectors.toList());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Financial Report");

            // Summary section
            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("Financial Report Summary");
            
            rowNum++;
            sheet.createRow(rowNum++).createCell(0).setCellValue("Total Income: " + summary.getTotalIncome());
            sheet.createRow(rowNum++).createCell(0).setCellValue("Total Expense: " + summary.getTotalExpense());
            sheet.createRow(rowNum++).createCell(0).setCellValue("Balance: " + summary.getBalance());

            rowNum += 2;
            // Transactions header
            Row transHeaderRow = sheet.createRow(rowNum++);
            transHeaderRow.createCell(0).setCellValue("Date");
            transHeaderRow.createCell(1).setCellValue("Type");
            transHeaderRow.createCell(2).setCellValue("Category");
            transHeaderRow.createCell(3).setCellValue("Amount");
            transHeaderRow.createCell(4).setCellValue("Note");

            // Transactions data
            for (Transaction tx : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(tx.getOccurredAt().toString());
                row.createCell(1).setCellValue(tx.getType().name());
                row.createCell(2).setCellValue(tx.getCategory().getName());
                row.createCell(3).setCellValue(tx.getAmount().doubleValue());
                row.createCell(4).setCellValue(tx.getNote() != null ? tx.getNote() : "");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] exportToPdf(Long userId, LocalDateTime from, LocalDateTime to) throws IOException {
        // Simplified PDF export - in production use iText7 properly
        ReportSummaryDto summary = reportService.getSummary(userId, from, to);
        String content = "Financial Report\n\n" +
                "Total Income: " + summary.getTotalIncome() + "\n" +
                "Total Expense: " + summary.getTotalExpense() + "\n" +
                "Balance: " + summary.getBalance();
        
        // TODO: Implement proper PDF generation with iText7
        return content.getBytes();
    }
}
