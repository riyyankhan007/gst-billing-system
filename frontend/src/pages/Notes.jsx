import React, { useEffect, useState } from "react";
import { getCreditNotes, getDebitNotes, createCreditNote, cancelCreditNote, createDebitNote, cancelDebitNote, getInvoices } from "../services/api";

export default function Notes() {
    const [tab, setTab] = useState("credit"); // "credit" or "debit"
    const [creditNotes, setCreditNotes] = useState([]);
    const [debitNotes, setDebitNotes] = useState([]);
    const [invoices, setInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    // Modal
    const [showModal, setShowModal] = useState(false);
    const [selectedInvoiceId, setSelectedInvoiceId] = useState("");
    const [taxableAmount, setTaxableAmount] = useState("");
    const [gstRate, setGstRate] = useState(18);
    const [reason, setReason] = useState("Sales Return");
    const [noteDate, setNoteDate] = useState(new Date().toISOString().split("T")[0]);
    const [noteText, setNoteText] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [modalError, setModalError] = useState("");

    useEffect(() => {
        loadData();
    }, []);

    async function loadData() {
        try {
            setLoading(true);
            setError("");
            const [cn, dn, invs] = await Promise.all([
                getCreditNotes(),
                getDebitNotes(),
                getInvoices()
            ]);
            setCreditNotes(cn);
            setDebitNotes(dn);
            setInvoices(invs.filter(i => i.status?.toUpperCase() !== "CANCELLED"));
        } catch (err) {
            setError(err.message || "Failed to load credit/debit notes");
        } finally {
            setLoading(false);
        }
    }

    const fmt = val => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(val || 0));

    function openModal() {
        setModalError("");
        if (invoices.length > 0) {
            setSelectedInvoiceId(invoices[0].id);
        } else {
            setSelectedInvoiceId("");
        }
        setTaxableAmount("");
        setGstRate(18);
        setReason(tab === "credit" ? "Sales Return" : "Upward price revision");
        setNoteDate(new Date().toISOString().split("T")[0]);
        setNoteText("");
        setShowModal(true);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        if (!selectedInvoiceId) {
            setModalError("Please select the original invoice.");
            return;
        }
        const taxVal = Number(taxableAmount);
        if (!taxVal || taxVal <= 0) {
            setModalError("Please enter a valid taxable amount.");
            return;
        }

        const taxAmount = (taxVal * Number(gstRate)) / 100;

        try {
            setSubmitting(true);
            setModalError("");
            if (tab === "credit") {
                await createCreditNote({
                    invoiceId: Number(selectedInvoiceId),
                    taxableAmount: taxVal,
                    totalTax: taxAmount,
                    reason,
                    noteDate,
                    notes: noteText
                });
                setSuccessMessage("Credit Note issued and invoice balance adjusted!");
            } else {
                await createDebitNote({
                    invoiceId: Number(selectedInvoiceId),
                    taxableAmount: taxVal,
                    totalTax: taxAmount,
                    reason,
                    noteDate,
                    notes: noteText
                });
                setSuccessMessage("Debit Note issued and invoice balance adjusted!");
            }
            setShowModal(false);
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            setModalError(err.message || "Failed to issue note");
        } finally {
            setSubmitting(false);
        }
    }

    async function handleCancelNote(id, isCredit) {
        if (!window.confirm(`Are you sure you want to cancel this ${isCredit ? "Credit" : "Debit"} Note?`)) return;
        try {
            if (isCredit) {
                await cancelCreditNote(id);
            } else {
                await cancelDebitNote(id);
            }
            setSuccessMessage("Note cancelled and original balance restored.");
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            setError(err.message || "Failed to cancel note");
        }
    }

    const currentList = tab === "credit" ? creditNotes : debitNotes;

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">GST ADJUSTMENTS</span>
                    <h1>Credit & Debit Notes</h1>
                    <p>Handle sales returns, discounts, and invoice value revisions compliant with GST</p>
                </div>
                <button className="primary-button" onClick={openModal} disabled={invoices.length === 0}>
                    + Issue {tab === "credit" ? "Credit Note" : "Debit Note"}
                </button>
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

            {/* Tab navigation */}
            <div style={{ display: "flex", gap: "10px", borderBottom: "1px solid var(--border)", paddingBottom: "8px" }}>
                <button
                    className={`btn ${tab === "credit" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("credit")}
                >
                    Credit Notes (Sales Returns) ({creditNotes.length})
                </button>
                <button
                    className={`btn ${tab === "debit" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("debit")}
                >
                    Debit Notes (Value Revisions) ({debitNotes.length})
                </button>
            </div>

            {/* Notes Table */}
            <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center" }}>
                        <div className="spinner" style={{ margin: "0 auto 12px" }} />
                        <p>Loading notes...</p>
                    </div>
                ) : currentList.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--muted)" }}>
                        <p>No {tab === "credit" ? "credit notes" : "debit notes"} recorded.</p>
                        {invoices.length > 0 && (
                            <button className="btn btn-secondary btn-sm" onClick={openModal}>
                                Issue First {tab === "credit" ? "Credit Note" : "Debit Note"}
                            </button>
                        )}
                    </div>
                ) : (
                    <table className="table" style={{ width: "100%", margin: 0 }}>
                        <thead>
                            <tr>
                                <th>Note #</th>
                                <th>Date</th>
                                <th>Original Invoice</th>
                                <th>Customer</th>
                                <th>Reason</th>
                                <th style={{ textAlign: "right" }}>Taxable</th>
                                <th style={{ textAlign: "right" }}>GST Tax</th>
                                <th style={{ textAlign: "right" }}>Total Note</th>
                                <th>Status</th>
                                <th style={{ textAlign: "center" }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {currentList.map(n => {
                                const noteNum = tab === "credit" ? n.creditNoteNumber : n.debitNoteNumber;
                                return (
                                    <tr key={n.id}>
                                        <td style={{ fontWeight: "600", fontFamily: "monospace" }}>{noteNum}</td>
                                        <td>{n.noteDate}</td>
                                        <td style={{ fontWeight: "600" }}>{n.invoice ? n.invoice.invoiceNumber : "-"}</td>
                                        <td>{n.customer ? n.customer.name : "-"}</td>
                                        <td>{n.reason}</td>
                                        <td style={{ textAlign: "right" }}>{fmt(n.taxableAmount)}</td>
                                        <td style={{ textAlign: "right", color: "#6366f1" }}>{fmt(n.totalTax)}</td>
                                        <td style={{ textAlign: "right", fontWeight: "700" }}>{fmt(n.grandTotal)}</td>
                                        <td>
                                            <span className={`badge badge-${(n.status || "ISSUED").toLowerCase()}`}>
                                                {n.status}
                                            </span>
                                        </td>
                                        <td style={{ textAlign: "center" }}>
                                            {n.status !== "CANCELLED" && (
                                                <button
                                                    className="btn btn-danger btn-sm"
                                                    style={{ padding: "3px 8px", fontSize: "11px" }}
                                                    onClick={() => handleCancelNote(n.id, tab === "credit")}
                                                >
                                                    Cancel
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Issue Note Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "500px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px" }}>
                                Issue {tab === "credit" ? "Credit Note" : "Debit Note"}
                            </h3>
                            <button className="btn-close" onClick={() => setShowModal(false)}>✕</button>
                        </div>

                        {modalError && (
                            <div style={{ padding: "10px", background: "rgba(239,68,68,0.15)", color: "var(--danger)", borderRadius: "6px", marginBottom: "12px", fontSize: "13px" }}>
                                {modalError}
                            </div>
                        )}

                        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Original Invoice *
                                </label>
                                <select
                                    className="input"
                                    value={selectedInvoiceId}
                                    onChange={e => setSelectedInvoiceId(e.target.value)}
                                    required
                                >
                                    {invoices.map(inv => (
                                        <option key={inv.id} value={inv.id}>
                                            {inv.invoiceNumber} — {inv.customer ? inv.customer.name : "Customer"} (Total: {fmt(inv.grandTotal)}, Bal: {fmt(inv.balanceAmount)})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Taxable Amount (₹) *
                                    </label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        className="input"
                                        value={taxableAmount}
                                        onChange={e => setTaxableAmount(e.target.value)}
                                        placeholder="0.00"
                                        required
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        GST Rate *
                                    </label>
                                    <select
                                        className="input"
                                        value={gstRate}
                                        onChange={e => setGstRate(Number(e.target.value))}
                                    >
                                        <option value="0">0%</option>
                                        <option value="5">5%</option>
                                        <option value="12">12%</option>
                                        <option value="18">18%</option>
                                        <option value="28">28%</option>
                                    </select>
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Reason *
                                    </label>
                                    {tab === "credit" ? (
                                        <select
                                            className="input"
                                            value={reason}
                                            onChange={e => setReason(e.target.value)}
                                        >
                                            <option value="Sales Return">Sales Return</option>
                                            <option value="Post-sale discount">Post-sale discount</option>
                                            <option value="Deficiency in service">Deficiency in service</option>
                                            <option value="Correction in invoice">Correction in invoice</option>
                                            <option value="Other">Other</option>
                                        </select>
                                    ) : (
                                        <select
                                            className="input"
                                            value={reason}
                                            onChange={e => setReason(e.target.value)}
                                        >
                                            <option value="Upward price revision">Upward price revision</option>
                                            <option value="Tax rate revision">Tax rate revision</option>
                                            <option value="Change in quantity">Change in quantity</option>
                                            <option value="Other">Other</option>
                                        </select>
                                    )}
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Note Date *
                                    </label>
                                    <input
                                        type="date"
                                        className="input"
                                        value={noteDate}
                                        onChange={e => setNoteDate(e.target.value)}
                                        required
                                    />
                                </div>
                            </div>

                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Additional Remarks / Notes
                                </label>
                                <textarea
                                    className="input"
                                    rows="2"
                                    value={noteText}
                                    onChange={e => setNoteText(e.target.value)}
                                    placeholder="Brief note explanation..."
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? "Issuing..." : `Issue ${tab === "credit" ? "Credit" : "Debit"} Note`}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
