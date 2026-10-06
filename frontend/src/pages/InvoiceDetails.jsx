import { useEffect, useState } from "react";
import Icon from "../components/Icon";
import {
    getInvoiceById,
    issueInvoice,
    markInvoiceAsSent,
    cancelInvoice,
    downloadInvoicePdf,
    getInvoiceReminder,
    sendInvoiceReminderEmail,
    recordPayment,
    generateEInvoice,
    cancelEInvoice,
    generateEWayBill,
    cancelEWayBill,
    getEWayBills,
    getPaymentsForInvoice,
    downloadPaymentReceiptPdf,
    getAssetUrl
} from "../services/api";

export default function InvoiceDetails({ invoiceId, onBack }) {
    const [invoice, setInvoice] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [actionLoading, setActionLoading] = useState(false);

    // E-Invoice state
    const [eInvoiceCancelModalOpen, setEInvoiceCancelModalOpen] = useState(false);
    const [cancelReason, setCancelReason] = useState("1");
    const [cancelRemarks, setCancelRemarks] = useState("Order cancelled by customer");

    // E-Way Bill state
    const [ewayModalOpen, setEwayModalOpen] = useState(false);
    const [ewbDistance, setEwbDistance] = useState("100");
    const [ewbTransporterId, setEwbTransporterId] = useState("");
    const [ewbVehicleNumber, setEwbVehicleNumber] = useState("");
    const [ewayBills, setEwayBills] = useState([]);

    // Payments for this invoice
    const [payments, setPayments] = useState([]);

    // Reminder modal state
    const [reminderModalOpen, setReminderModalOpen] = useState(false);
    const [reminderData, setReminderData] = useState(null);
    const [reminderEmailSending, setReminderEmailSending] = useState(false);
    const [reminderEmailStatus, setReminderEmailStatus] = useState({ text: "", type: "" });
    const [customNote, setCustomNote] = useState("");
    const [copied, setCopied] = useState(false);

    // Quick Payment modal state
    const [payModalOpen, setPayModalOpen] = useState(false);
    const [payAmount, setPayAmount] = useState("");
    const [payMethod, setPayMethod] = useState("UPI");
    const [payRef, setPayRef] = useState("");

    useEffect(() => {
        loadInvoice();
    }, [invoiceId]);

    async function loadInvoice() {
        try {
            setLoading(true);
            const [data, pays, ewbs] = await Promise.all([
                getInvoiceById(invoiceId),
                getPaymentsForInvoice(invoiceId).catch(() => []),
                getEWayBills(invoiceId).catch(() => [])
            ]);
            setInvoice(data);
            setPayments(pays || []);
            setEwayBills(ewbs || []);
            setError("");
        } catch (err) {
            setError(err.message || "Failed to load invoice");
        } finally {
            setLoading(false);
        }
    }

    function formatCurrency(amount) {
        return `₹${Number(amount || 0).toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;
    }

    async function handleDownloadPdf() {
        try {
            const blob = await downloadInvoicePdf(invoice.id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `Invoice-${invoice.invoiceNumber || invoice.id}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (err) {
            alert("Error downloading PDF: " + err.message);
        }
    }

    async function handleIssue() {
        if (!window.confirm("Issue this invoice? This will lock the invoice number and reduce product inventory.")) return;
        try {
            setActionLoading(true);
            const updated = await issueInvoice(invoice.id);
            setInvoice(updated);
        } catch (err) {
            alert(err.message || "Could not issue invoice");
        } finally {
            setActionLoading(false);
        }
    }

    async function handleMarkSent() {
        try {
            setActionLoading(true);
            const updated = await markInvoiceAsSent(invoice.id);
            setInvoice(updated);
        } catch (err) {
            alert(err.message || "Could not mark as sent");
        } finally {
            setActionLoading(false);
        }
    }

    async function handleRecordPayment(e) {
        e.preventDefault();
        const amt = parseFloat(payAmount);
        if (!amt || amt <= 0) {
            alert("Please enter a valid positive payment amount");
            return;
        }
        const bal = Number(invoice.balanceAmount != null ? invoice.balanceAmount : invoice.grandTotal);
        if (amt > bal) {
            alert(`Payment amount (${formatCurrency(amt)}) cannot exceed balance due (${formatCurrency(bal)})`);
            return;
        }

        const idempotencyKey = (typeof crypto !== "undefined" && crypto.randomUUID)
            ? crypto.randomUUID()
            : ("pay_" + Date.now() + "_" + Math.random().toString(36).substring(2, 9));

        try {
            setActionLoading(true);
            await recordPayment({
                invoiceId: invoice.id,
                amount: amt,
                paymentMethod: payMethod,
                referenceNumber: payRef,
                paymentDate: new Date().toISOString().split("T")[0]
            }, idempotencyKey);
            setPayModalOpen(false);
            setPayAmount("");
            setPayRef("");
            await loadInvoice();
        } catch (err) {
            alert(err.message || "Failed to record payment");
        } finally {
            setActionLoading(false);
        }
    }

    async function handleGenerateEInvoice() {
        try {
            setActionLoading(true);
            await generateEInvoice(invoice.id);
            await loadInvoice();
            alert("Statutory E-Invoice (IRN) generated successfully!");
        } catch (err) {
            alert(err.message || "Failed to generate E-Invoice");
        } finally {
            setActionLoading(false);
        }
    }

    async function handleCancelEInvoice(e) {
        e.preventDefault();
        try {
            setActionLoading(true);
            await cancelEInvoice(invoice.id, cancelReason, cancelRemarks);
            setEInvoiceCancelModalOpen(false);
            await loadInvoice();
            alert("Statutory E-Invoice cancelled successfully.");
        } catch (err) {
            alert(err.message || "Failed to cancel E-Invoice");
        } finally {
            setActionLoading(false);
        }
    }

    async function handleGenerateEWayBill(e) {
        e.preventDefault();
        try {
            setActionLoading(true);
            await generateEWayBill(invoice.id, {
                distanceKm: Number(ewbDistance || 100),
                transporterId: ewbTransporterId || null,
                vehicleNumber: ewbVehicleNumber || null,
                transportMode: "ROAD",
                supplyType: "OUTWARD"
            });
            setEwayModalOpen(false);
            await loadInvoice();
            alert("E-Way Bill generated successfully!");
        } catch (err) {
            alert(err.message || "Failed to generate E-Way Bill");
        } finally {
            setActionLoading(false);
        }
    }

    async function handleCancelEWayBill(ewbId) {
        const reason = prompt("Enter E-Way Bill cancellation reason (e.g. Order Cancelled, Data Error):", "Order Cancelled");
        if (!reason) return;
        try {
            setActionLoading(true);
            await cancelEWayBill(invoice.id, ewbId, "1", reason);
            await loadInvoice();
            alert("E-Way Bill cancelled successfully.");
        } catch (err) {
            alert(err.message || "Failed to cancel E-Way Bill");
        } finally {
            setActionLoading(false);
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

    async function handleCancel() {
        if (!window.confirm("Are you sure you want to CANCEL this invoice? This will reverse any stock adjustments.")) return;
        try {
            setActionLoading(true);
            const updated = await cancelInvoice(invoice.id);
            setInvoice(updated);
        } catch (err) {
            alert(err.message || "Could not cancel invoice");
        } finally {
            setActionLoading(false);
        }
    }

    // Open reminder modal and fetch preformatted details
    async function handleOpenReminder() {
        setReminderModalOpen(true);
        setReminderEmailStatus({ text: "", type: "" });
        setCopied(false);

        try {
            const data = await getInvoiceReminder(invoice.id);
            setReminderData(data);
        } catch {
            // Fallback client-side reminder details if endpoint fails
            const customerName = invoice.customer?.name || "Customer";
            const phone = invoice.customer?.phone || "";
            const email = invoice.customer?.email || "";
            const cleanPhone = phone.replace(/[^0-9]/g, "");
            const formattedPhone = cleanPhone.length === 10 ? "91" + cleanPhone : cleanPhone;
            const amount = formatCurrency(invoice.balanceAmount != null ? invoice.balanceAmount : invoice.grandTotal);
            const text = `*Payment Reminder from ${invoice.business?.name || "Our Business"}*\n\nHello ${customerName},\nThis is a friendly reminder for Invoice *#${invoice.invoiceNumber}*:\nDate: ${invoice.invoiceDate}\nBalance Due: *${amount}*\nStatus: ${invoice.status}\n\nPlease clear the payment at your earliest convenience.\nThank you!`;
            const whatsappUrl = formattedPhone
                ? `https://wa.me/${formattedPhone}?text=${encodeURIComponent(text)}`
                : `https://wa.me/?text=${encodeURIComponent(text)}`;

            setReminderData({
                customerName,
                customerPhone: phone,
                customerEmail: email,
                invoiceNumber: invoice.invoiceNumber,
                amount,
                messageText: text,
                whatsappUrl,
                status: invoice.status
            });
        }
    }

    async function handleSendEmailReminder() {
        if (!invoice.customer?.email) {
            setReminderEmailStatus({
                text: "Customer does not have an email address configured. Please use WhatsApp.",
                type: "error"
            });
            return;
        }

        try {
            setReminderEmailSending(true);
            setReminderEmailStatus({ text: "", type: "" });

            await sendInvoiceReminderEmail(invoice.id, customNote);
            setReminderEmailStatus({
                text: `Payment reminder email sent successfully to ${invoice.customer.email}!`,
                type: "success"
            });
        } catch (err) {
            setReminderEmailStatus({
                text: err.message || "Failed to send email reminder",
                type: "error"
            });
        } finally {
            setReminderEmailSending(false);
        }
    }

    function handleCopyMessage() {
        if (!reminderData?.messageText) return;
        navigator.clipboard.writeText(reminderData.messageText);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    }

    if (loading) {
        return (
            <div className="empty-state">
                <p>Loading invoice details...</p>
            </div>
        );
    }

    if (error || !invoice) {
        return (
            <div className="empty-state">
                <p style={{ color: "var(--danger)" }}>{error || "Invoice not found"}</p>
                <button type="button" className="secondary-button" onClick={onBack} style={{ marginTop: "16px", display: "inline-flex", alignItems: "center", gap: "6px" }}>
                    <Icon type="arrowLeft" size={14} /> Back to Invoices
                </button>
            </div>
        );
    }

    const business = invoice.business || {};
    const customer = invoice.customer || {};
    const balance = Number(invoice.balanceAmount != null ? invoice.balanceAmount : invoice.grandTotal);
    const paid = Number(invoice.paidAmount || 0);

    return (
        <div className="invoice-view-container">
            {/* Top Navigation & Utility Row */}
            <div className="invoice-top-nav">
                <button
                    type="button"
                    className="action-btn-sm"
                    onClick={onBack}
                    title="Back to Invoices"
                >
                    <Icon type="arrowLeft" size={13} />
                    <span>Back to Invoices</span>
                </button>

                <div className="invoice-top-nav-actions">
                    <button
                        type="button"
                        className="btn-utility"
                        onClick={handleDownloadPdf}
                        title="Download official PDF invoice"
                    >
                        <Icon type="download" size={14} /> Download PDF
                    </button>

                    {invoice.status !== "CANCELLED" && invoice.status !== "PAID" && (
                        <button
                            type="button"
                            className="btn-utility-danger"
                            onClick={handleCancel}
                            disabled={actionLoading}
                            title="Cancel this invoice"
                        >
                            Cancel Invoice
                        </button>
                    )}
                </div>
            </div>

            {/* Dedicated Invoice Command & Control Banner */}
            <div className="invoice-command-card">
                {/* Left: Invoice Identity & Key Snapshot */}
                <div className="invoice-command-identity">
                    <div className="invoice-command-title-row">
                        <h1 className="invoice-command-number">
                            Invoice #{invoice.invoiceNumber || invoice.id}
                        </h1>
                        <span className={`status-badge status-${invoice.status?.toLowerCase()}`}>
                            {invoice.status}
                        </span>
                    </div>

                    <div className="invoice-command-meta">
                        <span className="meta-chip">
                            <span className="meta-label">Customer:</span>
                            <strong>{customer.name || "Customer"}</strong>
                        </span>
                        <span className="meta-dot">•</span>
                        <span className="meta-chip">
                            <span className="meta-label">Date:</span>
                            <span>{invoice.invoiceDate}</span>
                        </span>
                        <span className="meta-dot">•</span>
                        <span className="meta-chip">
                            <span className="meta-label">Total:</span>
                            <strong style={{ color: "var(--text-primary)" }}>{formatCurrency(invoice.grandTotal)}</strong>
                        </span>
                        {balance > 0 ? (
                            <>
                                <span className="meta-dot">•</span>
                                <span className="meta-chip due">
                                    <span className="meta-label">Due:</span>
                                    <strong>{formatCurrency(balance)}</strong>
                                </span>
                            </>
                        ) : paid > 0 ? (
                            <>
                                <span className="meta-dot">•</span>
                                <span className="meta-chip paid">
                                    <Icon type="checkCircle" size={12} color="var(--success)" />
                                    <strong>Settled</strong>
                                </span>
                            </>
                        ) : null}
                    </div>
                </div>

                {/* Right: Structured Actions Grouped by Purpose */}
                <div className="invoice-command-actions">
                    {/* DRAFT Actions */}
                    {invoice.status === "DRAFT" && (
                        <button
                            type="button"
                            className="primary-button"
                            onClick={handleIssue}
                            disabled={actionLoading}
                        >
                            <Icon type="check" size={14} /> Issue Invoice
                        </button>
                    )}

                    {/* ISSUED Actions */}
                    {invoice.status === "ISSUED" && (
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={handleMarkSent}
                            disabled={actionLoading}
                        >
                            <Icon type="send" size={14} /> Mark as Sent
                        </button>
                    )}

                    {/* Record Payment */}
                    {["ISSUED", "SENT", "PARTIALLY_PAID", "OVERDUE"].includes(invoice.status) && (
                        <button
                            type="button"
                            className="primary-button"
                            onClick={() => {
                                setPayAmount(balance.toFixed(2));
                                setPayModalOpen(true);
                            }}
                            disabled={actionLoading}
                        >
                            <Icon type="payment" size={14} /> Record Payment
                        </button>
                    )}

                    {/* Remind Customer */}
                    {invoice.status !== "PAID" && invoice.status !== "CANCELLED" && (
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={handleOpenReminder}
                            title="Send payment reminder via WhatsApp or Email"
                        >
                            <Icon type="whatsapp" size={14} /> Remind Customer
                        </button>
                    )}

                    {/* Statutory & Compliance Group */}
                    {invoice.status !== "DRAFT" && invoice.status !== "CANCELLED" && (
                        <div className="compliance-action-group">
                            {invoice.irn ? (
                                <button
                                    type="button"
                                    className="compliance-btn danger"
                                    onClick={() => setEInvoiceCancelModalOpen(true)}
                                    disabled={actionLoading}
                                    title="Cancel Statutory IRN"
                                >
                                    <Icon type="cancel" size={13} /> Cancel IRN
                                </button>
                            ) : (
                                <button
                                    type="button"
                                    className="compliance-btn irn"
                                    onClick={handleGenerateEInvoice}
                                    disabled={actionLoading}
                                    title="Generate Statutory E-Invoice"
                                >
                                    <Icon type="zap" size={13} /> E-Invoice
                                </button>
                            )}

                            <button
                                type="button"
                                className="compliance-btn ewb"
                                onClick={() => setEwayModalOpen(true)}
                                disabled={actionLoading}
                                title="Generate Statutory E-Way Bill"
                            >
                                <Icon type="truck" size={13} /> E-Way Bill
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* Invoice Document Paper */}
            <div className="invoice-paper">
                {/* Top Section */}
                <div className="invoice-paper-top">
                    <div className="invoice-business-info">
                        <h2>{business.name || "Business Name"}</h2>
                        {business.address && <p>{business.address}</p>}
                        {business.gstin && <p><strong>GSTIN:</strong> {business.gstin}</p>}
                        {business.state && <p><strong>State:</strong> {business.state} {business.stateCode ? `(${business.stateCode})` : ""}</p>}
                        {business.phone && <p><strong>Phone:</strong> {business.phone}</p>}
                        {business.email && <p><strong>Email:</strong> {business.email}</p>}
                        {business.website && <p><strong>Web:</strong> {business.website}</p>}
                    </div>

                    <div className="invoice-meta">
                        <div className="invoice-badge-title">TAX INVOICE</div>
                        <div className="invoice-meta-row">
                            <strong>Invoice No:</strong> {invoice.invoiceNumber}
                        </div>
                        <div className="invoice-meta-row">
                            <strong>Date:</strong> {invoice.invoiceDate}
                        </div>
                        <div className="invoice-meta-row" style={{ marginTop: "8px" }}>
                            <span className={`status-badge status-${invoice.status?.toLowerCase()}`}>
                                {invoice.status}
                            </span>
                        </div>
                    </div>
                </div>

                {/* Bill To */}
                <div className="invoice-paper-billto">
                    <h3>Bill To / Customer</h3>
                    <strong>{customer.name}</strong>
                    {customer.address && <p>{customer.address}</p>}
                    {customer.gstin && <p><strong>GSTIN:</strong> {customer.gstin}</p>}
                    {customer.state && <p><strong>State:</strong> {customer.state} {customer.stateCode ? `(Code: ${customer.stateCode})` : ""}</p>}
                    {customer.phone && <p><strong>Phone:</strong> {customer.phone}</p>}
                    {customer.email && <p><strong>Email:</strong> {customer.email}</p>}
                </div>

                {/* Items Table */}
                <div className="table-responsive">
                    <table className="invoice-items-table">
                        <thead>
                            <tr>
                                <th>Product / Service</th>
                                <th>HSN</th>
                                <th style={{ textAlign: "center" }}>Qty</th>
                                <th style={{ textAlign: "right" }}>Unit Price</th>
                                <th style={{ textAlign: "center" }}>GST %</th>
                                <th style={{ textAlign: "right" }}>Tax Amount</th>
                                <th style={{ textAlign: "right" }}>Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            {invoice.items?.map((item) => (
                                <tr key={item.id}>
                                    <td><strong>{item.productName}</strong></td>
                                    <td>{item.hsnCode || "—"}</td>
                                    <td style={{ textAlign: "center" }}>{item.quantity}</td>
                                    <td style={{ textAlign: "right" }}>
                                        {formatCurrency(item.unitPrice)}
                                        {item.taxInclusive && (
                                            <span className="badge" style={{ display: "inline-block", fontSize: "10px", marginLeft: "6px", padding: "1px 5px", background: "rgba(59,130,246,0.12)", color: "#1d4ed8" }}>
                                                Incl.
                                            </span>
                                        )}
                                    </td>
                                    <td style={{ textAlign: "center" }}>{item.gstRate}%</td>
                                    <td style={{ textAlign: "right" }}>{formatCurrency(item.taxAmount)}</td>
                                    <td style={{ textAlign: "right" }}><strong>{formatCurrency(item.totalAmount)}</strong></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {/* Totals & Notes Section */}
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "24px", marginTop: "24px" }}>
                    {/* Left: Amount in Words, Bank Details, Terms */}
                    <div>
                        {invoice.amountInWords && (
                            <div style={{ marginBottom: "16px", padding: "10px", background: "var(--bg-secondary)", borderRadius: "6px", fontSize: "13px" }}>
                                <span style={{ fontSize: "11px", fontWeight: "700", color: "var(--text-muted)", display: "block", textTransform: "uppercase" }}>Amount in Words:</span>
                                <strong style={{ color: "var(--text-primary)" }}>{invoice.amountInWords}</strong>
                            </div>
                        )}

                        {/* Bank Details */}
                        {(business.bankName || business.bankAccountNumber || business.upiId) && (
                            <div style={{ marginBottom: "16px", padding: "12px", border: "1px dashed var(--border-color)", borderRadius: "6px", fontSize: "12px" }}>
                                <strong style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "6px", color: "var(--text-primary)" }}>
                                    <Icon type="bank" size={14} /> Bank & Payment Details
                                </strong>
                                {business.bankName && <div><strong>Bank:</strong> {business.bankName}</div>}
                                {business.bankAccountNumber && <div><strong>A/C No:</strong> {business.bankAccountNumber}</div>}
                                {business.bankIfsc && <div><strong>IFSC:</strong> {business.bankIfsc}</div>}
                                {business.upiId && <div><strong>UPI ID:</strong> {business.upiId}</div>}
                            </div>
                        )}

                        {/* Terms */}
                        {(invoice.termsAndConditions || business.defaultTerms) && (
                            <div style={{ fontSize: "11px", color: "var(--text-muted)" }}>
                                <strong style={{ display: "block", marginBottom: "4px", color: "var(--text-primary)" }}>Terms & Conditions:</strong>
                                <p style={{ whiteSpace: "pre-line", margin: 0 }}>
                                    {invoice.termsAndConditions || business.defaultTerms}
                                </p>
                            </div>
                        )}
                    </div>

                    {/* Right: Detailed Totals Box */}
                    <div className="invoice-paper-totals">
                        <div className="invoice-paper-totals-box">
                            <div className="paper-total-line">
                                <span>Taxable Amount</span>
                                <strong>{formatCurrency(invoice.taxableAmount)}</strong>
                            </div>

                            {Number(invoice.discountAmount || 0) > 0 && (
                                <div className="paper-total-line" style={{ color: "var(--success)" }}>
                                    <span>Discount</span>
                                    <span>- {formatCurrency(invoice.discountAmount)}</span>
                                </div>
                            )}

                            {Number(invoice.cgst || 0) > 0 && (
                                <div className="paper-total-line">
                                    <span>CGST</span>
                                    <span>{formatCurrency(invoice.cgst)}</span>
                                </div>
                            )}

                            {Number(invoice.sgst || 0) > 0 && (
                                <div className="paper-total-line">
                                    <span>SGST</span>
                                    <span>{formatCurrency(invoice.sgst)}</span>
                                </div>
                            )}

                            {Number(invoice.igst || 0) > 0 && (
                                <div className="paper-total-line">
                                    <span>IGST</span>
                                    <span>{formatCurrency(invoice.igst)}</span>
                                </div>
                            )}

                            <div className="paper-total-line">
                                <span>Total Tax</span>
                                <span>{formatCurrency(invoice.totalTax)}</span>
                            </div>

                            <div className="paper-total-line grand">
                                <span>Grand Total</span>
                                <span>{formatCurrency(invoice.grandTotal)}</span>
                            </div>

                            {paid > 0 && (
                                <div className="paper-total-line" style={{ color: "var(--success)", borderTop: "1px dashed var(--border-color)", paddingTop: "8px" }}>
                                    <span>Total Paid</span>
                                    <strong>{formatCurrency(paid)}</strong>
                                </div>
                            )}

                            <div className="paper-total-line" style={{ color: balance > 0 ? "var(--warning)" : "var(--success)", fontWeight: "700" }}>
                                <span>Balance Due</span>
                                <strong>{formatCurrency(balance)}</strong>
                            </div>

                            {business.signature && (
                                <div style={{ marginTop: "20px", textAlign: "right" }}>
                                    <img
                                        src={getAssetUrl(business.signature)}
                                        alt="Authorised Signatory"
                                        style={{ maxHeight: "48px", objectFit: "contain", marginBottom: "4px" }}
                                    />
                                    <div style={{ fontSize: "11px", color: "var(--text-muted)" }}>Authorised Signatory</div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* STATUTORY E-INVOICE CARD */}
            {invoice.irn && (
                <div className="card" style={{ padding: "16px 20px", marginTop: "16px", borderLeft: "4px solid #10b981", background: "#f0fdf4" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px" }}>
                        <div>
                            <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px" }}>
                                <span className="badge" style={{ display: "inline-flex", alignItems: "center", gap: "6px", background: "#10b981", color: "#fff", fontWeight: "700" }}>
                                    <Icon type="checkCircle" size={14} color="#fff" /> STATUTORY E-INVOICE (IRN)
                                </span>
                                <span style={{ fontSize: "12px", color: "#065f46" }}>Status: {invoice.einvoiceStatus || "GENERATED"}</span>
                            </div>
                            <div style={{ fontSize: "12px", fontFamily: "monospace", color: "#047857", wordBreak: "break-all", marginBottom: "4px" }}>
                                <strong>IRN:</strong> {invoice.irn}
                            </div>
                            <div style={{ display: "flex", gap: "16px", fontSize: "12px", color: "#065f46" }}>
                                <span><strong>Ack No:</strong> {invoice.ackNo || "N/A"}</span>
                                <span><strong>Ack Date:</strong> {invoice.ackDate || "N/A"}</span>
                            </div>
                        </div>
                        <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
                            onClick={() => {
                                navigator.clipboard.writeText(invoice.irn);
                                alert("IRN copied to clipboard!");
                            }}
                        >
                            <Icon type="copy" size={13} /> Copy IRN
                        </button>
                    </div>
                </div>
            )}

            {/* E-WAY BILLS LIST */}
            {ewayBills && ewayBills.length > 0 && (
                <div className="card" style={{ padding: "16px 20px", marginTop: "16px", borderLeft: "4px solid #f59e0b" }}>
                    <h3 style={{ margin: "0 0 12px 0", fontSize: "16px", display: "flex", alignItems: "center", gap: "8px" }}>
                        <Icon type="truck" size={18} color="#b45309" /> E-Way Bills
                    </h3>
                    <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                        {ewayBills.map(ewb => (
                            <div key={ewb.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px", background: "#fffbeb", padding: "10px 14px", borderRadius: "6px" }}>
                                <div>
                                    <div style={{ fontWeight: "700", fontFamily: "monospace", fontSize: "13px", color: "#92400e" }}>
                                        EWB #{ewb.ewbNumber}
                                    </div>
                                    <div style={{ fontSize: "12px", color: "#b45309", display: "flex", gap: "16px", marginTop: "2px" }}>
                                        <span>Date: {ewb.ewbDate}</span>
                                        <span>Valid Upto: {ewb.validUpto}</span>
                                        <span>Distance: {ewb.distanceKm} km</span>
                                        {ewb.vehicleNumber && <span>Vehicle: {ewb.vehicleNumber}</span>}
                                    </div>
                                </div>
                                <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                                    <span className="badge" style={{ background: ewb.status === "ACTIVE" ? "#10b981" : "#ef4444", color: "#fff" }}>
                                        {ewb.status}
                                    </span>
                                    {ewb.status === "ACTIVE" && (
                                        <button
                                            type="button"
                                            className="btn btn-danger btn-sm"
                                            onClick={() => handleCancelEWayBill(ewb.id)}
                                            style={{ padding: "4px 8px", fontSize: "11px" }}
                                        >
                                            Cancel
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* PAYMENT RECEIPTS & SETTLEMENT HISTORY */}
            {payments && payments.length > 0 && (
                <div className="card" style={{ padding: "16px 20px", marginTop: "16px", borderLeft: "4px solid #6366f1" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                        <h3 style={{ margin: 0, fontSize: "16px", display: "flex", alignItems: "center", gap: "8px" }}>
                            <Icon type="payment" size={18} color="#4f46e5" /> Payment Receipts & Settlement History
                        </h3>
                        <span style={{ fontSize: "12px", color: "var(--muted)" }}>Total Paid: {formatCurrency(paid)}</span>
                    </div>
                    <div style={{ overflowX: "auto" }}>
                        <table className="table" style={{ width: "100%", margin: 0, fontSize: "13px" }}>
                            <thead>
                                <tr>
                                    <th>Receipt #</th>
                                    <th>Date</th>
                                    <th>Method</th>
                                    <th>Ref / UTR #</th>
                                    <th style={{ textAlign: "right" }}>Amount</th>
                                    <th style={{ textAlign: "center" }}>Receipt PDF</th>
                                </tr>
                            </thead>
                            <tbody>
                                {payments.map(p => (
                                    <tr key={p.id}>
                                        <td>
                                            <span className="badge" style={{ fontFamily: "monospace", fontSize: "11px", background: "rgba(99, 102, 241, 0.12)", color: "#4f46e5" }}>
                                                {p.receiptNumber || ("RCP-" + p.id)}
                                            </span>
                                        </td>
                                        <td>{p.paymentDate}</td>
                                        <td>{p.paymentMethod}</td>
                                        <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{p.referenceNumber || "-"}</td>
                                        <td style={{ textAlign: "right", fontWeight: "700", color: "#10b981" }}>
                                            {formatCurrency(p.amount)}
                                        </td>
                                        <td style={{ textAlign: "center" }}>
                                            <button
                                                type="button"
                                                className="btn btn-secondary btn-sm"
                                                onClick={() => handleDownloadReceipt(p)}
                                                style={{ padding: "4px 8px", fontSize: "11px", display: "inline-flex", alignItems: "center", gap: "5px" }}
                                            >
                                                <Icon type="download" size={12} /> Download Receipt
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* QUICK PAYMENT MODAL */}
            {payModalOpen && (
                <div className="modal-backdrop" onClick={() => setPayModalOpen(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: "440px" }}>
                        <div className="modal-header">
                            <h2 style={{ display: "flex", alignItems: "center", gap: "8px", margin: 0 }}><Icon type="payment" size={20} /> Record Payment for {invoice.invoiceNumber}</h2>
                            <button type="button" className="modal-close-btn" onClick={() => setPayModalOpen(false)}><Icon type="close" size={14} /></button>
                        </div>
                        <form onSubmit={handleRecordPayment} className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            <div className="reminder-summary-card">
                                <div className="reminder-summary-item">
                                    <span>Grand Total:</span>
                                    <strong>{formatCurrency(invoice.grandTotal)}</strong>
                                </div>
                                <div className="reminder-summary-item">
                                    <span>Already Paid:</span>
                                    <strong style={{ color: "var(--success)" }}>{formatCurrency(paid)}</strong>
                                </div>
                                <div className="reminder-summary-item" style={{ borderTop: "1px solid var(--border-color)", paddingTop: "6px" }}>
                                    <span>Balance Due:</span>
                                    <strong style={{ color: "var(--warning)", fontSize: "15px" }}>{formatCurrency(balance)}</strong>
                                </div>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Payment Amount (₹) <span style={{ color: "var(--danger)" }}>*</span></label>
                                <input
                                    type="number"
                                    step="0.01"
                                    className="form-control"
                                    required
                                    value={payAmount}
                                    max={balance}
                                    min="0.01"
                                    onChange={e => setPayAmount(e.target.value)}
                                    placeholder="Amount received"
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Payment Method <span style={{ color: "var(--danger)" }}>*</span></label>
                                <select
                                    className="form-control"
                                    value={payMethod}
                                    onChange={e => setPayMethod(e.target.value)}
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

                            <div className="form-group">
                                <label className="form-label">Reference / UTR / Cheque Number</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={payRef}
                                    onChange={e => setPayRef(e.target.value)}
                                    placeholder="e.g. UTR-982348123"
                                />
                            </div>

                            <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "8px" }}>
                                <button type="button" className="secondary-button" onClick={() => setPayModalOpen(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="primary-button" disabled={actionLoading}>
                                    {actionLoading ? "Recording..." : "Save Payment"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* REMINDER MODAL */}
            {reminderModalOpen && reminderData && (
                <div className="modal-backdrop" onClick={() => setReminderModalOpen(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                                </svg>
                                Send Payment Reminder
                            </h2>
                            <button
                                type="button"
                                className="modal-close-btn"
                                onClick={() => setReminderModalOpen(false)}
                            >
                                <Icon type="close" size={14} />
                            </button>
                        </div>

                        <div className="modal-body">
                            {/* Customer Summary Card */}
                            <div className="reminder-summary-card">
                                <div className="reminder-summary-item">
                                    <span>Customer:</span>
                                    <strong>{reminderData.customerName}</strong>
                                </div>
                                <div className="reminder-summary-item">
                                    <span>Phone:</span>
                                    <strong>{reminderData.customerPhone || "Not provided"}</strong>
                                </div>
                                <div className="reminder-summary-item">
                                    <span>Email:</span>
                                    <strong>{reminderData.customerEmail || "Not provided"}</strong>
                                </div>
                                <div className="reminder-summary-item" style={{ borderTop: "1px solid var(--border-color)", paddingTop: "6px", marginTop: "6px" }}>
                                    <span>Total Due:</span>
                                    <strong style={{ color: "var(--primary)", fontSize: "15px" }}>{reminderData.amount}</strong>
                                </div>
                            </div>

                            {/* Message Preview */}
                            <label style={{ fontSize: "12px", fontWeight: "700", color: "var(--text-muted)", textTransform: "uppercase", display: "block", marginBottom: "6px" }}>
                                Reminder Message Preview
                            </label>
                            <div className="reminder-preview-box">
                                {reminderData.messageText}
                            </div>

                            {reminderEmailStatus.text && (
                                <div className={reminderEmailStatus.type === "success" ? "form-success" : "form-error"}>
                                    {reminderEmailStatus.text}
                                </div>
                            )}

                            {/* Actions List */}
                            <div className="reminder-actions-vertical">
                                {/* Option 1: WhatsApp */}
                                <a
                                    href={reminderData.whatsappUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="reminder-action-card whatsapp"
                                >
                                    <div className="reminder-action-icon whatsapp">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
                                        </svg>
                                    </div>
                                    <div className="reminder-action-text">
                                        <strong>Send via WhatsApp</strong>
                                        <span>
                                            {reminderData.customerPhone
                                                ? `Open WhatsApp chat with ${reminderData.customerPhone}`
                                                : "Open WhatsApp to select contact"}
                                        </span>
                                    </div>
                                </a>

                                {/* Option 2: Email */}
                                <button
                                    type="button"
                                    className="reminder-action-card"
                                    onClick={handleSendEmailReminder}
                                    disabled={reminderEmailSending || !reminderData.customerEmail}
                                >
                                    <div className="reminder-action-icon email">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                                            <polyline points="22,6 12,13 2,6" />
                                        </svg>
                                    </div>
                                    <div className="reminder-action-text">
                                        <strong>{reminderEmailSending ? "Sending Email..." : "Send Email Reminder"}</strong>
                                        <span>
                                            {reminderData.customerEmail
                                                ? `Deliver invoice reminder to ${reminderData.customerEmail}`
                                                : "Email not set for this customer"}
                                        </span>
                                    </div>
                                </button>

                                {/* Option 3: Copy Text */}
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={handleCopyMessage}
                                    style={{ width: "100%", display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "6px" }}
                                >
                                    {copied ? (
                                        <>
                                            <Icon type="check" size={14} /> Copied to Clipboard!
                                        </>
                                    ) : (
                                        <>
                                            <Icon type="copy" size={14} /> Copy Reminder Text
                                        </>
                                    )}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* E-INVOICE CANCELLATION MODAL */}
            {eInvoiceCancelModalOpen && (
                <div className="modal-backdrop" onClick={() => setEInvoiceCancelModalOpen(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: "440px" }}>
                        <div className="modal-header">
                            <h2 style={{ display: "flex", alignItems: "center", gap: "8px", margin: 0 }}><Icon type="cancel" size={20} color="var(--danger)" /> Cancel Statutory E-Invoice</h2>
                            <button type="button" className="modal-close-btn" onClick={() => setEInvoiceCancelModalOpen(false)}><Icon type="close" size={14} /></button>
                        </div>
                        <form onSubmit={handleCancelEInvoice} className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            <p style={{ fontSize: "13px", color: "var(--text-muted)", margin: 0 }}>
                                Statutory IRN cancellation transmits directly to the IRP portal. Note: Once cancelled, an IRN cannot be re-generated for this invoice number.
                            </p>
                            <div className="form-group">
                                <label className="form-label">Cancellation Reason Code <span style={{ color: "var(--danger)" }}>*</span></label>
                                <select
                                    className="form-control"
                                    value={cancelReason}
                                    onChange={e => setCancelReason(e.target.value)}
                                >
                                    <option value="1">1 - Duplicate Invoice</option>
                                    <option value="2">2 - Data Entry Error</option>
                                    <option value="3">3 - Order Cancelled</option>
                                    <option value="4">4 - Others</option>
                                </select>
                            </div>
                            <div className="form-group">
                                <label className="form-label">Remarks <span style={{ color: "var(--danger)" }}>*</span></label>
                                <input
                                    type="text"
                                    className="form-control"
                                    required
                                    value={cancelRemarks}
                                    onChange={e => setCancelRemarks(e.target.value)}
                                    placeholder="Brief reason for cancellation"
                                />
                            </div>
                            <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "8px" }}>
                                <button type="button" className="secondary-button" onClick={() => setEInvoiceCancelModalOpen(false)}>
                                    Dismiss
                                </button>
                                <button type="submit" className="danger-button" disabled={actionLoading}>
                                    {actionLoading ? "Cancelling..." : "Confirm IRN Cancellation"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* E-WAY BILL GENERATION MODAL */}
            {ewayModalOpen && (
                <div className="modal-backdrop" onClick={() => setEwayModalOpen(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: "460px" }}>
                        <div className="modal-header">
                            <h2 style={{ display: "flex", alignItems: "center", gap: "8px", margin: 0 }}><Icon type="truck" size={20} color="#b45309" /> Generate Statutory E-Way Bill</h2>
                            <button type="button" className="modal-close-btn" onClick={() => setEwayModalOpen(false)}><Icon type="close" size={14} /></button>
                        </div>
                        <form onSubmit={handleGenerateEWayBill} className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            <div className="form-group">
                                <label className="form-label">Approx Distance (in KM) <span style={{ color: "var(--danger)" }}>*</span></label>
                                <input
                                    type="number"
                                    className="form-control"
                                    required
                                    min="1"
                                    value={ewbDistance}
                                    onChange={e => setEwbDistance(e.target.value)}
                                    placeholder="e.g. 150"
                                />
                                <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>Validity is calculated at 200 km/day as per GST rules.</span>
                            </div>
                            <div className="form-group">
                                <label className="form-label">Vehicle Registration Number</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={ewbVehicleNumber}
                                    onChange={e => setEwbVehicleNumber(e.target.value)}
                                    placeholder="e.g. MH12AB1234 or DL01XY9876"
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Transporter ID / GSTIN</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={ewbTransporterId}
                                    onChange={e => setEwbTransporterId(e.target.value)}
                                    placeholder="15-digit Transporter GSTIN (Optional)"
                                />
                            </div>
                            <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "8px" }}>
                                <button type="button" className="secondary-button" onClick={() => setEwayModalOpen(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="primary-button" disabled={actionLoading}>
                                    {actionLoading ? "Generating..." : "Generate E-Way Bill"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
