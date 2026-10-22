/**
 * Helper utilities for customer detail data resolution, filtering, and aggregates.
 */

/**
 * Safely resolves customer invoices from API response whether provided as 'invoices' or 'recentInvoices'.
 */
export function resolveCustomerInvoices(data) {
    if (!data) return [];
    if (Array.isArray(data.invoices)) return data.invoices;
    if (Array.isArray(data.recentInvoices)) return data.recentInvoices;
    return [];
}

/**
 * Returns the effective total invoice count, favoring totalInvoicesCount from backend DTO.
 */
export function getCustomerInvoiceCount(data) {
    if (!data) return 0;
    if (typeof data.totalInvoicesCount === "number") {
        return data.totalInvoicesCount;
    }
    return resolveCustomerInvoices(data).length;
}

/**
 * Filters invoices by search query (invoice number or notes) and status.
 */
export function filterCustomerInvoices(invoices, search = "", statusFilter = "ALL") {
    if (!Array.isArray(invoices)) return [];
    const q = (search || "").trim().toLowerCase();
    const filter = (statusFilter || "ALL").toUpperCase();

    return invoices.filter(inv => {
        const status = (inv.status || "").toUpperCase();
        const matchesStatus = filter === "ALL" || status === filter;
        const matchesSearch = !q ||
            (inv.invoiceNumber && inv.invoiceNumber.toLowerCase().includes(q)) ||
            (inv.notes && inv.notes.toLowerCase().includes(q));
        return matchesStatus && matchesSearch;
    });
}

/**
 * Calculates aggregate GST breakdown, ordered products, and chronological ledger entries.
 */
export function calculateCustomerAggregates(data, payments = []) {
    if (!data) {
        return {
            totalCgst: 0,
            totalSgst: 0,
            totalIgst: 0,
            totalTax: 0,
            taxableTurnover: 0,
            orderedItems: [],
            ledgerEntries: []
        };
    }

    const rawInvoices = resolveCustomerInvoices(data);

    let cgstSum = 0;
    let sgstSum = 0;
    let igstSum = 0;
    let taxSum = 0;
    let taxableSum = 0;
    const itemMap = new Map();

    const activeInvoices = rawInvoices.filter(
        inv => inv.status !== "CANCELLED" && inv.status !== "DRAFT"
    );

    rawInvoices.forEach(inv => {
        if (inv.status !== "CANCELLED") {
            cgstSum += Number(inv.cgst || 0);
            sgstSum += Number(inv.sgst || 0);
            igstSum += Number(inv.igst || 0);
            taxSum += Number(inv.totalTax || 0);
            taxableSum += Number(inv.taxableAmount || 0);
        }

        // Aggregate items
        if (Array.isArray(inv.items)) {
            inv.items.forEach(item => {
                const key = (item.productName || item.itemDescription || "Product").trim().toLowerCase();
                const existing = itemMap.get(key) || {
                    name: item.productName || item.itemDescription || "Item",
                    hsnCode: item.hsnCode || "—",
                    unit: item.unit || "PCS",
                    totalQty: 0,
                    totalAmount: 0,
                    taxableAmount: 0,
                    taxAmount: 0,
                    gstRate: item.gstRate || 18,
                    orderCount: 0,
                    lastDate: inv.invoiceDate
                };

                existing.totalQty += Number(item.quantity || 0);
                existing.totalAmount += Number(item.totalAmount || item.total || 0);
                existing.taxableAmount += Number(item.taxableAmount || 0);
                existing.taxAmount += Number(item.taxAmount || 0);
                existing.orderCount += 1;
                if (inv.invoiceDate && (!existing.lastDate || inv.invoiceDate > existing.lastDate)) {
                    existing.lastDate = inv.invoiceDate;
                }
                itemMap.set(key, existing);
            });
        }
    });

    // Build Chronological Ledger Entries
    const customer = data.customer;
    const opening = Number(customer?.openingBalance || 0);
    const entries = [];

    if (opening !== 0) {
        entries.push({
            date: customer?.createdAt ? customer.createdAt.substring(0, 10) : "Initial",
            type: "OPENING_BALANCE",
            reference: "Opening Balance",
            description: "Customer opening balance brought forward",
            debit: opening > 0 ? opening : 0,
            credit: opening < 0 ? Math.abs(opening) : 0,
            timestamp: 0
        });
    }

    activeInvoices.forEach(inv => {
        const grandTotal = Number(inv.grandTotal || 0);
        entries.push({
            date: inv.invoiceDate || "—",
            type: "INVOICE",
            reference: inv.invoiceNumber,
            description: `Tax Invoice #${inv.invoiceNumber}`,
            debit: grandTotal,
            credit: 0,
            invoiceId: inv.id,
            timestamp: new Date(inv.invoiceDate || 0).getTime()
        });
    });

    (payments || []).forEach(pay => {
        const amt = Number(pay.amount || 0);
        entries.push({
            date: pay.paymentDate || "—",
            type: "PAYMENT",
            reference: pay.referenceNumber || `REC-${pay.id}`,
            description: `Payment received via ${pay.paymentMethod || "Bank Transfer"}`,
            debit: 0,
            credit: amt,
            paymentId: pay.id,
            timestamp: new Date(pay.paymentDate || 0).getTime()
        });
    });

    // Sort ledger chronologically
    entries.sort((a, b) => a.timestamp - b.timestamp);

    // Compute running balance
    let runBal = 0;
    entries.forEach(ent => {
        runBal += (ent.debit - ent.credit);
        ent.runningBalance = runBal;
    });

    return {
        totalCgst: cgstSum,
        totalSgst: sgstSum,
        totalIgst: igstSum,
        totalTax: taxSum,
        taxableTurnover: taxableSum,
        orderedItems: Array.from(itemMap.values()).sort((a, b) => b.totalAmount - a.totalAmount),
        ledgerEntries: entries
    };
}
