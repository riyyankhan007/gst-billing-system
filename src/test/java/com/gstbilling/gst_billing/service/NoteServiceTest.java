package com.gstbilling.gst_billing.service;

import com.gstbilling.gst_billing.entity.Business;
import com.gstbilling.gst_billing.entity.CreditNote;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;
import com.gstbilling.gst_billing.entity.User;
import com.gstbilling.gst_billing.entity.UserRole;
import com.gstbilling.gst_billing.repository.BusinessRepository;
import com.gstbilling.gst_billing.repository.CustomerRepository;
import com.gstbilling.gst_billing.repository.InvoiceRepository;
import com.gstbilling.gst_billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NoteServiceTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired private NoteService noteService;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private UserRepository userRepository;

    private Business business;
    private Customer customer;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        business = new Business();
        business.setName("Note Test Business");
        business.setState("Maharashtra");
        business.setStateCode("27");
        business.setFinancialYear("2026-27");
        business = businessRepository.save(business);

        String email = "notetester_" + System.currentTimeMillis() + "@example.com";
        User user = new User();
        user.setName("Note Tester");
        user.setEmail(email);
        user.setPassword("secret");
        user.setUserRole(UserRole.OWNER);
        user.setBusiness(business);
        userRepository.save(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList())
        );

        customer = new Customer();
        customer.setName("Note Customer");
        customer.setBusiness(business);
        customer.setState("Maharashtra");
        customer.setStateCode("27");
        customer = customerRepository.save(customer);

        String invNum = "INV-TEST-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 6);
        invoice = new Invoice();
        invoice.setBusiness(business);
        invoice.setCustomer(customer);
        invoice.setInvoiceNumber(invNum);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setSupplierState("Maharashtra");
        invoice.setCustomerState("Maharashtra");
        invoice.setTaxableAmount(new BigDecimal("1000.00"));
        invoice.setTotalTax(new BigDecimal("180.00"));
        invoice.setCgst(new BigDecimal("90.00"));
        invoice.setSgst(new BigDecimal("90.00"));
        invoice.setIgst(BigDecimal.ZERO);
        invoice.setGrandTotal(new BigDecimal("1180.00"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalanceAmount(new BigDecimal("1180.00"));
        invoice.setStatus("ISSUED");
        invoice = invoiceRepository.save(invoice);
    }

    @Test
    @DisplayName("Should successfully create Credit Note, split taxes for intra-state, and deduct invoice balance")
    void shouldCreateCreditNoteAndAdjustBalance() {
        CreditNote note = new CreditNote();
        note.setInvoiceId(invoice.getId());
        note.setReason("Sales Return");
        note.setTaxableAmount(new BigDecimal("500.00"));
        note.setTotalTax(new BigDecimal("90.00"));
        note.setNoteDate(LocalDate.now());

        CreditNote saved = noteService.createCreditNote(note);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreditNoteNumber());
        assertTrue(saved.getCreditNoteNumber().startsWith("CN/"));
        assertEquals(0, new BigDecimal("45.00").compareTo(saved.getCgst()));
        assertEquals(0, new BigDecimal("45.00").compareTo(saved.getSgst()));
        assertEquals(0, new BigDecimal("590.00").compareTo(saved.getGrandTotal()));

        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("590.00").compareTo(updatedInvoice.getBalanceAmount()));
        assertEquals("ISSUED", updatedInvoice.getStatus());
    }

    @Test
    @DisplayName("Should reject Credit Note if grand total exceeds parent invoice grand total")
    void shouldRejectCreditNoteExceedingInvoiceAmount() {
        CreditNote note = new CreditNote();
        note.setInvoiceId(invoice.getId());
        note.setReason("Sales Return");
        note.setTaxableAmount(new BigDecimal("2000.00"));
        note.setTotalTax(new BigDecimal("360.00"));
        note.setNoteDate(LocalDate.now());

        assertThrows(ResponseStatusException.class, () -> noteService.createCreditNote(note));
    }

    @Test
    @DisplayName("Should reject Credit Note with zero or negative total")
    void shouldRejectCreditNoteWithZeroTotal() {
        CreditNote note = new CreditNote();
        note.setInvoiceId(invoice.getId());
        note.setReason("Correction");
        note.setTaxableAmount(BigDecimal.ZERO);
        note.setTotalTax(BigDecimal.ZERO);
        note.setNoteDate(LocalDate.now());

        assertThrows(ResponseStatusException.class, () -> noteService.createCreditNote(note));
    }
}
