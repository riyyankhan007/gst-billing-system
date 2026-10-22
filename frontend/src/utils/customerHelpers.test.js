import test from "node:test";
import assert from "node:assert/strict";
import {
    resolveCustomerInvoices,
    getCustomerInvoiceCount,
    filterCustomerInvoices,
    calculateCustomerAggregates
} from "./customerHelpers.js";

test("resolveCustomerInvoices - correctly extracts invoices when key is 'invoices'", () => {
    const data = {
        invoices: [
            { id: 1, invoiceNumber: "INV-001", status: "ISSUED" },
            { id: 2, invoiceNumber: "INV-002", status: "PAID" }
        ]
    };
    const resolved = resolveCustomerInvoices(data);
    assert.equal(resolved.length, 2);
    assert.equal(resolved[0].invoiceNumber, "INV-001");
});

test("resolveCustomerInvoices - backward compatibility: correctly extracts invoices when key is 'recentInvoices'", () => {
    const data = {
        recentInvoices: [
            { id: 10, invoiceNumber: "INV-010", status: "ISSUED" },
            { id: 11, invoiceNumber: "INV-011", status: "CANCELLED" }
        ]
    };
    const resolved = resolveCustomerInvoices(data);
    assert.equal(resolved.length, 2);
    assert.equal(resolved[0].invoiceNumber, "INV-010");
});

test("resolveCustomerInvoices - returns empty array when data or invoices are missing", () => {
    assert.deepEqual(resolveCustomerInvoices(null), []);
    assert.deepEqual(resolveCustomerInvoices({}), []);
    assert.deepEqual(resolveCustomerInvoices({ invoices: null }), []);
});

test("getCustomerInvoiceCount - uses totalInvoicesCount when provided", () => {
    const data = {
        totalInvoicesCount: 7,
        invoices: [{ id: 1 }]
    };
    assert.equal(getCustomerInvoiceCount(data), 7);
});

test("getCustomerInvoiceCount - falls back to array length when totalInvoicesCount missing", () => {
    const data = {
        invoices: [{ id: 1 }, { id: 2 }, { id: 3 }]
    };
    assert.equal(getCustomerInvoiceCount(data), 3);
});

test("filterCustomerInvoices - default ALL includes all statuses including CANCELLED and DRAFT", () => {
    const invoices = [
        { invoiceNumber: "INV-001", status: "ISSUED" },
        { invoiceNumber: "INV-002", status: "PAID" },
        { invoiceNumber: "INV-003", status: "CANCELLED" },
        { invoiceNumber: "INV-004", status: "DRAFT" },
        { invoiceNumber: "INV-005", status: "OVERDUE" }
    ];
    const filtered = filterCustomerInvoices(invoices, "", "ALL");
    assert.equal(filtered.length, 5);
});

test("filterCustomerInvoices - filters by status case-insensitively", () => {
    const invoices = [
        { invoiceNumber: "INV-001", status: "ISSUED" },
        { invoiceNumber: "INV-002", status: "PAID" },
        { invoiceNumber: "INV-003", status: "CANCELLED" }
    ];
    const cancelled = filterCustomerInvoices(invoices, "", "cancelled");
    assert.equal(cancelled.length, 1);
    assert.equal(cancelled[0].invoiceNumber, "INV-003");
});

test("filterCustomerInvoices - searches by invoice number and notes", () => {
    const invoices = [
        { invoiceNumber: "INV/2026-27/0007", notes: "Annual AMC contract" },
        { invoiceNumber: "INV/2026-27/0006", notes: "Monthly retainer" },
        { invoiceNumber: "INV/2026-27/0005", notes: "Hardware delivery" }
    ];
    const matchNum = filterCustomerInvoices(invoices, "0007", "ALL");
    assert.equal(matchNum.length, 1);
    assert.equal(matchNum[0].invoiceNumber, "INV/2026-27/0007");

    const matchNote = filterCustomerInvoices(invoices, "retainer", "ALL");
    assert.equal(matchNote.length, 1);
    assert.equal(matchNote[0].invoiceNumber, "INV/2026-27/0006");
});

test("calculateCustomerAggregates - correctly aggregates GST, ordered items, and running balance ledger", () => {
    const data = {
        customer: { name: "Test Corp", openingBalance: "100.00", createdAt: "2026-01-01T00:00:00" },
        invoices: [
            {
                id: 1,
                invoiceNumber: "INV-001",
                invoiceDate: "2026-01-10",
                status: "ISSUED",
                taxableAmount: "1000.00",
                cgst: "90.00",
                sgst: "90.00",
                igst: "0.00",
                totalTax: "180.00",
                grandTotal: "1180.00",
                items: [
                    { productName: "Widget A", quantity: 2, unitPrice: 500, totalAmount: 1180, taxableAmount: 1000, taxAmount: 180 }
                ]
            },
            {
                id: 2,
                invoiceNumber: "INV-002",
                invoiceDate: "2026-01-15",
                status: "CANCELLED", // Must not contribute to tax or turnover
                taxableAmount: "500.00",
                totalTax: "90.00",
                grandTotal: "590.00"
            }
        ]
    };
    const payments = [
        { id: 101, paymentDate: "2026-01-12", amount: "500.00", paymentMethod: "UPI" }
    ];

    const agg = calculateCustomerAggregates(data, payments);

    // Turnover & tax: only INV-001 should be included
    assert.equal(agg.taxableTurnover, 1000);
    assert.equal(agg.totalTax, 180);
    assert.equal(agg.totalCgst, 90);
    assert.equal(agg.totalSgst, 90);

    // Ordered items
    assert.equal(agg.orderedItems.length, 1);
    assert.equal(agg.orderedItems[0].name, "Widget A");
    assert.equal(agg.orderedItems[0].totalQty, 2);

    // Ledger entries: Opening Balance (100) -> Invoice 1 (+1180) -> Payment (-500)
    assert.equal(agg.ledgerEntries.length, 3);
    assert.equal(agg.ledgerEntries[0].type, "OPENING_BALANCE");
    assert.equal(agg.ledgerEntries[0].runningBalance, 100);

    assert.equal(agg.ledgerEntries[1].type, "INVOICE");
    assert.equal(agg.ledgerEntries[1].runningBalance, 1280); // 100 + 1180

    assert.equal(agg.ledgerEntries[2].type, "PAYMENT");
    assert.equal(agg.ledgerEntries[2].runningBalance, 780); // 1280 - 500
});
