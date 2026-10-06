package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.ReportDto.*;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.InvoiceItem;
import com.gstbilling.gst_billing.entity.Payment;
import com.gstbilling.gst_billing.entity.Product;
import com.gstbilling.gst_billing.repository.CustomerRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.PaymentRepository;
import com.gstbilling.gst_billing.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserService currentUserService;

    public ReportService(InvoiceRepository invoiceRepository,
                         CustomerRepository customerRepository,
                         ProductRepository productRepository,
                         PaymentRepository paymentRepository,
                         CurrentUserService currentUserService) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public SalesReport getSalesReport(LocalDate startDate, LocalDate endDate) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<Invoice> invoices = invoiceRepository.findByBusiness_IdAndInvoiceDateBetween(businessId, start, end)
                .stream()
                .filter(inv -> !"CANCELLED".equalsIgnoreCase(inv.getStatus()) && !"DRAFT".equalsIgnoreCase(inv.getStatus()))
                .sorted(Comparator.comparing(Invoice::getInvoiceDate))
                .toList();

        BigDecimal sumTaxable = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;
        BigDecimal sumGrand = BigDecimal.ZERO;
        BigDecimal sumPaid = BigDecimal.ZERO;
        BigDecimal sumBalance = BigDecimal.ZERO;

        Map<LocalDate, List<Invoice>> byDate = invoices.stream()
                .collect(Collectors.groupingBy(Invoice::getInvoiceDate));

        List<SalesDailyBreakdown> daily = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Invoice>> entry : byDate.entrySet()) {
            LocalDate d = entry.getKey();
            List<Invoice> dayInvs = entry.getValue();

            BigDecimal dayTaxable = dayInvs.stream().map(Invoice::getTaxableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayTax = dayInvs.stream().map(Invoice::getTotalTax).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayTotal = dayInvs.stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

            daily.add(new SalesDailyBreakdown(d, dayInvs.size(), dayTaxable, dayTax, dayTotal));

            sumTaxable = sumTaxable.add(dayTaxable);
            sumTax = sumTax.add(dayTax);
            sumGrand = sumGrand.add(dayTotal);
            sumPaid = sumPaid.add(dayInvs.stream().map(i -> i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            sumBalance = sumBalance.add(dayInvs.stream().map(i -> i.getBalanceAmount() != null ? i.getBalanceAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        daily.sort(Comparator.comparing(SalesDailyBreakdown::date));

        return new SalesReport(start, end, invoices.size(), sumTaxable, sumTax, sumGrand, sumPaid, sumBalance, daily);
    }

    @Transactional(readOnly = true)
    public GstSummaryReport getGstReport(LocalDate startDate, LocalDate endDate) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<Invoice> invoices = invoiceRepository.findByBusiness_IdAndInvoiceDateBetween(businessId, start, end)
                .stream()
                .filter(inv -> !"CANCELLED".equalsIgnoreCase(inv.getStatus()) && !"DRAFT".equalsIgnoreCase(inv.getStatus()))
                .toList();

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        BigDecimal b2bTaxable = BigDecimal.ZERO;
        BigDecimal b2bTax = BigDecimal.ZERO;
        BigDecimal b2bTotal = BigDecimal.ZERO;
        long b2bCount = 0;

        BigDecimal b2cTaxable = BigDecimal.ZERO;
        BigDecimal b2cTax = BigDecimal.ZERO;
        BigDecimal b2cTotal = BigDecimal.ZERO;
        long b2cCount = 0;

        BigDecimal exportTaxable = BigDecimal.ZERO;
        BigDecimal exportTotal = BigDecimal.ZERO;
        long exportCount = 0;

        // Group HSN items
        Map<String, HsnAccumulator> hsnMap = new HashMap<>();

        for (Invoice inv : invoices) {
            BigDecimal invTaxable = inv.getTaxableAmount() != null ? inv.getTaxableAmount() : BigDecimal.ZERO;
            BigDecimal invTax = inv.getTotalTax() != null ? inv.getTotalTax() : BigDecimal.ZERO;
            BigDecimal invGrand = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO;

            totalTaxable = totalTaxable.add(invTaxable);
            totalCgst = totalCgst.add(inv.getCgst() != null ? inv.getCgst() : BigDecimal.ZERO);
            totalSgst = totalSgst.add(inv.getSgst() != null ? inv.getSgst() : BigDecimal.ZERO);
            totalIgst = totalIgst.add(inv.getIgst() != null ? inv.getIgst() : BigDecimal.ZERO);
            totalTax = totalTax.add(invTax);
            grandTotal = grandTotal.add(invGrand);

            Customer cust = inv.getCustomer();
            boolean isExport = Boolean.TRUE.equals(inv.getExportType())
                    || (cust != null && "EXPORT".equalsIgnoreCase(cust.getCustomerType()));
            boolean isB2B = cust != null && cust.getGstin() != null && !cust.getGstin().isBlank();

            if (isExport) {
                exportTaxable = exportTaxable.add(invTaxable);
                exportTotal = exportTotal.add(invGrand);
                exportCount++;
            } else if (isB2B) {
                b2bTaxable = b2bTaxable.add(invTaxable);
                b2bTax = b2bTax.add(invTax);
                b2bTotal = b2bTotal.add(invGrand);
                b2bCount++;
            } else {
                b2cTaxable = b2cTaxable.add(invTaxable);
                b2cTax = b2cTax.add(invTax);
                b2cTotal = b2cTotal.add(invGrand);
                b2cCount++;
            }

            if (inv.getItems() != null) {
                boolean isIntra = inv.getSupplierState() != null && inv.getCustomerState() != null
                        && inv.getSupplierState().equalsIgnoreCase(inv.getCustomerState());

                for (InvoiceItem item : inv.getItems()) {
                    String hsn = (item.getHsnCode() != null && !item.getHsnCode().isBlank())
                            ? item.getHsnCode().trim() : "OTHERS";

                    HsnAccumulator acc = hsnMap.computeIfAbsent(hsn, k -> new HsnAccumulator(k, item.getProductName(), item.getUnit()));
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                    BigDecimal taxable = item.getTaxableAmount() != null ? item.getTaxableAmount() : BigDecimal.ZERO;
                    BigDecimal tax = item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO;

                    acc.qty = acc.qty.add(qty);
                    acc.taxable = acc.taxable.add(taxable);
                    acc.totalTax = acc.totalTax.add(tax);

                    if (isIntra) {
                        BigDecimal half = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                        acc.cgst = acc.cgst.add(half);
                        acc.sgst = acc.sgst.add(tax.subtract(half));
                    } else {
                        acc.igst = acc.igst.add(tax);
                    }
                }
            }
        }

        List<HsnSummaryItem> hsnList = hsnMap.values().stream()
                .map(a -> new HsnSummaryItem(a.hsnCode, a.description, a.uqc, a.qty, a.taxable, a.cgst, a.sgst, a.igst, a.totalTax))
                .sorted(Comparator.comparing(HsnSummaryItem::hsnCode))
                .toList();

        return new GstSummaryReport(
                start, end,
                totalTaxable, totalCgst, totalSgst, totalIgst, totalTax, grandTotal,
                b2bTaxable, b2bTax, b2bTotal, b2bCount,
                b2cTaxable, b2cTax, b2cTotal, b2cCount,
                exportTaxable, exportTotal, exportCount,
                hsnList
        );
    }

    @Transactional(readOnly = true)
    public List<CustomerReportItem> getCustomerReport() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Customer> customers = customerRepository.findByBusinessId(businessId);
        List<Invoice> invoices = invoiceRepository.findByBusiness_Id(businessId);
        LocalDate today = LocalDate.now();

        Map<Long, List<Invoice>> invsByCust = invoices.stream()
                .filter(i -> !"CANCELLED".equalsIgnoreCase(i.getStatus()))
                .filter(i -> i.getCustomer() != null)
                .collect(Collectors.groupingBy(i -> i.getCustomer().getId()));

        List<CustomerReportItem> results = new ArrayList<>();
        for (Customer c : customers) {
            List<Invoice> custInvs = invsByCust.getOrDefault(c.getId(), Collections.emptyList());

            BigDecimal totalInvoiced = custInvs.stream().map(i -> i.getGrandTotal() != null ? i.getGrandTotal() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPaid = custInvs.stream().map(i -> i.getPaidAmount() != null ? i.getPaidAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = custInvs.stream().map(i -> i.getBalanceAmount() != null ? i.getBalanceAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal overdue = custInvs.stream()
                    .filter(i -> i.getDueDate() != null && today.isAfter(i.getDueDate()) && !"PAID".equalsIgnoreCase(i.getStatus()))
                    .map(i -> i.getBalanceAmount() != null ? i.getBalanceAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            results.add(new CustomerReportItem(
                    c.getId(),
                    c.getName(),
                    c.getGstin(),
                    c.getState(),
                    c.getCustomerType(),
                    custInvs.size(),
                    totalInvoiced,
                    totalPaid,
                    outstanding,
                    overdue
            ));
        }

        results.sort(Comparator.comparing(CustomerReportItem::totalInvoiced).reversed());
        return results;
    }

    @Transactional(readOnly = true)
    public List<ProductReportItem> getProductReport() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Product> products = productRepository.findByBusinessId(businessId);
        List<Invoice> invoices = invoiceRepository.findByBusiness_Id(businessId);

        Map<Long, ProductAccumulator> aggMap = new HashMap<>();

        for (Invoice inv : invoices) {
            if ("CANCELLED".equalsIgnoreCase(inv.getStatus())) continue;

            if (inv.getItems() != null) {
                for (InvoiceItem item : inv.getItems()) {
                    if (item.getProductId() != null) {
                        ProductAccumulator acc = aggMap.computeIfAbsent(item.getProductId(), k -> new ProductAccumulator());
                        BigDecimal q = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                        BigDecimal rev = item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO;
                        BigDecimal tax = item.getTaxAmount() != null ? item.getTaxAmount() : BigDecimal.ZERO;

                        acc.qty = acc.qty.add(q);
                        acc.rev = acc.rev.add(rev);
                        acc.tax = acc.tax.add(tax);
                    }
                }
            }
        }

        List<ProductReportItem> items = new ArrayList<>();
        for (Product p : products) {
            ProductAccumulator acc = aggMap.getOrDefault(p.getId(), new ProductAccumulator());
            items.add(new ProductReportItem(
                    p.getId(),
                    p.getName(),
                    p.getSku(),
                    p.getHsnCode(),
                    p.getUnit(),
                    p.getStockQuantity(),
                    acc.qty,
                    acc.rev,
                    acc.tax
            ));
        }

        items.sort(Comparator.comparing(ProductReportItem::totalRevenue).reversed());
        return items;
    }

    public String exportInvoicesCsv() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Invoice> invoices = invoiceRepository.findByBusiness_IdOrderByInvoiceDateDescIdDesc(businessId);

        StringBuilder sb = new StringBuilder();
        sb.append("Invoice Number,Date,Due Date,Customer Name,Customer GSTIN,Customer State,Taxable Amount,CGST,SGST,IGST,Total Tax,Grand Total,Paid Amount,Balance Due,Status\n");

        for (Invoice inv : invoices) {
            sb.append(escape(inv.getInvoiceNumber())).append(",")
                    .append(inv.getInvoiceDate()).append(",")
                    .append(inv.getDueDate() != null ? inv.getDueDate() : "").append(",")
                    .append(escape(inv.getCustomer() != null ? inv.getCustomer().getName() : "")).append(",")
                    .append(escape(inv.getCustomer() != null ? inv.getCustomer().getGstin() : "")).append(",")
                    .append(escape(inv.getCustomerState())).append(",")
                    .append(inv.getTaxableAmount()).append(",")
                    .append(inv.getCgst()).append(",")
                    .append(inv.getSgst()).append(",")
                    .append(inv.getIgst()).append(",")
                    .append(inv.getTotalTax()).append(",")
                    .append(inv.getGrandTotal()).append(",")
                    .append(inv.getPaidAmount()).append(",")
                    .append(inv.getBalanceAmount()).append(",")
                    .append(inv.getStatus()).append("\n");
        }
        return sb.toString();
    }

    public String exportCustomersCsv() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Customer> customers = customerRepository.findByBusinessId(businessId);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Name,GSTIN,PAN,Phone,Email,Customer Type,State,Billing Address,Credit Limit,Opening Balance\n");

        for (Customer c : customers) {
            sb.append(c.getId()).append(",")
                    .append(escape(c.getName())).append(",")
                    .append(escape(c.getGstin())).append(",")
                    .append(escape(c.getPan())).append(",")
                    .append(escape(c.getPhone())).append(",")
                    .append(escape(c.getEmail())).append(",")
                    .append(escape(c.getCustomerType())).append(",")
                    .append(escape(c.getState())).append(",")
                    .append(escape(c.getBillingAddress())).append(",")
                    .append(c.getCreditLimit()).append(",")
                    .append(c.getOpeningBalance()).append("\n");
        }
        return sb.toString();
    }

    public String exportProductsCsv() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Product> products = productRepository.findByBusinessId(businessId);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Name,SKU,Type,HSN/SAC,Unit,Price,GST Rate %,Tax Inclusive,Current Stock,Low Stock Threshold\n");

        for (Product p : products) {
            sb.append(p.getId()).append(",")
                    .append(escape(p.getName())).append(",")
                    .append(escape(p.getSku())).append(",")
                    .append(escape(p.getProductType())).append(",")
                    .append(escape(p.getHsnCode())).append(",")
                    .append(escape(p.getUnit())).append(",")
                    .append(p.getPrice()).append(",")
                    .append(p.getGstRate()).append(",")
                    .append(p.isTaxInclusive() ? "YES" : "NO").append(",")
                    .append(p.getStockQuantity()).append(",")
                    .append(p.getLowStockThreshold()).append("\n");
        }
        return sb.toString();
    }

    public String exportPaymentsCsv() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Payment> payments = paymentRepository.findByBusiness_IdOrderByPaymentDateDesc(businessId);

        StringBuilder sb = new StringBuilder();
        sb.append("Payment ID,Date,Invoice Number,Customer Name,Amount,Payment Method,Reference Number,Notes,Created By\n");

        for (Payment p : payments) {
            sb.append(p.getId()).append(",")
                    .append(p.getPaymentDate()).append(",")
                    .append(escape(p.getInvoice() != null ? p.getInvoice().getInvoiceNumber() : "")).append(",")
                    .append(escape(p.getCustomer() != null ? p.getCustomer().getName() : "")).append(",")
                    .append(p.getAmount()).append(",")
                    .append(escape(p.getPaymentMethod())).append(",")
                    .append(escape(p.getReferenceNumber())).append(",")
                    .append(escape(p.getNotes())).append(",")
                    .append(escape(p.getCreatedBy())).append("\n");
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        String val = s.replace("\"", "\"\"");
        if (val.startsWith("=") || val.startsWith("+") || val.startsWith("-") || val.startsWith("@") || val.startsWith("\t") || val.startsWith("\r")) {
            val = "'" + val;
        }
        return "\"" + val + "\"";
    }

    private static class HsnAccumulator {
        String hsnCode;
        String description;
        String uqc;
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        public HsnAccumulator(String hsnCode, String description, String uqc) {
            this.hsnCode = hsnCode;
            this.description = description;
            this.uqc = uqc != null ? uqc : "NOS";
        }
    }

    private static class ProductAccumulator {
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal rev = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
    }
}
