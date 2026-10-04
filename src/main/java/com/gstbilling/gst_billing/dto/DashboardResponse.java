package com.gstbilling.gst_billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        BigDecimal todaySales,
        BigDecimal monthSales,
        BigDecimal yearSales,
        BigDecimal totalSales,

        long totalInvoices,
        long paidInvoicesCount,
        long unpaidInvoicesCount,
        long overdueInvoicesCount,

        BigDecimal totalCollected,
        BigDecimal totalOutstanding,

        BigDecimal totalTax,
        BigDecimal totalCgst,
        BigDecimal totalSgst,
        BigDecimal totalIgst,

        long totalCustomers,
        long customersWithOutstanding,
        long overdueCustomersCount,

        long totalProducts,
        long lowStockProductsCount,
        List<TopProductDto> topSellingProducts,

        List<RecentInvoiceDto> recentInvoices
) {
    public record TopProductDto(Long productId, String productName, BigDecimal totalQuantity, BigDecimal totalRevenue) {}
    public record RecentInvoiceDto(Long id, String invoiceNumber, String customerName, LocalDate invoiceDate, BigDecimal grandTotal, String status) {}
}
