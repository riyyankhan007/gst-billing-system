package com.gstbilling.gst_billing.service;

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
import java.util.List;

@Service
public class NoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final CurrentUserService currentUserService;
    private final InvoiceSequenceService sequenceService;

    public NoteService(CreditNoteRepository creditNoteRepository,
                       DebitNoteRepository debitNoteRepository,
                       InvoiceRepository invoiceRepository,
                       CustomerRepository customerRepository,
                       BusinessRepository businessRepository,
                       CurrentUserService currentUserService,
                       InvoiceSequenceService sequenceService) {
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.businessRepository = businessRepository;
        this.currentUserService = currentUserService;
        this.sequenceService = sequenceService;
    }

    // =====================================
    // Credit Note
    // =====================================

    @Transactional
    public CreditNote createCreditNote(CreditNote note) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        Invoice invoice = invoiceRepository.findByIdAndBusiness_Id(note.getInvoiceId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with ID: " + note.getInvoiceId()));

        Customer customer = invoice.getCustomer();
        note.setBusiness(business);
        note.setInvoice(invoice);
        note.setCustomer(customer);
        note.setNoteDate(note.getNoteDate() != null ? note.getNoteDate() : LocalDate.now());
        note.setCreatedAt(LocalDateTime.now());
        note.setStatus("ISSUED");

        BigDecimal taxable = note.getTaxableAmount() != null ? note.getTaxableAmount() : BigDecimal.ZERO;
        BigDecimal tax = note.getTotalTax() != null ? note.getTotalTax() : BigDecimal.ZERO;

        if (taxable.compareTo(BigDecimal.ZERO) < 0 || tax.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit note taxable amount and tax must be non-negative.");
        }

        BigDecimal grandTotal = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);
        if (grandTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit note grand total must be greater than zero.");
        }

        BigDecimal invGrand = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        if (grandTotal.compareTo(invGrand) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Credit note amount (₹%s) cannot exceed original invoice amount (₹%s)", grandTotal, invGrand));
        }

        // Concurrency-safe number generation
        String noteNumber = sequenceService.generateNextCreditNoteNumber(business, note.getNoteDate());
        note.setCreditNoteNumber(noteNumber);

        // Taxes
        boolean isIntra = invoice.getSupplierState() != null && invoice.getCustomerState() != null
                && invoice.getSupplierState().trim().equalsIgnoreCase(invoice.getCustomerState().trim());

        note.setGrandTotal(grandTotal);

        if (isIntra) {
            BigDecimal half = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            note.setCgst(half);
            note.setSgst(tax.subtract(half));
            note.setIgst(BigDecimal.ZERO);
        } else {
            note.setCgst(BigDecimal.ZERO);
            note.setSgst(BigDecimal.ZERO);
            note.setIgst(tax);
        }

        CreditNote saved = creditNoteRepository.save(note);

        // Adjust original invoice balance
        BigDecimal invBal = invoice.getBalanceAmount() != null ? invoice.getBalanceAmount() : invoice.getGrandTotal();
        BigDecimal newBal = invBal.subtract(grandTotal).setScale(2, RoundingMode.HALF_UP);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;
        invoice.setBalanceAmount(newBal);

        if (newBal.compareTo(BigDecimal.ZERO) == 0 && "ISSUED".equalsIgnoreCase(invoice.getStatus())) {
            invoice.setStatus("PAID");
        }
        invoiceRepository.save(invoice);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CreditNote> getAllCreditNotes() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return creditNoteRepository.findByBusiness_IdOrderByNoteDateDescIdDesc(businessId);
    }

    @Transactional(readOnly = true)
    public CreditNote getCreditNoteById(Long id) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return creditNoteRepository.findByIdAndBusiness_Id(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit note not found with ID: " + id));
    }

    @Transactional
    public CreditNote cancelCreditNote(Long id) {
        CreditNote note = getCreditNoteById(id);
        if ("CANCELLED".equalsIgnoreCase(note.getStatus())) return note;

        note.setStatus("CANCELLED");
        Invoice inv = note.getInvoice();
        if (inv != null) {
            BigDecimal currentBal = inv.getBalanceAmount() != null ? inv.getBalanceAmount() : BigDecimal.ZERO;
            inv.setBalanceAmount(currentBal.add(note.getGrandTotal()).setScale(2, RoundingMode.HALF_UP));
            invoiceRepository.save(inv);
        }

        return creditNoteRepository.save(note);
    }

    // =====================================
    // Debit Note
    // =====================================

    @Transactional
    public DebitNote createDebitNote(DebitNote note) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        Invoice invoice = null;
        Customer customer = null;

        if (note.getInvoiceId() != null) {
            invoice = invoiceRepository.findByIdAndBusiness_Id(note.getInvoiceId(), businessId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with ID: " + note.getInvoiceId()));
            customer = invoice.getCustomer();
        }

        note.setBusiness(business);
        note.setInvoice(invoice);
        note.setCustomer(customer);
        note.setNoteDate(note.getNoteDate() != null ? note.getNoteDate() : LocalDate.now());
        note.setCreatedAt(LocalDateTime.now());
        note.setStatus("ISSUED");

        BigDecimal taxable = note.getTaxableAmount() != null ? note.getTaxableAmount() : BigDecimal.ZERO;
        BigDecimal tax = note.getTotalTax() != null ? note.getTotalTax() : BigDecimal.ZERO;

        if (taxable.compareTo(BigDecimal.ZERO) < 0 || tax.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit note taxable amount and tax must be non-negative.");
        }

        BigDecimal grandTotal = taxable.add(tax).setScale(2, RoundingMode.HALF_UP);
        if (grandTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit note grand total must be greater than zero.");
        }

        // Concurrency-safe number generation
        String noteNumber = sequenceService.generateNextDebitNoteNumber(business, note.getNoteDate());
        note.setDebitNoteNumber(noteNumber);

        note.setGrandTotal(grandTotal);

        boolean isIntra = invoice != null && invoice.getSupplierState() != null && invoice.getCustomerState() != null
                && invoice.getSupplierState().trim().equalsIgnoreCase(invoice.getCustomerState().trim());

        if (isIntra) {
            BigDecimal half = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            note.setCgst(half);
            note.setSgst(tax.subtract(half));
            note.setIgst(BigDecimal.ZERO);
        } else {
            note.setCgst(BigDecimal.ZERO);
            note.setSgst(BigDecimal.ZERO);
            note.setIgst(tax);
        }

        DebitNote saved = debitNoteRepository.save(note);

        if (invoice != null) {
            BigDecimal invBal = invoice.getBalanceAmount() != null ? invoice.getBalanceAmount() : invoice.getGrandTotal();
            invoice.setBalanceAmount(invBal.add(grandTotal).setScale(2, RoundingMode.HALF_UP));
            invoiceRepository.save(invoice);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<DebitNote> getAllDebitNotes() {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return debitNoteRepository.findByBusiness_IdOrderByNoteDateDescIdDesc(businessId);
    }

    @Transactional(readOnly = true)
    public DebitNote getDebitNoteById(Long id) {
        Long businessId = currentUserService.getCurrentUser().getBusiness().getId();
        return debitNoteRepository.findByIdAndBusiness_Id(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Debit note not found with ID: " + id));
    }

    @Transactional
    public DebitNote cancelDebitNote(Long id) {
        DebitNote note = getDebitNoteById(id);
        if ("CANCELLED".equalsIgnoreCase(note.getStatus())) return note;

        note.setStatus("CANCELLED");
        Invoice inv = note.getInvoice();
        if (inv != null) {
            BigDecimal currentBal = inv.getBalanceAmount() != null ? inv.getBalanceAmount() : BigDecimal.ZERO;
            BigDecimal newBal = currentBal.subtract(note.getGrandTotal()).setScale(2, RoundingMode.HALF_UP);
            if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;
            inv.setBalanceAmount(newBal);
            invoiceRepository.save(inv);
        }

        return debitNoteRepository.save(note);
    }
}
