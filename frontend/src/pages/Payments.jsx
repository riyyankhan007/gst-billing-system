import React, { useEffect, useState } from "react";
import { getPayments, recordPayment, deletePayment, getInvoices, downloadPaymentReceiptPdf, reconcileAllPayments } from "../services/api";

export default function Payments({ onSelectInvoice }) {
    const [payments, setPayments] = useState([]);
    const [invoices, setInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [reconciling, setReconciling] = useState(false);

    // Modal state
    const [showModal, setShowModal] = useState(false);
    const [selectedInvoice, setSelectedInvoice] = useState(null);
    const [amount, setAmount] = useState("");
    const [paymentDate, setPaymentDate] = useState(new Date().toISOString().split("T")[0]);
    const [paymentMethod, setPaymentMethod] = useState("UPI");
    const [referenceNumber, setReferenceNumber] = useState("");
    const [notes, setNotes] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [modalError, setModalError] = useState("");

    // Search filter
    const [search, setSearch] = useState("");
    const [methodFilter, setMethodFilter] = useState("ALL");

    useEffect(() => {
        loadData();
    }, []);

    async function loadData() {
        try {
            setLoading(true);
            setError("");
            const [payList, invList] = await Promise.all([
                getPayments(),
                getInvoices()
            ]);
            setPayments(payList);
            // Filter invoices that are not cancelled and have balance > 0
            setInvoices(invList.filter(i => i.status?.toUpperCase() !== "CANCELLED" && Number(i.balanceAmount != null ? i.balanceAmount : i.grandTotal || 0) > 0));
        } catch (err) {
            setError(err.message || "Failed to load payments data");
        } finally {
            setLoading(false);
        }
    }

    const fmt = val => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(val || 0));

    function openRecordModal(presetInvoice = null) {
        setModalError("");
        if (presetInvoice) {
            setSelectedInvoice(presetInvoice);
            setAmount(presetInvoice.balanceAmount || presetInvoice.grandTotal || "");
        } else if (invoices.length > 0) {
            setSelectedInvoice(invoices[0]);
            setAmount(invoices[0].balanceAmount || invoices[0].grandTotal || "");
        } else {
            setSelectedInvoice(null);
            setAmount("");
        }
        setPaymentDate(new Date().toISOString().split("T")[0]);
        setPaymentMethod("UPI");
        setReferenceNumber("");
        setNotes("");
        setShowModal(true);
    }

    async function handleRecordSubmit(e) {
        e.preventDefault();
        if (!selectedInvoice) {
            setModalError("Please select an invoice to record payment against.");
            return;
        }
        const payNum = Number(amount);
        if (!payNum || payNum <= 0) {
            setModalError("Please enter a valid positive payment amount.");
            return;
        }

        const idempotencyKey = (typeof crypto !== "undefined" && crypto.randomUUID)
            ? crypto.randomUUID()
            : ("pay_" + Date.now() + "_" + Math.random().toString(36).substring(2, 9));

        try {
            setSubmitting(true);
            setModalError("");
            await recordPayment({
                invoiceId: selectedInvoice.id,
                amount: payNum,
                paymentDate,
                paymentMethod,
                referenceNumber,
                notes
            }, idempotencyKey);
            setShowModal(false);
            setSuccessMessage(`Payment of ${fmt(payNum)} recorded successfully!`);
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            setModalError(err.message || "Failed to record payment");
        } finally {
            setSubmitting(false);
        }
    }

    async function handleDownloadReceipt(payment) {
        try {
            const blob = await downloadPaymentReceiptPdf(payment.id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `Receipt-${payment.receiptNumber || payment.id}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (err) {
            alert("Error downloading receipt PDF: " + err.message);
        }
    }

    async function handleReconcile() {
        try {
            setReconciling(true);
            const res = await reconcileAllPayments();
            setSuccessMessage(`Ledger Reconciliation Complete: verified ${res.totalInvoicesChecked} invoices, resolved ${res.totalDriftsResolved} drifts.`);
            setTimeout(() => setSuccessMessage(""), 5000);
            await loadData();
        } catch (err) {
            setError(err.message || "Failed to reconcile payments");
        } finally {
            setReconciling(false);
        }
    }

    async function handleDeletePayment(id) {
        if (!window.confirm("Are you sure you want to reverse/delete this payment record? The invoice balance will be adjusted.")) {
            return;
        }
        try {
            await deletePayment(id);
            setSuccessMessage("Payment reversed successfully.");
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            setError(err.message || "Failed to delete payment");
        }
    }

    const filteredPayments = payments.filter(p => {
        const matchesMethod = methodFilter === "ALL" || (p.paymentMethod && p.paymentMethod.toUpperCase() === methodFilter);
        const q = search.toLowerCase();
        const matchesSearch = !q
            || (p.invoiceNumber && p.invoiceNumber.toLowerCase().includes(q))
            || (p.customerName && p.customerName.toLowerCase().includes(q))
            || (p.referenceNumber && p.referenceNumber.toLowerCase().includes(q));
        return matchesMethod && matchesSearch;
    });

    const totalCollected = filteredPayments.reduce((sum, p) => sum + Number(p.amount || 0), 0);

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            {/* Header */}
            <div className="page-heading">
                <div>
                    <span className="eyebrow">RECEIVABLES & CASHFLOW</span>
                    <h1>Payments Received</h1>
                    <p>Track collections, payment receipts, and settle customer invoices</p>
                </div>
                <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                    <button
                        className="secondary-button"
                        onClick={handleReconcile}
                        disabled={reconciling}
                        title="Audit and heal any drift between payments and invoice balances"
                    >
                        {reconciling ? "Reconciling..." : "⚡ Reconcile Ledgers"}
                    </button>
                    <button className="primary-button" onClick={() => openRecordModal()} disabled={invoices.length === 0}>
                        + Record Payment
                    </button>
                </div>
            </div>

            {successMessage && (
                <div style={{ padding: "12px 16px", background: "rgba(16, 185, 129, 0.15)", color: "#065f46", borderRadius: "8px", fontWeight: "600" }}>
                    ✓ {successMessage}
                </div>
            )}

            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.15)", color: "var(--danger)", borderRadius: "8px" }}>
                    {error}
                </div>
            )}

            {/* Filter & Summary Card */}
            <div className="card" style={{ padding: "16px 20px" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "16px" }}>
                    <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", flex: 1, alignItems: "center" }}>
                        <input
                            type="text"
                            className="search-input"
                            placeholder="Search by invoice #, customer, receipt, or ref #..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            style={{ minWidth: "280px" }}
                        />
                        <select
                            className="form-control"
                            value={methodFilter}
                            onChange={e => setMethodFilter(e.target.value)}
                            style={{ width: "160px" }}
                        >
                            <option value="ALL">All Methods</option>
                            <option value="UPI">UPI</option>
                            <option value="NEFT">NEFT</option>
                            <option value="RTGS">RTGS</option>
                            <option value="IMPS">IMPS</option>
                            <option value="CASH">Cash</option>
                            <option value="CARD">Card</option>
                            <option value="CHEQUE">Cheque</option>
                            <option value="NETBANKING">Net Banking</option>
                        </select>
                    </div>
                    <div style={{ textAlign: "right" }}>
                        <span style={{ fontSize: "12px", color: "var(--muted)" }}>Total Filtered Collection</span>
                        <div style={{ fontSize: "20px", fontWeight: "700", color: "#10b981" }}>{fmt(totalCollected)}</div>
                    </div>
                </div>
            </div>

            {/* Payments Table */}
            <div className="table-card">
                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center" }}>
                        <div className="spinner" style={{ margin: "0 auto 12px" }} />
                        <p>Loading payments...</p>
                    </div>
                ) : filteredPayments.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        <p style={{ fontSize: "15px", margin: "0 0 12px" }}>No payment records found.</p>
                        {invoices.length > 0 && (
                            <button className="primary-button action-btn-sm" onClick={() => openRecordModal()}>
                                Record first payment
                            </button>
                        )}
                    </div>
                ) : (
                    <div className="table-container">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Receipt #</th>
                                    <th>Date</th>
                                    <th>Invoice #</th>
                                    <th>Customer</th>
                                    <th>Method</th>
                                    <th>Ref / UTR #</th>
                                    <th style={{ textAlign: "right", paddingRight: "24px" }}>Amount</th>
                                    <th style={{ paddingLeft: "16px" }}>Recorded By</th>
                                    <th style={{ textAlign: "right" }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredPayments.map(p => (
                                    <tr key={p.id}>
                                        <td>
                                            <span className="badge" style={{ fontSize: "11px", fontWeight: 600, background: "#e0f2fe", color: "#0369a1", letterSpacing: "0.2px" }}>
                                                {p.receiptNumber || ("RCP-" + p.id)}
                                            </span>
                                        </td>
                                        <td>{p.paymentDate}</td>
                                        <td>
                                            <button
                                                type="button"
                                                className="btn-link"
                                                style={{ fontWeight: "600", padding: 0 }}
                                                onClick={() => onSelectInvoice && onSelectInvoice(p.invoiceId)}
                                            >
                                                {p.invoiceNumber}
                                            </button>
                                        </td>
                                        <td><strong>{p.customerName || "—"}</strong></td>
                                        <td>
                                            <span className="badge" style={{ background: "#ede9fe", color: "#6d28d9", fontWeight: 600 }}>
                                                {p.paymentMethod}
                                            </span>
                                        </td>
                                        <td style={{ letterSpacing: "0.2px", color: "var(--text-secondary)" }}>{p.referenceNumber || "—"}</td>
                                        <td className="table-num" style={{ textAlign: "right", paddingRight: "24px", color: "var(--success)", fontWeight: 700, fontSize: "14px" }}>
                                            {fmt(p.amount)}
                                        </td>
                                        <td style={{ paddingLeft: "16px", fontSize: "12px", color: "var(--text-muted)" }}>
                                            <span style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
                                                {p.createdBy || "System"}
                                            </span>
                                        </td>
                                        <td style={{ textAlign: "right" }}>
                                            <div style={{ display: "inline-flex", gap: "6px", justifyContent: "flex-end" }}>
                                                <button
                                                    type="button"
                                                    className="action-btn-sm"
                                                    onClick={() => handleDownloadReceipt(p)}
                                                    title="Download official Payment Receipt PDF"
                                                >
                                                    Receipt
                                                </button>
                                                <button
                                                    type="button"
                                                    className="action-btn-sm"
                                                    style={{ color: "var(--danger)" }}
                                                    onClick={() => handleDeletePayment(p.id)}
                                                    title="Reverse / delete this payment"
                                                >
                                                    Reverse
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Record Payment Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "500px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px" }}>Record Payment Received</h3>
                            <button className="btn-close" onClick={() => setShowModal(false)}>✕</button>
                        </div>

                        {modalError && (
                            <div style={{ padding: "10px", background: "rgba(239,68,68,0.15)", color: "var(--danger)", borderRadius: "6px", marginBottom: "12px", fontSize: "13px" }}>
                                {modalError}
                            </div>
                        )}

                        <form onSubmit={handleRecordSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Select Invoice *
                                </label>
                                <select
                                    className="input"
                                    value={selectedInvoice ? selectedInvoice.id : ""}
                                    onChange={e => {
                                        const inv = invoices.find(i => String(i.id) === e.target.value);
                                        setSelectedInvoice(inv);
                                        if (inv) setAmount(inv.balanceAmount || inv.grandTotal || "");
                                    }}
                                    required
                                >
                                    {invoices.map(inv => (
                                        <option key={inv.id} value={inv.id}>
                                            {inv.invoiceNumber} — {inv.customer ? inv.customer.name : "Customer"} (Due: {fmt(inv.balanceAmount || inv.grandTotal)})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {selectedInvoice && (
                                <div style={{ background: "var(--bg-subtle, #f8fafc)", padding: "10px 14px", borderRadius: "6px", fontSize: "13px" }}>
                                    <div style={{ display: "flex", justifyContent: "space-between" }}>
                                        <span style={{ color: "var(--muted)" }}>Grand Total:</span>
                                        <strong>{fmt(selectedInvoice.grandTotal)}</strong>
                                    </div>
                                    <div style={{ display: "flex", justifyContent: "space-between", marginTop: "4px" }}>
                                        <span style={{ color: "var(--muted)" }}>Outstanding Balance:</span>
                                        <strong style={{ color: "var(--danger)" }}>{fmt(selectedInvoice.balanceAmount || selectedInvoice.grandTotal)}</strong>
                                    </div>
                                </div>
                            )}

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Payment Amount (₹) *
                                    </label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        className="input"
                                        value={amount}
                                        onChange={e => setAmount(e.target.value)}
                                        placeholder="0.00"
                                        required
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Payment Date *
                                    </label>
                                    <input
                                        type="date"
                                        className="input"
                                        value={paymentDate}
                                        onChange={e => setPaymentDate(e.target.value)}
                                        required
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Payment Method *
                                    </label>
                                    <select
                                        className="input"
                                        value={paymentMethod}
                                        onChange={e => setPaymentMethod(e.target.value)}
                                        required
                                    >
                                        <option value="UPI">UPI / QR Code</option>
                                        <option value="NEFT">NEFT (Bank Transfer)</option>
                                        <option value="RTGS">RTGS (High Value)</option>
                                        <option value="IMPS">IMPS (Instant)</option>
                                        <option value="CASH">Cash</option>
                                        <option value="CARD">Credit / Debit Card</option>
                                        <option value="CHEQUE">Cheque</option>
                                        <option value="NETBANKING">Net Banking</option>
                                        <option value="OTHER">Other</option>
                                    </select>
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Reference / UTR / Cheque #
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={referenceNumber}
                                        onChange={e => setReferenceNumber(e.target.value)}
                                        placeholder="e.g. UPI Ref / UTR No"
                                    />
                                </div>
                            </div>

                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Notes (Optional)
                                </label>
                                <textarea
                                    className="input"
                                    rows="2"
                                    value={notes}
                                    onChange={e => setNotes(e.target.value)}
                                    placeholder="Add any internal transaction notes..."
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? "Saving..." : "Confirm & Settle"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
