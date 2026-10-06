package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.InvoiceSequence;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.InvoiceSequenceRepository;
import com.gstbilling.gst_billing.util.FinancialYearUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class InvoiceSequenceService {

    private final InvoiceSequenceRepository sequenceRepository;
    private final InvoiceRepository invoiceRepository;
    private final BusinessRepository businessRepository;

    public InvoiceSequenceService(
            InvoiceSequenceRepository sequenceRepository,
            InvoiceRepository invoiceRepository,
            BusinessRepository businessRepository
    ) {
        this.sequenceRepository = sequenceRepository;
        this.invoiceRepository = invoiceRepository;
        this.businessRepository = businessRepository;
    }

    /**
     * Generates a concurrency-safe, gapless invoice number for the given business and date.
     * Financial year is automatically determined based on Indian tax rules (April 1 to March 31).
     *
     * @param business The business for which to generate the number
     * @param invoiceDate Date of the invoice
     * @return Formatted sequential invoice number (e.g. "INV/2026-27/0001")
     */
    @Transactional
    public String generateNextInvoiceNumber(Business business, LocalDate invoiceDate) {
        String fy = FinancialYearUtil.getFinancialYear(invoiceDate);
        String prefix = (business.getInvoicePrefix() != null && !business.getInvoicePrefix().isBlank())
                ? business.getInvoicePrefix().trim().toUpperCase() : "INV";

        // Acquire pessimistic row lock on sequence record
        InvoiceSequence sequence = sequenceRepository
                .findByBusinessAndFyAndDocTypeForUpdate(business.getId(), fy, "INVOICE")
                .orElseGet(() -> {
                    // Initialize sequence row for this (business, FY, docType)
                    long initialSeq = 0L;
                    // If business already has an invoiceSeqNumber in this FY, initialize from it
                    if (fy.equalsIgnoreCase(business.getFinancialYear())
                            && business.getInvoiceSeqNumber() != null
                            && business.getInvoiceSeqNumber() > 1L) {
                        initialSeq = business.getInvoiceSeqNumber() - 1L;
                    }
                    InvoiceSequence newSeq = new InvoiceSequence(business, fy, "INVOICE", prefix, initialSeq);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        long candidateSeq = sequence.getCurrentSequence() + 1;
        String formattedNumber = String.format("%s/%s/%04d", prefix, fy, candidateSeq);

        // Guarantee no collision with pre-existing manual numbers
        while (invoiceRepository.existsByBusiness_IdAndInvoiceNumberIgnoreCase(business.getId(), formattedNumber)) {
            candidateSeq++;
            formattedNumber = String.format("%s/%s/%04d", prefix, fy, candidateSeq);
        }

        sequence.setCurrentSequence(candidateSeq);
        sequence.setPrefix(prefix);
        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);

        // Keep business entity in sync
        business.setInvoiceSeqNumber(candidateSeq + 1);
        business.setFinancialYear(fy);
        businessRepository.save(business);

        return formattedNumber;
    }

    /**
     * Generates a concurrency-safe, gapless payment receipt number for the given business and date.
     * Format: RCP/2026-27/0001
     */
    @Transactional
    public String generateNextReceiptNumber(Business business, LocalDate receiptDate) {
        String fy = FinancialYearUtil.getFinancialYear(receiptDate != null ? receiptDate : LocalDate.now());
        String prefix = "RCP";

        InvoiceSequence sequence = sequenceRepository
                .findByBusinessAndFyAndDocTypeForUpdate(business.getId(), fy, "PAYMENT_RECEIPT")
                .orElseGet(() -> {
                    InvoiceSequence newSeq = new InvoiceSequence(business, fy, "PAYMENT_RECEIPT", prefix, 0L);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        long candidateSeq = sequence.getCurrentSequence() + 1;
        String formattedNumber = String.format("%s/%s/%04d", prefix, fy, candidateSeq);

        sequence.setCurrentSequence(candidateSeq);
        sequence.setPrefix(prefix);
        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);

        return formattedNumber;
    }

    /**
     * Generates a concurrency-safe, gapless Credit Note number.
     * Format: CN/2026-27/0001
     */
    @Transactional
    public String generateNextCreditNoteNumber(Business business, LocalDate noteDate) {
        String fy = FinancialYearUtil.getFinancialYear(noteDate != null ? noteDate : LocalDate.now());
        String prefix = "CN";

        InvoiceSequence sequence = sequenceRepository
                .findByBusinessAndFyAndDocTypeForUpdate(business.getId(), fy, "CREDIT_NOTE")
                .orElseGet(() -> {
                    InvoiceSequence newSeq = new InvoiceSequence(business, fy, "CREDIT_NOTE", prefix, 0L);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        long candidateSeq = sequence.getCurrentSequence() + 1;
        String formattedNumber = String.format("%s/%s/%04d", prefix, fy, candidateSeq);

        sequence.setCurrentSequence(candidateSeq);
        sequence.setPrefix(prefix);
        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);

        return formattedNumber;
    }

    /**
     * Generates a concurrency-safe, gapless Debit Note number.
     * Format: DN/2026-27/0001
     */
    @Transactional
    public String generateNextDebitNoteNumber(Business business, LocalDate noteDate) {
        String fy = FinancialYearUtil.getFinancialYear(noteDate != null ? noteDate : LocalDate.now());
        String prefix = "DN";

        InvoiceSequence sequence = sequenceRepository
                .findByBusinessAndFyAndDocTypeForUpdate(business.getId(), fy, "DEBIT_NOTE")
                .orElseGet(() -> {
                    InvoiceSequence newSeq = new InvoiceSequence(business, fy, "DEBIT_NOTE", prefix, 0L);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        long candidateSeq = sequence.getCurrentSequence() + 1;
        String formattedNumber = String.format("%s/%s/%04d", prefix, fy, candidateSeq);

        sequence.setCurrentSequence(candidateSeq);
        sequence.setPrefix(prefix);
        sequence.setUpdatedAt(LocalDateTime.now());
        sequenceRepository.save(sequence);

        return formattedNumber;
    }
}
