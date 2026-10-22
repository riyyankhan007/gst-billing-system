package com.gstbilling.gst_billing.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gstbilling.gst_billing.entity.Customer;
import com.gstbilling.gst_billing.entity.Invoice;

import java.math.BigDecimal;
import java.util.List;

public record CustomerDetailsResponse(
        Customer customer,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance,
        BigDecimal overdueAmount,
        int totalInvoicesCount,
        int pendingInvoicesCount,
        @JsonProperty("invoices")
        @JsonAlias({"invoices", "recentInvoices"})
        List<Invoice> invoices
) {
    @JsonProperty("recentInvoices")
    public List<Invoice> recentInvoices() {
        return invoices;
    }
}
