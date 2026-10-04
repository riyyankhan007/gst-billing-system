package com.gstbilling.gst_billing.dto;

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
        List<Invoice> recentInvoices
) {}
