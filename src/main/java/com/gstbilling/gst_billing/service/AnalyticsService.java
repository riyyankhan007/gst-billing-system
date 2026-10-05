package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.AnalyticsResponse;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AnalyticsService {

    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM");

    public AnalyticsService(InvoiceRepository invoiceRepository, CurrentUserService currentUserService) {
        this.invoiceRepository = invoiceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String range, LocalDate customStart, LocalDate customEnd) {
        Long businessId = TenantContext.getTenantId();
        if (businessId == null) {
            businessId = currentUserService.getCurrentUser().getBusiness().getId();
        }

        LocalDate today = LocalDate.now();
        String activeRange = (range != null && !range.isBlank()) ? range.toUpperCase() : "MONTH";

        LocalDate startDate;
        LocalDate endDate = today;

        switch (activeRange) {
            case "TODAY" -> startDate = today;
            case "7D" -> startDate = today.minusDays(6);
            case "YEAR" -> {
                if (today.getMonthValue() >= 4) {
                    startDate = LocalDate.of(today.getYear(), 4, 1);
                } else {
                    startDate = LocalDate.of(today.getYear() - 1, 4, 1);
                }
            }
            case "CUSTOM" -> {
                startDate = (customStart != null) ? customStart : today.minusDays(29);
                endDate = (customEnd != null) ? customEnd : today;
                if (startDate.isAfter(endDate)) {
                    LocalDate temp = startDate;
                    startDate = endDate;
                    endDate = temp;
                }
            }
            case "MONTH" -> startDate = today.withDayOfMonth(1);
            default -> {
                activeRange = "MONTH";
                startDate = today.withDayOfMonth(1);
            }
        }

        // 1. Sales metrics
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        LocalDate startOfFy;
        LocalDate endOfFy;
        if (today.getMonthValue() >= 4) {
            startOfFy = LocalDate.of(today.getYear(), 4, 1);
            endOfFy = LocalDate.of(today.getYear() + 1, 3, 31);
        } else {
            startOfFy = LocalDate.of(today.getYear() - 1, 4, 1);
            endOfFy = LocalDate.of(today.getYear(), 3, 31);
        }

        BigDecimal todaySales = invoiceRepository.sumGrandTotalByBusinessIdAndDate(businessId, today);
        BigDecimal thisMonthSales = invoiceRepository.sumGrandTotalByBusinessIdAndDateBetween(businessId, startOfMonth, endOfMonth);
        BigDecimal thisYearSales = invoiceRepository.sumGrandTotalByBusinessIdAndDateBetween(businessId, startOfFy, endOfFy);
        BigDecimal selectedPeriodSales = invoiceRepository.sumGrandTotalByBusinessIdAndDateBetween(businessId, startDate, endDate);

        AnalyticsResponse.SalesSummaryDto salesSummary = new AnalyticsResponse.SalesSummaryDto(
                todaySales.setScale(2, RoundingMode.HALF_UP),
                thisMonthSales.setScale(2, RoundingMode.HALF_UP),
                thisYearSales.setScale(2, RoundingMode.HALF_UP),
                selectedPeriodSales.setScale(2, RoundingMode.HALF_UP)
        );

        // 2. GST tax summary for selected period
        List<Object[]> taxTotalsList = invoiceRepository.getPeriodTaxAndBalanceSummary(businessId, startDate, endDate);
        BigDecimal totalGst = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        if (taxTotalsList != null && !taxTotalsList.isEmpty()) {
            Object[] row = taxTotalsList.get(0);
            if (row != null && row.length >= 4) {
                totalGst = row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
                cgst = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                sgst = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                igst = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            }
        }

        AnalyticsResponse.GstSummaryDto gstSummary = new AnalyticsResponse.GstSummaryDto(
                totalGst.setScale(2, RoundingMode.HALF_UP),
                cgst.setScale(2, RoundingMode.HALF_UP),
                sgst.setScale(2, RoundingMode.HALF_UP),
                igst.setScale(2, RoundingMode.HALF_UP)
        );

        // 3. Invoices status breakdown in period
        List<Object[]> statusRows = invoiceRepository.getStatusBreakdownByDateBetween(businessId, startDate, endDate);
        long totalInvoices = 0;
        long issuedCount = 0;
        long paidCount = 0;
        long pendingCount = 0;
        long cancelledCount = 0;

        if (statusRows != null) {
            for (Object[] r : statusRows) {
                String st = (String) r[0];
                long cnt = (r[1] != null) ? ((Number) r[1]).longValue() : 0;
                totalInvoices += cnt;

                if (st != null) {
                    switch (st.toUpperCase()) {
                        case "PAID" -> paidCount += cnt;
                        case "CANCELLED" -> cancelledCount += cnt;
                        case "ISSUED" -> {
                            issuedCount += cnt;
                            pendingCount += cnt;
                        }
                        default -> pendingCount += cnt;
                    }
                }
            }
        }

        // 4. Overdue and Outstanding summary (all-time active)
        List<Object[]> overdueRows = invoiceRepository.getOverdueSummary(businessId, today);
        long overdueCount = 0;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        if (overdueRows != null && !overdueRows.isEmpty()) {
            Object[] r = overdueRows.get(0);
            if (r != null && r.length >= 2) {
                overdueCount = (r[0] != null) ? ((Number) r[0]).longValue() : 0;
                overdueAmount = (r[1] != null) ? (BigDecimal) r[1] : BigDecimal.ZERO;
            }
        }

        // Total unpaid across all pending invoices
        BigDecimal totalOutstanding = invoiceRepository.getSalesAndTaxTrendByDateBetween(businessId, LocalDate.of(2000, 1, 1), today.plusYears(10))
                .stream().map(o -> BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add); // fallback safe
        // Fetch accurate unpaid balance from database for all active invoices
        List<Object[]> allActiveSummary = invoiceRepository.getPeriodTaxAndBalanceSummary(businessId, LocalDate.of(2000, 1, 1), today.plusYears(10));
        BigDecimal activeUnpaidAmount = BigDecimal.ZERO;
        if (allActiveSummary != null && !allActiveSummary.isEmpty() && allActiveSummary.get(0).length >= 5) {
            activeUnpaidAmount = (allActiveSummary.get(0)[4] != null) ? (BigDecimal) allActiveSummary.get(0)[4] : BigDecimal.ZERO;
        }

        AnalyticsResponse.InvoiceSummaryDto invoiceSummary = new AnalyticsResponse.InvoiceSummaryDto(
                totalInvoices,
                issuedCount,
                paidCount,
                pendingCount,
                cancelledCount,
                overdueCount
        );

        AnalyticsResponse.OutstandingSummaryDto outstandingSummary = new AnalyticsResponse.OutstandingSummaryDto(
                activeUnpaidAmount.setScale(2, RoundingMode.HALF_UP),
                overdueAmount.setScale(2, RoundingMode.HALF_UP),
                pendingCount,
                overdueCount
        );

        // 5. Top 5 Customers in period
        List<Object[]> topCustomerRows = invoiceRepository.getTopCustomersByDateBetween(businessId, startDate, endDate);
        List<AnalyticsResponse.TopCustomerDto> topCustomers = new ArrayList<>();
        if (topCustomerRows != null) {
            int limit = Math.min(topCustomerRows.size(), 5);
            for (int i = 0; i < limit; i++) {
                Object[] r = topCustomerRows.get(i);
                Long custId = ((Number) r[0]).longValue();
                String custName = (String) r[1];
                long invCount = ((Number) r[2]).longValue();
                BigDecimal totalPurchase = (BigDecimal) r[3];
                BigDecimal balance = (BigDecimal) r[4];

                topCustomers.add(new AnalyticsResponse.TopCustomerDto(
                        custId,
                        custName,
                        invCount,
                        totalPurchase.setScale(2, RoundingMode.HALF_UP),
                        balance.setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }

        // 6. Top 5 Products in period
        List<Object[]> topProductRows = invoiceRepository.getTopProductsByDateBetween(businessId, startDate, endDate);
        List<AnalyticsResponse.TopProductDto> topProducts = new ArrayList<>();
        if (topProductRows != null) {
            int limit = Math.min(topProductRows.size(), 5);
            for (int i = 0; i < limit; i++) {
                Object[] r = topProductRows.get(i);
                Long prodId = r[0] != null ? ((Number) r[0]).longValue() : null;
                String prodName = (String) r[1];
                BigDecimal qty = (BigDecimal) r[2];
                BigDecimal rev = (BigDecimal) r[3];

                topProducts.add(new AnalyticsResponse.TopProductDto(
                        prodId,
                        prodName,
                        qty.setScale(2, RoundingMode.HALF_UP),
                        rev.setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }

        // 7. Sales and GST Trend over time
        List<Object[]> trendRows = invoiceRepository.getSalesAndTaxTrendByDateBetween(businessId, startDate, endDate);
        Map<LocalDate, Object[]> trendMap = new HashMap<>();
        if (trendRows != null) {
            for (Object[] r : trendRows) {
                LocalDate d = (LocalDate) r[0];
                trendMap.put(d, r);
            }
        }

        List<AnalyticsResponse.SalesTrendItemDto> salesTrend = new ArrayList<>();
        List<AnalyticsResponse.GstTrendItemDto> gstTrend = new ArrayList<>();

        // Generate day-by-day continuous timeline for smooth chart rendering if <= 31 days
        long dayCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (dayCount <= 31 && dayCount > 0) {
            for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                String dateStr = d.toString();
                String label = d.format(DISPLAY_DATE_FORMAT);

                Object[] r = trendMap.get(d);
                BigDecimal s = (r != null && r[1] != null) ? (BigDecimal) r[1] : BigDecimal.ZERO;
                BigDecimal tTax = (r != null && r[2] != null) ? (BigDecimal) r[2] : BigDecimal.ZERO;
                BigDecimal tCgst = (r != null && r[3] != null) ? (BigDecimal) r[3] : BigDecimal.ZERO;
                BigDecimal tSgst = (r != null && r[4] != null) ? (BigDecimal) r[4] : BigDecimal.ZERO;
                BigDecimal tIgst = (r != null && r[5] != null) ? (BigDecimal) r[5] : BigDecimal.ZERO;
                long count = (r != null && r[6] != null) ? ((Number) r[6]).longValue() : 0;

                salesTrend.add(new AnalyticsResponse.SalesTrendItemDto(dateStr, label, s.setScale(2, RoundingMode.HALF_UP), count));
                gstTrend.add(new AnalyticsResponse.GstTrendItemDto(
                        dateStr,
                        label,
                        tCgst.setScale(2, RoundingMode.HALF_UP),
                        tSgst.setScale(2, RoundingMode.HALF_UP),
                        tIgst.setScale(2, RoundingMode.HALF_UP),
                        tTax.setScale(2, RoundingMode.HALF_UP)
                ));
            }
        } else {
            // Group by date as retrieved for larger spans
            if (trendRows != null) {
                for (Object[] r : trendRows) {
                    LocalDate d = (LocalDate) r[0];
                    String dateStr = d.toString();
                    String label = d.format(DISPLAY_DATE_FORMAT);

                    BigDecimal s = r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO;
                    BigDecimal tTax = r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO;
                    BigDecimal tCgst = r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO;
                    BigDecimal tSgst = r[4] != null ? (BigDecimal) r[4] : BigDecimal.ZERO;
                    BigDecimal tIgst = r[5] != null ? (BigDecimal) r[5] : BigDecimal.ZERO;
                    long count = r[6] != null ? ((Number) r[6]).longValue() : 0;

                    salesTrend.add(new AnalyticsResponse.SalesTrendItemDto(dateStr, label, s.setScale(2, RoundingMode.HALF_UP), count));
                    gstTrend.add(new AnalyticsResponse.GstTrendItemDto(
                            dateStr,
                            label,
                            tCgst.setScale(2, RoundingMode.HALF_UP),
                            tSgst.setScale(2, RoundingMode.HALF_UP),
                            tIgst.setScale(2, RoundingMode.HALF_UP),
                            tTax.setScale(2, RoundingMode.HALF_UP)
                    ));
                }
            }
        }

        return new AnalyticsResponse(
                activeRange,
                startDate,
                endDate,
                salesSummary,
                gstSummary,
                invoiceSummary,
                outstandingSummary,
                topCustomers,
                topProducts,
                salesTrend,
                gstTrend
        );
    }
}
