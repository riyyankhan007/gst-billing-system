package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.DashboardResponse;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.InvoiceItem;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.repository.CustomerRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public DashboardService(InvoiceRepository invoiceRepository,
                            CustomerRepository customerRepository,
                            ProductRepository productRepository,
                            CurrentUserService currentUserService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardMetrics() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // Indian Financial Year: April 1 to March 31
        LocalDate startOfFy;
        LocalDate endOfFy;
        if (today.getMonthValue() >= 4) {
            startOfFy = LocalDate.of(today.getYear(), 4, 1);
            endOfFy = LocalDate.of(today.getYear() + 1, 3, 31);
        } else {
            startOfFy = LocalDate.of(today.getYear() - 1, 4, 1);
            endOfFy = LocalDate.of(today.getYear(), 3, 31);
        }

        List<Invoice> allInvoices = invoiceRepository.findByBusiness_IdOrderByInvoiceDateDescIdDesc(businessId);

        BigDecimal todaySales = BigDecimal.ZERO;
        BigDecimal monthSales = BigDecimal.ZERO;
        BigDecimal yearSales = BigDecimal.ZERO;
        BigDecimal totalSales = BigDecimal.ZERO;

        long paidCount = 0;
        long unpaidCount = 0;
        long overdueCount = 0;

        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        Set<Long> customersWithOverdueSet = new HashSet<>();
        Set<Long> customersWithOutstandingSet = new HashSet<>();

        // Map for product sales aggregation: productId -> [name, totalQty, totalRevenue]
        Map<Long, ProductSalesAgg> productSalesMap = new HashMap<>();

        for (Invoice inv : allInvoices) {
            if ("CANCELLED".equalsIgnoreCase(inv.getStatus()) || "DRAFT".equalsIgnoreCase(inv.getStatus())) {
                continue;
            }

            BigDecimal grandTotal = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO;
            BigDecimal paid = inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal balance = inv.getBalanceAmount() != null ? inv.getBalanceAmount() : grandTotal.subtract(paid);

            totalSales = totalSales.add(grandTotal);
            totalCollected = totalCollected.add(paid);
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                totalOutstanding = totalOutstanding.add(balance);
                if (inv.getCustomer() != null) {
                    customersWithOutstandingSet.add(inv.getCustomer().getId());
                }
            }

            LocalDate invDate = inv.getInvoiceDate() != null ? inv.getInvoiceDate() : inv.getCreatedAt().toLocalDate();
            if (invDate.isEqual(today)) {
                todaySales = todaySales.add(grandTotal);
            }
            if (!invDate.isBefore(startOfMonth) && !invDate.isAfter(endOfMonth)) {
                monthSales = monthSales.add(grandTotal);
            }
            if (!invDate.isBefore(startOfFy) && !invDate.isAfter(endOfFy)) {
                yearSales = yearSales.add(grandTotal);
            }

            // Tax
            if (inv.getTotalTax() != null) totalTax = totalTax.add(inv.getTotalTax());
            if (inv.getCgst() != null) totalCgst = totalCgst.add(inv.getCgst());
            if (inv.getSgst() != null) totalSgst = totalSgst.add(inv.getSgst());
            if (inv.getIgst() != null) totalIgst = totalIgst.add(inv.getIgst());

            // Status counts
            boolean isOverdue = inv.getDueDate() != null
                    && today.isAfter(inv.getDueDate())
                    && balance.compareTo(BigDecimal.ZERO) > 0
                    && !"PAID".equalsIgnoreCase(inv.getStatus());

            if (isOverdue) {
                overdueCount++;
                if (inv.getCustomer() != null) {
                    customersWithOverdueSet.add(inv.getCustomer().getId());
                }
            }

            if ("PAID".equalsIgnoreCase(inv.getStatus())) {
                paidCount++;
            } else if (!isOverdue) {
                unpaidCount++;
            }

            // Aggregate items for top products
            if (inv.getItems() != null) {
                for (InvoiceItem item : inv.getItems()) {
                    Long pId = item.getProductId();
                    if (pId != null) {
                        BigDecimal q = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                        BigDecimal rev = item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO;

                        productSalesMap.compute(pId, (k, v) -> {
                            if (v == null) {
                                return new ProductSalesAgg(pId, item.getProductName(), q, rev);
                            }
                            v.quantity = v.quantity.add(q);
                            v.revenue = v.revenue.add(rev);
                            return v;
                        });
                    }
                }
            }
        }

        // Customer metrics
        List<Customer> customers = customerRepository.findByBusinessId(businessId);
        long totalCustomers = customers.size();

        // Product metrics
        List<Product> products = productRepository.findByBusinessId(businessId);
        long totalProducts = products.size();
        long lowStockCount = products.stream()
                .filter(p -> "PRODUCT".equalsIgnoreCase(p.getProductType())
                        && p.getStockQuantity() != null
                        && p.getLowStockThreshold() != null
                        && p.getStockQuantity().compareTo(p.getLowStockThreshold()) <= 0)
                .count();

        // Top 5 selling products by revenue
        List<DashboardResponse.TopProductDto> topProducts = productSalesMap.values().stream()
                .sorted(Comparator.comparing(ProductSalesAgg::getRevenue).reversed())
                .limit(5)
                .map(p -> new DashboardResponse.TopProductDto(p.productId, p.productName, p.quantity, p.revenue))
                .collect(Collectors.toList());

        // Recent 5 invoices
        List<DashboardResponse.RecentInvoiceDto> recentInvoices = allInvoices.stream()
                .limit(5)
                .map(inv -> new DashboardResponse.RecentInvoiceDto(
                        inv.getId(),
                        inv.getInvoiceNumber(),
                        inv.getCustomer() != null ? inv.getCustomer().getName() : "Customer",
                        inv.getInvoiceDate(),
                        inv.getGrandTotal(),
                        inv.getStatus()
                ))
                .collect(Collectors.toList());

        return new DashboardResponse(
                todaySales.setScale(2, RoundingMode.HALF_UP),
                monthSales.setScale(2, RoundingMode.HALF_UP),
                yearSales.setScale(2, RoundingMode.HALF_UP),
                totalSales.setScale(2, RoundingMode.HALF_UP),

                allInvoices.size(),
                paidCount,
                unpaidCount,
                overdueCount,

                totalCollected.setScale(2, RoundingMode.HALF_UP),
                totalOutstanding.setScale(2, RoundingMode.HALF_UP),

                totalTax.setScale(2, RoundingMode.HALF_UP),
                totalCgst.setScale(2, RoundingMode.HALF_UP),
                totalSgst.setScale(2, RoundingMode.HALF_UP),
                totalIgst.setScale(2, RoundingMode.HALF_UP),

                totalCustomers,
                customersWithOutstandingSet.size(),
                customersWithOverdueSet.size(),

                totalProducts,
                lowStockCount,
                topProducts,
                recentInvoices
        );
    }

    private static class ProductSalesAgg {
        Long productId;
        String productName;
        BigDecimal quantity;
        BigDecimal revenue;

        public ProductSalesAgg(Long productId, String productName, BigDecimal quantity, BigDecimal revenue) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.revenue = revenue;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }
    }
}
