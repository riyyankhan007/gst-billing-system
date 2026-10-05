package com.gstbilling.gst_billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsResponse(
        String range,
        LocalDate startDate,
        LocalDate endDate,
        SalesSummaryDto sales,
        GstSummaryDto gst,
        InvoiceSummaryDto invoices,
        OutstandingSummaryDto outstanding,
        List<TopCustomerDto> topCustomers,
        List<TopProductDto> topProducts,
        List<SalesTrendItemDto> salesTrend,
        List<GstTrendItemDto> gstTrend
) {
    public record SalesSummaryDto(
            BigDecimal today,
            BigDecimal thisMonth,
            BigDecimal thisYear,
            BigDecimal selectedPeriod
    ) {}

    public record GstSummaryDto(
            BigDecimal totalGst,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst
    ) {}

    public record InvoiceSummaryDto(
            long total,
            long issued,
            long paid,
            long pending,
            long cancelled,
            long overdue
    ) {}

    public record OutstandingSummaryDto(
            BigDecimal unpaidAmount,
            BigDecimal overdueAmount,
            long unpaidCount,
            long overdueCount
    ) {}

    public record TopCustomerDto(
            Long customerId,
            String customerName,
            long invoiceCount,
            BigDecimal totalPurchaseValue,
            BigDecimal outstandingBalance
    ) {}

    public record TopProductDto(
            Long productId,
            String productName,
            BigDecimal totalQuantity,
            BigDecimal totalRevenue
    ) {}

    public record SalesTrendItemDto(
            String date,
            String label,
            BigDecimal sales,
            long invoiceCount
    ) {}

    public record GstTrendItemDto(
            String date,
            String label,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal totalGst
    ) {}
}
