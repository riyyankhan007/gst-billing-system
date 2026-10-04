package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.dto.ReminderResponse;
import com.gstbilling.gst_billing.entity.*;
import com.gstbilling.gst_billing.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUserService currentUserService;
    private final GstCalculationService gstCalculationService;
    private final EmailService emailService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            BusinessRepository businessRepository,
            StockMovementRepository stockMovementRepository,
            CurrentUserService currentUserService,
            GstCalculationService gstCalculationService,
            EmailService emailService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.businessRepository = businessRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.currentUserService = currentUserService;
        this.gstCalculationService = gstCalculationService;
        this.emailService = emailService;
    }

    @Transactional
    public Invoice createInvoice(Invoice invoice) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();

        // Lock business to generate concurrency-safe per-business invoice number
        Business business = businessRepository.findByIdForUpdate(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        // Generate per-business sequential invoice number
        String prefix = (business.getInvoicePrefix() != null && !business.getInvoicePrefix().isBlank())
                ? business.getInvoicePrefix().trim() : "INV";
        String fy = (business.getFinancialYear() != null && !business.getFinancialYear().isBlank())
                ? business.getFinancialYear().trim() : "2026-27";
        long seq = (business.getInvoiceSeqNumber() != null && business.getInvoiceSeqNumber() > 0)
                ? business.getInvoiceSeqNumber() : 1L;

        String invoiceNumber = String.format("%s/%s/%03d", prefix, fy, seq);
        while (invoiceRepository.existsByBusiness_IdAndInvoiceNumberIgnoreCase(businessId, invoiceNumber)) {
            seq++;
            invoiceNumber = String.format("%s/%s/%03d", prefix, fy, seq);
        }

        business.setInvoiceSeqNumber(seq + 1);
        businessRepository.save(business);

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setBusiness(business);

        // Fetch and validate customer
        Customer customer = customerRepository.findByIdAndBusinessId(invoice.getCustomerId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + invoice.getCustomerId()));

        invoice.setCustomer(customer);
        invoice.setSupplierState(business.getState() != null ? business.getState() : "");
        invoice.setCustomerState(customer.getState() != null ? customer.getState() : "");

        invoice.setInvoiceDate(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now());
        if (invoice.getDueDate() == null) {
            int termsDays = 15;
            if (customer.getPaymentTerms() != null) {
                try {
                    String clean = customer.getPaymentTerms().replaceAll("[^0-9]", "");
                    if (!clean.isBlank()) {
                        termsDays = Integer.parseInt(clean);
                    }
                } catch (Exception ignored) {}
            }
            invoice.setDueDate(invoice.getInvoiceDate().plusDays(termsDays));
        }

        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(LocalDateTime.now());

        if (invoice.getTermsAndConditions() == null || invoice.getTermsAndConditions().isBlank()) {
            invoice.setTermsAndConditions(business.getDefaultTerms());
        }

        // Calculate line items and totals via GstCalculationService
        calculateAndPopulateInvoice(invoice, business, customer);

        String desiredStatus = (invoice.getStatus() != null && !invoice.getStatus().isBlank())
                ? invoice.getStatus().toUpperCase() : "DRAFT";

        if ("ISSUED".equals(desiredStatus)) {
            invoice.setStatus("ISSUED");
            applyStockReduction(invoice, business);
        } else {
            invoice.setStatus("DRAFT");
        }

        return invoiceRepository.save(invoice);
    }

    private void calculateAndPopulateInvoice(Invoice invoice, Business business, Customer customer) {
        boolean isExport = Boolean.TRUE.equals(invoice.getExportType())
                || "EXPORT".equalsIgnoreCase(customer.getCustomerType());

        List<GstCalculationService.ItemInput> itemInputs = new ArrayList<>();

        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice must contain at least one item");
        }

        for (InvoiceItem item : invoice.getItems()) {
            Product product = null;
            if (item.getProductId() != null) {
                product = productRepository.findByIdAndBusinessId(item.getProductId(), business.getId()).orElse(null);
            }

            if (product != null) {
                if (item.getProductName() == null || item.getProductName().isBlank()) {
                    item.setProductName(product.getName());
                }
                if (item.getHsnCode() == null || item.getHsnCode().isBlank()) {
                    item.setHsnCode(product.getHsnCode());
                }
                if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    item.setUnitPrice(product.getPrice());
                }
                if (item.getGstRate() == null) {
                    item.setGstRate(product.getGstRate());
                }
                if (item.getUnit() == null) {
                    item.setUnit(product.getUnit());
                }
                if (item.getTaxInclusive() == null) {
                    item.setTaxInclusive(product.isTaxInclusive());
                }
                if (item.getDiscount() == null) {
                    item.setDiscount(product.getDiscount() != null ? product.getDiscount() : BigDecimal.ZERO);
                }
            } else {
                if (item.getProductName() == null || item.getProductName().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item product name is required");
                }
                if (item.getUnitPrice() == null) {
                    item.setUnitPrice(BigDecimal.ZERO);
                }
                if (item.getGstRate() == null) {
                    item.setGstRate(BigDecimal.ZERO);
                }
                if (item.getDiscount() == null) {
                    item.setDiscount(BigDecimal.ZERO);
                }
                if (item.getTaxInclusive() == null) {
                    item.setTaxInclusive(false);
                }
            }

            itemInputs.add(new GstCalculationService.ItemInput(
                    item.getProductId(),
                    item.getProductName(),
                    item.getHsnCode(),
                    item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE,
                    item.getUnitPrice(),
                    item.getGstRate(),
                    item.getDiscount(),
                    Boolean.TRUE.equals(item.getTaxInclusive())
            ));
        }

        GstCalculationService.GstResult calcResult = gstCalculationService.calculate(
                invoice.getSupplierState(),
                invoice.getCustomerState(),
                isExport,
                invoice.getDiscountAmount(),
                itemInputs
        );

        // Assign calculated values to items
        for (int i = 0; i < invoice.getItems().size(); i++) {
            InvoiceItem item = invoice.getItems().get(i);
            GstCalculationService.ItemOutput output = calcResult.items().get(i);
            item.setTaxableAmount(output.taxableAmount());
            item.setTaxAmount(output.taxAmount());
            item.setTotalAmount(output.totalAmount());
            item.setInvoice(invoice);
        }

        invoice.setTaxableAmount(calcResult.taxableAmount());
        invoice.setCgst(calcResult.cgst());
        invoice.setSgst(calcResult.sgst());
        invoice.setIgst(calcResult.igst());
        invoice.setTotalTax(calcResult.totalTax());
        invoice.setGrandTotal(calcResult.grandTotal());
        invoice.setAmountInWords(calcResult.amountInWords());

        BigDecimal paid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        invoice.setPaidAmount(paid);
        invoice.setBalanceAmount(calcResult.grandTotal().subtract(paid).setScale(2, RoundingMode.HALF_UP));
    }

    private void applyStockReduction(Invoice invoice, Business business) {
        if (invoice.getItems() == null) return;

        for (InvoiceItem item : invoice.getItems()) {
            if (item.getProductId() != null) {
                Product prod = productRepository.findByIdAndBusinessId(item.getProductId(), business.getId()).orElse(null);
                if (prod != null && !"SERVICE".equalsIgnoreCase(prod.getProductType())) {
                    BigDecimal before = prod.getStockQuantity() != null ? prod.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                    BigDecimal after = before.subtract(qty);
                    prod.setStockQuantity(after);
                    productRepository.save(prod);

                    StockMovement movement = new StockMovement(
                            business,
                            prod,
                            "INVOICE",
                            qty.negate(),
                            before,
                            after,
                            invoice.getInvoiceNumber(),
                            "Deducted for issued invoice #" + invoice.getInvoiceNumber()
                    );
                    stockMovementRepository.save(movement);
                }
            }
        }
    }

    private void reverseStockReduction(Invoice invoice, Business business) {
        if (invoice.getItems() == null) return;

        for (InvoiceItem item : invoice.getItems()) {
            if (item.getProductId() != null) {
                Product prod = productRepository.findByIdAndBusinessId(item.getProductId(), business.getId()).orElse(null);
                if (prod != null && !"SERVICE".equalsIgnoreCase(prod.getProductType())) {
                    BigDecimal before = prod.getStockQuantity() != null ? prod.getStockQuantity() : BigDecimal.ZERO;
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                    BigDecimal after = before.add(qty);
                    prod.setStockQuantity(after);
                    productRepository.save(prod);

                    StockMovement movement = new StockMovement(
                            business,
                            prod,
                            "INVOICE_CANCELLED",
                            qty,
                            before,
                            after,
                            invoice.getInvoiceNumber(),
                            "Stock restored on cancellation of invoice #" + invoice.getInvoiceNumber()
                    );
                    stockMovementRepository.save(movement);
                }
            }
        }
    }

    @Transactional
    public Invoice issueInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);

        if ("ISSUED".equalsIgnoreCase(invoice.getStatus()) || "PAID".equalsIgnoreCase(invoice.getStatus())) {
            return invoice;
        }

        if (!"DRAFT".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT invoices can be issued");
        }

        invoice.setStatus("ISSUED");
        invoice.setUpdatedAt(LocalDateTime.now());
        applyStockReduction(invoice, invoice.getBusiness());

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice markAsSent(Long id) {
        Invoice invoice = getInvoiceById(id);
        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a cancelled invoice");
        }
        invoice.setStatus("SENT");
        invoice.setUpdatedAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateDraftInvoice(Long id, Invoice updatedInvoice) {
        Invoice existing = getInvoiceById(id);

        if (!"DRAFT".equalsIgnoreCase(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT invoices can be directly modified. Issued invoices require a Credit Note or Cancellation.");
        }

        Business business = existing.getBusiness();
        if (updatedInvoice.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndBusinessId(updatedInvoice.getCustomerId(), business.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
            existing.setCustomer(customer);
            existing.setCustomerState(customer.getState());
        }

        if (updatedInvoice.getInvoiceDate() != null) {
            existing.setInvoiceDate(updatedInvoice.getInvoiceDate());
        }
        if (updatedInvoice.getDueDate() != null) {
            existing.setDueDate(updatedInvoice.getDueDate());
        }
        if (updatedInvoice.getDiscountAmount() != null) {
            existing.setDiscountAmount(updatedInvoice.getDiscountAmount());
        }
        if (updatedInvoice.getNotes() != null) {
            existing.setNotes(updatedInvoice.getNotes());
        }
        if (updatedInvoice.getTermsAndConditions() != null) {
            existing.setTermsAndConditions(updatedInvoice.getTermsAndConditions());
        }

        if (updatedInvoice.getItems() != null && !updatedInvoice.getItems().isEmpty()) {
            existing.getItems().clear();
            for (InvoiceItem item : updatedInvoice.getItems()) {
                item.setInvoice(existing);
                existing.getItems().add(item);
            }
        }

        calculateAndPopulateInvoice(existing, business, existing.getCustomer());
        existing.setUpdatedAt(LocalDateTime.now());

        return invoiceRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Invoice> invoices = invoiceRepository.findByBusiness_IdOrderByInvoiceDateDescIdDesc(businessId);
        invoices.forEach(this::checkAndUpdateOverdue);
        return invoices;
    }

    @Transactional(readOnly = true)
    public List<Invoice> searchInvoices(String status, Long customerId) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        List<Invoice> invoices = invoiceRepository.searchInvoices(businessId, status, customerId);
        invoices.forEach(this::checkAndUpdateOverdue);
        return invoices;
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(id, businessId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found or not available to this business")
                );
        checkAndUpdateOverdue(invoice);
        return invoice;
    }

    private void checkAndUpdateOverdue(Invoice invoice) {
        if (invoice == null) return;
        if (!"CANCELLED".equalsIgnoreCase(invoice.getStatus())
                && !"PAID".equalsIgnoreCase(invoice.getStatus())
                && !"DRAFT".equalsIgnoreCase(invoice.getStatus())) {

            if (invoice.getDueDate() != null
                    && LocalDate.now().isAfter(invoice.getDueDate())
                    && invoice.getBalanceAmount() != null
                    && invoice.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus("OVERDUE");
            }
        }
    }

    @Transactional
    public Invoice cancelInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);

        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            return invoice;
        }

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel a fully paid invoice. Please reverse payments or issue a credit note instead.");
        }

        // If it was issued, restore stock
        if ("ISSUED".equalsIgnoreCase(invoice.getStatus())
                || "SENT".equalsIgnoreCase(invoice.getStatus())
                || "OVERDUE".equalsIgnoreCase(invoice.getStatus())
                || "PARTIALLY_PAID".equalsIgnoreCase(invoice.getStatus())) {
            reverseStockReduction(invoice, invoice.getBusiness());
        }

        invoice.setStatus("CANCELLED");
        invoice.setUpdatedAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);
        if (!"DRAFT".equalsIgnoreCase(invoice.getStatus()) && !"CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT or CANCELLED invoices can be deleted.");
        }
        invoiceRepository.delete(invoice);
    }

    public ReminderResponse getReminderDetails(Long id) {
        Invoice invoice = getInvoiceById(id);
        Customer customer = invoice.getCustomer();
        Business business = invoice.getBusiness();

        String customerName = customer != null ? customer.getName() : "Customer";
        String customerPhone = customer != null ? customer.getPhone() : "";
        String customerEmail = customer != null ? customer.getEmail() : "";
        String invoiceNumber = invoice.getInvoiceNumber();
        String businessName = business != null ? business.getName() : "Our Business";
        String amount = "Rs. " + (invoice.getBalanceAmount() != null ? invoice.getBalanceAmount().setScale(2, RoundingMode.HALF_UP) : "0.00");

        String textMessage = String.format(
                "Payment Reminder from %s\n\nDear %s,\nThis is a friendly reminder for Invoice #%s.\nDate: %s\nDue Date: %s\nOutstanding Balance: %s\nStatus: %s\n\nPlease settle this invoice at your earliest convenience. Thank you!",
                businessName, customerName, invoiceNumber, invoice.getInvoiceDate(),
                invoice.getDueDate() != null ? invoice.getDueDate() : "Due on receipt",
                amount, invoice.getStatus()
        );

        String cleanPhone = customerPhone != null ? customerPhone.replaceAll("[^0-9]", "") : "";
        if (cleanPhone.length() == 10) {
            cleanPhone = "91" + cleanPhone;
        }

        String encodedText = java.net.URLEncoder.encode(textMessage, java.nio.charset.StandardCharsets.UTF_8);
        String whatsappUrl = cleanPhone.isEmpty()
                ? "https://wa.me/?text=" + encodedText
                : "https://wa.me/" + cleanPhone + "?text=" + encodedText;

        return new ReminderResponse(
                customerName,
                customerPhone,
                customerEmail,
                invoiceNumber,
                amount,
                textMessage,
                whatsappUrl,
                false,
                invoice.getStatus()
        );
    }

    public ReminderResponse sendInvoiceReminder(Long id, String customNote) {
        Invoice invoice = getInvoiceById(id);
        ReminderResponse details = getReminderDetails(id);

        if (invoice.getCustomer() == null || invoice.getCustomer().getEmail() == null || invoice.getCustomer().getEmail().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer does not have an email address configured. You can use the WhatsApp reminder option."
            );
        }

        boolean sent = emailService.sendInvoiceReminderEmail(invoice, customNote);
        return new ReminderResponse(
                details.customerName(),
                details.customerPhone(),
                details.customerEmail(),
                details.invoiceNumber(),
                details.amount(),
                details.messageText(),
                details.whatsappUrl(),
                sent,
                details.status()
        );
    }
}
