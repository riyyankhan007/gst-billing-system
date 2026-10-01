package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
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
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUserService currentUserService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            CurrentUserService currentUserService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Invoice createInvoice(Invoice invoice) {

        // Temporary invoice number
        invoice.setInvoiceNumber(
                "TEMP-" + System.currentTimeMillis()
        );

        // The business is always derived from the JWT-authenticated user.
        Business business = currentUserService.getCurrentUser().getBusiness();

        // Attach business to invoice
        invoice.setBusiness(business);

        // Fetch customer
        Customer customer = customerRepository.findByIdAndBusinessId(invoice.getCustomerId(), business.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found: " + invoice.getCustomerId()
                        )
                );

        // Attach customer to invoice
        invoice.setCustomer(customer);

        // Supplier state comes from business
        invoice.setSupplierState(business.getState());

        // Customer state comes from customer
        invoice.setCustomerState(customer.getState());

        // Invoice date
        invoice.setInvoiceDate(
                invoice.getInvoiceDate() != null
                        ? invoice.getInvoiceDate()
                        : LocalDate.now()
        );

        // Creation timestamp
        invoice.setCreatedAt(LocalDateTime.now());

        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        // Determine intra-state or inter-state
        boolean intraState = invoice.getSupplierState()
                .equalsIgnoreCase(invoice.getCustomerState());

        for (InvoiceItem item : invoice.getItems()) {

            // Fetch product from database
            Product product = productRepository.findByIdAndBusinessId(item.getProductId(), business.getId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Product not found: " + item.getProductId()
                            )
                    );

            // Copy product details into invoice item
            item.setProductName(product.getName());
            item.setHsnCode(product.getHsnCode());
            item.setUnitPrice(product.getPrice());
            item.setGstRate(product.getGstRate());

            // Quantity × Unit Price
            BigDecimal itemTaxableAmount = item.getQuantity()
                    .multiply(item.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            // GST calculation
            BigDecimal itemTax = itemTaxableAmount
                    .multiply(item.getGstRate())
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );

            // Taxable amount + GST
            BigDecimal itemTotal = itemTaxableAmount
                    .add(itemTax)
                    .setScale(2, RoundingMode.HALF_UP);

            // Store calculated values
            item.setTaxableAmount(itemTaxableAmount);
            item.setTaxAmount(itemTax);
            item.setTotalAmount(itemTotal);

            // Connect item to invoice
            item.setInvoice(invoice);

            // Add to invoice totals
            taxableAmount = taxableAmount.add(itemTaxableAmount);
            totalTax = totalTax.add(itemTax);
        }

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        if (intraState) {

            // Intra-state → CGST + SGST
            cgst = totalTax
                    .divide(
                            BigDecimal.valueOf(2),
                            2,
                            RoundingMode.HALF_UP
                    );

            sgst = totalTax.subtract(cgst);

        } else {

            // Inter-state → IGST
            igst = totalTax;
        }

        // Taxable amount + GST
        BigDecimal grandTotal = taxableAmount
                .add(totalTax)
                .setScale(2, RoundingMode.HALF_UP);

        // Set invoice totals
        invoice.setTaxableAmount(taxableAmount);
        invoice.setCgst(cgst);
        invoice.setSgst(sgst);
        invoice.setIgst(igst);
        invoice.setTotalTax(totalTax);
        invoice.setGrandTotal(grandTotal);

        // Default status
        if (invoice.getStatus() == null) {
            invoice.setStatus("DRAFT");
        }

        // First save → generate database ID
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Generate final invoice number
        String invoiceNumber = String.format(
                "%s-%03d",
                business.getInvoicePrefix() == null || business.getInvoicePrefix().isBlank() ? "INV" : business.getInvoicePrefix(), savedInvoice.getId()
        );

        savedInvoice.setInvoiceNumber(invoiceNumber);

        // Save final invoice
        return invoiceRepository.save(savedInvoice);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findByBusiness_Id(currentUserService.getCurrentUser().getBusiness().getId());
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findByIdAndBusiness_Id(id, currentUserService.getCurrentUser().getBusiness().getId())
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Invoice is not available to this business")
                );
    }
    public Invoice markAsPaid(Long id) {
        Invoice invoice = getInvoiceById(id);

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Only draft invoices can be marked as paid");
        }

        invoice.setStatus("PAID");
        return invoiceRepository.save(invoice);
    }

    public Invoice cancelInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Only draft invoices can be cancelled");
        }

        invoice.setStatus("CANCELLED");
        return invoiceRepository.save(invoice);
    }
}
