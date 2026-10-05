package com.gstbilling.gst_billing.controller;

import com.gstbilling.gst_billing.entity.CreditNote;
import com.gstbilling.gst_billing.entity.DebitNote;
import com.gstbilling.gst_billing.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    // =====================================
    // Credit Notes
    // =====================================

    @PostMapping("/credit-notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<CreditNote> createCreditNote(@RequestBody CreditNote note) {
        CreditNote created = noteService.createCreditNote(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/credit-notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<CreditNote> getAllCreditNotes() {
        return noteService.getAllCreditNotes();
    }

    @GetMapping("/credit-notes/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public CreditNote getCreditNoteById(@PathVariable Long id) {
        return noteService.getCreditNoteById(id);
    }

    @PutMapping("/credit-notes/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public CreditNote cancelCreditNote(@PathVariable Long id) {
        return noteService.cancelCreditNote(id);
    }

    // =====================================
    // Debit Notes
    // =====================================

    @PostMapping("/debit-notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<DebitNote> createDebitNote(@RequestBody DebitNote note) {
        DebitNote created = noteService.createDebitNote(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/debit-notes")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public List<DebitNote> getAllDebitNotes() {
        return noteService.getAllDebitNotes();
    }

    @GetMapping("/debit-notes/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'VIEWER', 'SUPPORT')")
    public DebitNote getDebitNoteById(@PathVariable Long id) {
        return noteService.getDebitNoteById(id);
    }

    @PutMapping("/debit-notes/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'ACCOUNTANT')")
    public DebitNote cancelDebitNote(@PathVariable Long id) {
        return noteService.cancelDebitNote(id);
    }
}
