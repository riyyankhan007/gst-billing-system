package com.gstbilling.gst_billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportDto {

    public record SalesReport(
            LocalDate startDate,
            LocalDate endDate,
            long totalInvoices,
            BigDecimal totalTaxableAmount,
            BigDecimal totalTaxAmount,
            BigDecimal totalSalesAmount,
            BigDecimal totalCollected,
            BigDecimal totalOutstanding,
            List<SalesDailyBreakdown> dailyBreakdown
    ) {}

    public record SalesDailyBreakdown(
            LocalDate date,
            long invoiceCount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount
    ) {}

    public record GstSummaryReport(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal totalTaxableAmount,
            BigDecimal totalCgst,
            BigDecimal totalSgst,
            BigDecimal totalIgst,
            BigDecimal totalTax,
            BigDecimal grandTotal,

            // B2B Section
            BigDecimal b2bTaxable,
            BigDecimal b2bTax,
            BigDecimal b2bTotal,
            long b2bCount,

            // B2C Section
            BigDecimal b2cTaxable,
            BigDecimal b2cTax,
            BigDecimal b2cTotal,
            long b2cCount,

            // Export Section
            BigDecimal exportTaxable,
            BigDecimal exportTotal,
            long exportCount,

            // HSN Summary
            List<HsnSummaryItem> hsnSummary
    ) {}

    public record HsnSummaryItem(
            String hsnCode,
            String description,
            String uqc,
            BigDecimal totalQuantity,
            BigDecimal taxableValue,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalTax
    ) {}

    public record CustomerReportItem(
            Long customerId,
            String customerName,
            String gstin,
            String state,
            String customerType,
            long totalInvoices,
            BigDecimal totalInvoiced,
            BigDecimal totalPaid,
            BigDecimal outstandingBalance,
            BigDecimal overdueBalance
    ) {}

    public record ProductReportItem(
            Long productId,
            String productName,
            String sku,
            String hsnCode,
            String unit,
            BigDecimal currentStock,
            BigDecimal totalQuantitySold,
            BigDecimal totalRevenue,
            BigDecimal totalTaxCollected
    ) {}
}
