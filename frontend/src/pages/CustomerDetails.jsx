import { useEffect, useState, useMemo } from "react";
import { getCustomerDetails, getPaymentsForCustomer, downloadInvoicePdf } from "../services/api";
import Icon from "../components/Icon";

export default function CustomerDetails({ customerId, onBack, onSelectInvoice, onNewInvoice }) {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [data, setData] = useState(null);
    const [payments, setPayments] = useState([]);
    const [activeTab, setActiveTab] = useState("invoices"); // "invoices" | "orders" | "payments" | "ledger"
    const [copiedField, setCopiedField] = useState("");
    const [invoiceSearch, setInvoiceSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");

    useEffect(() => {
        if (!customerId) return;
        loadCustomerData();
    }, [customerId]);

    const loadCustomerData = async () => {
        try {
            setLoading(true);
            setError("");
            const [custData, payData] = await Promise.all([
                getCustomerDetails(customerId),
                getPaymentsForCustomer(customerId).catch(() => [])
            ]);
            setData(custData);
            setPayments(payData || []);
        } catch (err) {
            setError(err.message || "Failed to load customer profile");
        } finally {
            setLoading(false);
        }
    };

    const copyToClipboard = (text, field) => {
        if (!text) return;
        navigator.clipboard.writeText(text);
        setCopiedField(field);
        setTimeout(() => setCopiedField(""), 2000);
    };

    const fmt = val =>
        `₹${Number(val || 0).toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;

    const handleDownloadPdf = async (e, id, invoiceNum) => {
        e.stopPropagation();
        try {
            const blob = await downloadInvoicePdf(id);
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `Invoice-${invoiceNum || id}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        } catch (err) {
            alert("Could not download PDF: " + err.message);
        }
    };

    // Calculate aggregate GST breakdown and order items
    const { totalCgst, totalSgst, totalIgst, totalTax, taxableTurnover, orderedItems, ledgerEntries } = useMemo(() => {
        if (!data || !data.invoices) {
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

        let cgstSum = 0;
        let sgstSum = 0;
        let igstSum = 0;
        let taxSum = 0;
        let taxableSum = 0;
        const itemMap = new Map();

        const activeInvoices = data.invoices.filter(
            inv => inv.status !== "CANCELLED" && inv.status !== "DRAFT"
        );

        data.invoices.forEach(inv => {
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
        const computedLedger = entries.map(ent => {
            runBal += ent.debit - ent.credit;
            return { ...ent, runningBalance: runBal };
        });

        return {
            totalCgst: cgstSum,
            totalSgst: sgstSum,
            totalIgst: igstSum,
            totalTax: taxSum,
            taxableTurnover: taxableSum,
            orderedItems: Array.from(itemMap.values()).sort((a, b) => b.totalAmount - a.totalAmount),
            ledgerEntries: computedLedger
        };
    }, [data, payments]);

    if (loading) {
        return (
            <div className="customer-profile-page">
                <div style={{ padding: "60px", textAlign: "center" }}>
                    <div className="skeleton-line" style={{ width: "240px", height: "24px", margin: "0 auto 16px" }} />
                    <div className="skeleton-line" style={{ width: "400px", height: "14px", margin: "0 auto" }} />
                    <p style={{ color: "var(--text-muted)", marginTop: "16px", fontSize: "14px" }}>
                        Loading customer ledger and transaction history...
                    </p>
                </div>
            </div>
        );
    }

    if (error || !data) {
        return (
            <div className="customer-profile-page">
                <button type="button" className="action-btn-sm" onClick={onBack} style={{ width: "fit-content" }}>
                    <Icon type="arrowLeft" />
                    <span>Back to Customers</span>
                </button>
                <div className="alert alert-error" style={{ marginTop: "16px" }}>
                    <strong>Unable to load customer profile:</strong> {error || "Customer record not found"}
                </div>
            </div>
        );
    }

    const { customer, totalInvoiced, totalPaid, outstandingBalance, overdueAmount, invoices } = data;

    // Filter invoices by search and status
    const filteredInvoices = (invoices || []).filter(inv => {
        const matchesStatus = statusFilter === "ALL" || (inv.status || "").toUpperCase() === statusFilter;
        const q = invoiceSearch.toLowerCase();
        const matchesSearch = !q ||
            (inv.invoiceNumber && inv.invoiceNumber.toLowerCase().includes(q)) ||
            (inv.notes && inv.notes.toLowerCase().includes(q));
        return matchesStatus && matchesSearch;
    });

    const exportStatementCsv = () => {
        if (!ledgerEntries.length) return;
        const rows = [
            ["Date", "Type", "Reference", "Description", "Debit (INR)", "Credit (INR)", "Running Balance (INR)"]
        ];
        ledgerEntries.forEach(ent => {
            rows.push([
                ent.date,
                ent.type,
                ent.reference,
                `"${(ent.description || "").replace(/"/g, '""')}"`,
                ent.debit.toFixed(2),
                ent.credit.toFixed(2),
                ent.runningBalance.toFixed(2)
            ]);
        });
        const csvContent = "data:text/csv;charset=utf-8," + rows.map(r => r.join(",")).join("\n");
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", `Statement-${customer.name.replace(/\s+/g, "_")}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    return (
        <div className="customer-profile-page">
            {/* Top Navigation Row */}
            <div className="customer-top-nav">
                <button
                    type="button"
                    className="action-btn-sm"
                    onClick={onBack}
                    title="Return to customer list"
                >
                    <Icon type="arrowLeft" size={14} />
                    <span>Back to Customers Directory</span>
                </button>

                <div style={{ display: "flex", gap: "10px" }}>
                    {customer.phone && (
                        <a
                            href={`https://wa.me/91${customer.phone.replace(/\D/g, "")}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="action-btn-sm"
                            style={{ color: "var(--whatsapp-text)", borderColor: "#bbf7d0", backgroundColor: "var(--whatsapp-bg)" }}
                            title="Message on WhatsApp"
                        >
                            <Icon type="whatsapp" size={14} />
                            <span>WhatsApp</span>
                        </a>
                    )}
                    <button
                        type="button"
                        className="action-btn-sm"
                        onClick={exportStatementCsv}
                        title="Download Statement CSV"
                    >
                        <Icon type="download" size={14} />
                        <span>Export Statement</span>
                    </button>
                    {onNewInvoice && (
                        <button
                            type="button"
                            className="primary-button"
                            onClick={() => onNewInvoice(customer)}
                            style={{ height: "34px", padding: "0 14px", fontSize: "13px" }}
                        >
                            <Icon type="plus" size={14} />
                            <span>New Invoice</span>
                        </button>
                    )}
                </div>
            </div>

            {/* Customer Hero Identity Card */}
            <div className="customer-hero-card">
                <div className="customer-hero-main">
                    <div className="customer-avatar-large">
                        {customer.name ? customer.name.trim().charAt(0).toUpperCase() : "C"}
                    </div>
                    <div className="customer-hero-title">
                        <h1>{customer.name}</h1>
                        <div className="customer-hero-badges">
                            <span className="badge" style={{ backgroundColor: "#e0f2fe", color: "#0369a1", fontWeight: 700 }}>
                                {customer.customerType || "B2B"}
                            </span>
                            {customer.gstin ? (
                                <button
                                    type="button"
                                    className="badge-tag"
                                    onClick={() => copyToClipboard(customer.gstin, "gstin")}
                                    title="Click to copy GSTIN"
                                    style={{ background: "#f1f5f9", cursor: "pointer", border: "1px solid #cbd5e1" }}
                                >
                                    <span>GSTIN: {customer.gstin}</span>
                                    <Icon type="copy" size={11} color="var(--text-muted)" />
                                    {copiedField === "gstin" && <span style={{ color: "var(--success)", fontSize: "10px" }}>Copied!</span>}
                                </button>
                            ) : (
                                <span className="status-badge status-draft">Unregistered (B2C)</span>
                            )}
                            {customer.pan && (
                                <span className="badge-tag" style={{ background: "#f8fafc", border: "1px solid #e2e8f0" }}>
                                    PAN: {customer.pan}
                                </span>
                            )}
                            <span className="badge-tag" style={{ background: "#f8fafc", border: "1px solid #e2e8f0" }}>
                                {customer.state} {customer.stateCode ? `(${customer.stateCode})` : ""}
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Financial Performance KPI Cards */}
            <div className="customer-financial-grid">
                {/* Total Sales */}
                <div className="customer-financial-card" style={{ "--card-accent": "var(--primary)" }}>
                    <div className="customer-financial-label">Total Sales (Turnover)</div>
                    <div className="customer-financial-value" style={{ color: "var(--primary)" }}>
                        {fmt(totalInvoiced)}
                    </div>
                    <div className="customer-financial-sub">
                        <span>{invoices?.length || 0} Invoices Generated</span>
                        <span>•</span>
                        <span>Taxable: {fmt(taxableTurnover)}</span>
                    </div>
                </div>

                {/* Total GST Paid */}
                <div className="customer-financial-card" style={{ "--card-accent": "#8b5cf6" }}>
                    <div className="customer-financial-label">Total GST Paid / Liability</div>
                    <div className="customer-financial-value" style={{ color: "#7c3aed" }}>
                        {fmt(totalTax)}
                    </div>
                    <div className="customer-financial-sub" style={{ fontSize: "11px" }}>
                        <span>CGST: {fmt(totalCgst)}</span>
                        <span>•</span>
                        <span>SGST: {fmt(totalSgst)}</span>
                        {totalIgst > 0 && <span>• IGST: {fmt(totalIgst)}</span>}
                    </div>
                </div>

                {/* Total Payments Received */}
                <div className="customer-financial-card" style={{ "--card-accent": "var(--success)" }}>
                    <div className="customer-financial-label">Total Paid (Receipts)</div>
                    <div className="customer-financial-value" style={{ color: "var(--success)" }}>
                        {fmt(totalPaid)}
                    </div>
                    <div className="customer-financial-sub">
                        <span>{payments?.length || 0} Settlement Transactions</span>
                    </div>
                </div>

                {/* Outstanding & Overdue Balance */}
                <div className="customer-financial-card" style={{ "--card-accent": Number(overdueAmount) > 0 ? "var(--danger)" : "var(--warning)" }}>
                    <div className="customer-financial-label">Outstanding Balance</div>
                    <div className="customer-financial-value" style={{ color: Number(outstandingBalance) > 0 ? "var(--danger)" : "var(--text-primary)" }}>
                        {fmt(outstandingBalance)}
                    </div>
                    <div className="customer-financial-sub">
                        {Number(overdueAmount) > 0 ? (
                            <span style={{ color: "var(--danger)", fontWeight: 700 }}>
                                Overdue: {fmt(overdueAmount)}
                            </span>
                        ) : (
                            <span style={{ color: "var(--success)", fontWeight: 600 }}>Zero Overdue Dues</span>
                        )}
                    </div>
                </div>
            </div>

            {/* Master Profile & Contact Information */}
            <div className="customer-details-card">
                <div className="customer-details-grid">
                    {/* Section 1: Contact Details */}
                    <div className="customer-detail-section">
                        <div className="customer-detail-section-title">
                            <Icon type="phone" size={14} color="var(--primary)" />
                            <span>Contact & Communication</span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Phone (WhatsApp)</span>
                            <span className="customer-detail-val">{customer.phone || "—"}</span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Billing Email</span>
                            <span className="customer-detail-val">{customer.email || "—"}</span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Place of Supply</span>
                            <span className="customer-detail-val">
                                {customer.state || "—"} {customer.stateCode ? `(Code: ${customer.stateCode})` : ""}
                            </span>
                        </div>
                    </div>

                    {/* Section 2: Addresses */}
                    <div className="customer-detail-section">
                        <div className="customer-detail-section-title">
                            <Icon type="truck" size={14} color="var(--primary)" />
                            <span>Registered Addresses</span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Billing Address</span>
                            <span className="customer-detail-val" style={{ whiteSpace: "pre-line", fontSize: "12.5px" }}>
                                {customer.billingAddress || "—"}
                            </span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Shipping Destination</span>
                            <span className="customer-detail-val" style={{ whiteSpace: "pre-line", fontSize: "12.5px" }}>
                                {customer.shippingAddress || customer.billingAddress || "—"}
                            </span>
                        </div>
                    </div>

                    {/* Section 3: Credit & Ledger Controls */}
                    <div className="customer-detail-section">
                        <div className="customer-detail-section-title">
                            <Icon type="bank" size={14} color="var(--primary)" />
                            <span>Credit & Ledger Limits</span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Credit Limit</span>
                            <span className="customer-detail-val">{fmt(customer.creditLimit)}</span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Available Credit</span>
                            <span className="customer-detail-val" style={{ color: Number(customer.creditLimit) - Number(outstandingBalance) < 0 ? "var(--danger)" : "var(--success)" }}>
                                {fmt(Math.max(0, Number(customer.creditLimit || 0) - Number(outstandingBalance || 0)))}
                            </span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Opening Balance</span>
                            <span className="customer-detail-val">{fmt(customer.openingBalance)}</span>
                        </div>
                        <div className="customer-detail-row">
                            <span className="customer-detail-key">Default Payment Terms</span>
                            <span className="customer-detail-val">{customer.paymentTerms || 15} Days Net</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Tabbed Invoices, Orders & Payments History */}
            <div className="table-card">
                <div style={{ padding: "16px 20px 0 20px", borderBottom: "1px solid var(--border-color)" }}>
                    <div className="customer-tabs-bar" style={{ borderBottom: "none", margin: 0 }}>
                        <button
                            type="button"
                            className={`customer-tab-btn ${activeTab === "invoices" ? "active" : ""}`}
                            onClick={() => setActiveTab("invoices")}
                        >
                            <Icon type="invoice" size={16} />
                            <span>Invoices & Billings</span>
                            <span className="customer-tab-badge">{invoices?.length || 0}</span>
                        </button>

                        <button
                            type="button"
                            className={`customer-tab-btn ${activeTab === "orders" ? "active" : ""}`}
                            onClick={() => setActiveTab("orders")}
                        >
                            <Icon type="product" size={16} />
                            <span>Orders Made (Items Purchased)</span>
                            <span className="customer-tab-badge">{orderedItems.length}</span>
                        </button>

                        <button
                            type="button"
                            className={`customer-tab-btn ${activeTab === "payments" ? "active" : ""}`}
                            onClick={() => setActiveTab("payments")}
                        >
                            <Icon type="payment" size={16} />
                            <span>Payments Received</span>
                            <span className="customer-tab-badge">{payments?.length || 0}</span>
                        </button>

                        <button
                            type="button"
                            className={`customer-tab-btn ${activeTab === "ledger" ? "active" : ""}`}
                            onClick={() => setActiveTab("ledger")}
                        >
                            <Icon type="notes" size={16} />
                            <span>Account Statement Ledger</span>
                            <span className="customer-tab-badge">{ledgerEntries.length}</span>
                        </button>
                    </div>
                </div>

                {/* TAB 1: INVOICES & BILLINGS */}
                {activeTab === "invoices" && (
                    <div>
                        {/* Search & Filter Toolbar */}
                        <div style={{ padding: "14px 20px", display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px", flexWrap: "wrap", borderBottom: "1px solid var(--border-color)", background: "#f8fafc" }}>
                            <div className="search-input-wrapper" style={{ width: "280px" }}>
                                <Icon type="search" size={14} className="search-icon" />
                                <input
                                    type="text"
                                    className="search-input"
                                    placeholder="Search invoice number or notes..."
                                    value={invoiceSearch}
                                    onChange={e => setInvoiceSearch(e.target.value)}
                                />
                            </div>

                            <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                                <span style={{ fontSize: "12px", color: "var(--text-muted)", fontWeight: 600 }}>Status:</span>
                                <select
                                    className="filter-select"
                                    value={statusFilter}
                                    onChange={e => setStatusFilter(e.target.value)}
                                >
                                    <option value="ALL">All Statuses</option>
                                    <option value="ISSUED">Issued</option>
                                    <option value="PARTIALLY_PAID">Partially Paid</option>
                                    <option value="PAID">Paid</option>
                                    <option value="DRAFT">Draft</option>
                                    <option value="CANCELLED">Cancelled</option>
                                </select>
                            </div>
                        </div>

                        {filteredInvoices.length === 0 ? (
                            <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                                <Icon type="invoice" size={32} color="#cbd5e1" />
                                <p style={{ marginTop: "12px", fontSize: "14px", fontWeight: 500 }}>No invoices found matching current criteria.</p>
                            </div>
                        ) : (
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Invoice #</th>
                                        <th style={{ textAlign: "right" }}>Taxable Amt</th>
                                        <th style={{ textAlign: "right" }}>Total GST</th>
                                        <th style={{ textAlign: "right" }}>Grand Total</th>
                                        <th style={{ textAlign: "right" }}>Paid</th>
                                        <th style={{ textAlign: "right" }}>Balance Due</th>
                                        <th>Status</th>
                                        <th style={{ textAlign: "right" }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredInvoices.map(inv => (
                                        <tr key={inv.id}>
                                            <td>{inv.invoiceDate}</td>
                                            <td style={{ fontWeight: 600 }}>
                                                {onSelectInvoice ? (
                                                    <button
                                                        type="button"
                                                        onClick={() => onSelectInvoice(inv.id)}
                                                        style={{ background: "none", border: "none", color: "var(--primary)", fontWeight: 700, cursor: "pointer", padding: 0 }}
                                                        title="View full invoice details"
                                                    >
                                                        {inv.invoiceNumber}
                                                    </button>
                                                ) : (
                                                    inv.invoiceNumber
                                                )}
                                            </td>
                                            <td className="table-num">{fmt(inv.taxableAmount)}</td>
                                            <td className="table-num" style={{ color: "#7c3aed" }}>
                                                {fmt(inv.totalTax)}
                                            </td>
                                            <td className="table-num" style={{ fontWeight: 700 }}>{fmt(inv.grandTotal)}</td>
                                            <td className="table-num" style={{ color: "var(--success)" }}>{fmt(inv.paidAmount)}</td>
                                            <td className="table-num" style={{ color: Number(inv.balanceAmount) > 0 ? "var(--danger)" : "var(--text-muted)", fontWeight: 600 }}>
                                                {fmt(inv.balanceAmount)}
                                            </td>
                                            <td>
                                                <span className={`status-badge status-${(inv.status || "DRAFT").toLowerCase()}`}>
                                                    {inv.status}
                                                </span>
                                            </td>
                                            <td style={{ textAlign: "right" }}>
                                                <div style={{ display: "inline-flex", gap: "6px" }}>
                                                    {onSelectInvoice && (
                                                        <button
                                                            type="button"
                                                            className="action-btn-sm"
                                                            onClick={() => onSelectInvoice(inv.id)}
                                                            title="View details"
                                                        >
                                                            View
                                                        </button>
                                                    )}
                                                    <button
                                                        type="button"
                                                        className="action-btn-sm"
                                                        onClick={(e) => handleDownloadPdf(e, inv.id, inv.invoiceNumber)}
                                                        title="Download GST PDF"
                                                    >
                                                        <Icon type="download" size={13} />
                                                        <span>PDF</span>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {/* TAB 2: ORDERS MADE (ITEMS PURCHASED) */}
                {activeTab === "orders" && (
                    <div>
                        {orderedItems.length === 0 ? (
                            <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                                <Icon type="product" size={32} color="#cbd5e1" />
                                <p style={{ marginTop: "12px", fontSize: "14px", fontWeight: 500 }}>No item purchase history available yet.</p>
                            </div>
                        ) : (
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Product / Service Item</th>
                                        <th>HSN / SAC</th>
                                        <th style={{ textAlign: "right" }}>Total Volume</th>
                                        <th style={{ textAlign: "center" }}>GST Rate</th>
                                        <th style={{ textAlign: "right" }}>Taxable Sales</th>
                                        <th style={{ textAlign: "right" }}>GST Paid</th>
                                        <th style={{ textAlign: "right" }}>Gross Total</th>
                                        <th style={{ textAlign: "center" }}>Orders Count</th>
                                        <th>Last Purchased</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {orderedItems.map((item, idx) => (
                                        <tr key={idx}>
                                            <td style={{ fontWeight: 600, color: "var(--text-primary)" }}>{item.name}</td>
                                            <td style={{ letterSpacing: "0.4px" }}>{item.hsnCode}</td>
                                            <td className="table-num">
                                                {item.totalQty} {item.unit}
                                            </td>
                                            <td style={{ textAlign: "center", fontWeight: 600 }}>{item.gstRate}%</td>
                                            <td className="table-num">{fmt(item.taxableAmount)}</td>
                                            <td className="table-num" style={{ color: "#7c3aed" }}>{fmt(item.taxAmount)}</td>
                                            <td className="table-num" style={{ fontWeight: 700, color: "var(--primary)" }}>{fmt(item.totalAmount)}</td>
                                            <td style={{ textAlign: "center" }}>
                                                <span className="badge" style={{ backgroundColor: "#eff6ff", color: "var(--primary)" }}>
                                                    {item.orderCount} Orders
                                                </span>
                                            </td>
                                            <td style={{ color: "var(--text-muted)" }}>{item.lastDate || "—"}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {/* TAB 3: PAYMENTS RECEIVED */}
                {activeTab === "payments" && (
                    <div>
                        {payments.length === 0 ? (
                            <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                                <Icon type="payment" size={32} color="#cbd5e1" />
                                <p style={{ marginTop: "12px", fontSize: "14px", fontWeight: 500 }}>No payments recorded for this customer yet.</p>
                            </div>
                        ) : (
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Payment ID</th>
                                        <th style={{ textAlign: "right" }}>Amount Received</th>
                                        <th>Payment Method</th>
                                        <th>Reference / UTR #</th>
                                        <th>Linked Invoice</th>
                                        <th>Recorded By</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {payments.map(pay => (
                                        <tr key={pay.id}>
                                            <td>{pay.paymentDate}</td>
                                            <td style={{ fontWeight: 600, color: "var(--primary)" }}>
                                                PAY-{pay.id}
                                            </td>
                                            <td className="table-num" style={{ color: "var(--success)", fontWeight: 700 }}>
                                                {fmt(pay.amount)}
                                            </td>
                                            <td>
                                                <span className="badge" style={{ backgroundColor: "#f0fdf4", color: "#166534", border: "1px solid #bbf7d0" }}>
                                                    {pay.paymentMethod || "BANK_TRANSFER"}
                                                </span>
                                            </td>
                                            <td style={{ letterSpacing: "0.2px" }}>{pay.referenceNumber || "—"}</td>
                                            <td>
                                                {pay.invoiceNumber ? (
                                                    <span style={{ fontWeight: 600 }}>{pay.invoiceNumber}</span>
                                                ) : (
                                                    <span style={{ color: "var(--text-muted)" }}>General Ledger</span>
                                                )}
                                            </td>
                                            <td style={{ color: "var(--text-muted)" }}>{pay.recordedBy || "System"}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {/* TAB 4: ACCOUNT STATEMENT LEDGER */}
                {activeTab === "ledger" && (
                    <div>
                        <div style={{ padding: "14px 20px", display: "flex", justifyContent: "space-between", alignItems: "center", background: "#f8fafc", borderBottom: "1px solid var(--border-color)" }}>
                            <span style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-secondary)" }}>
                                Complete Double-Entry Running Ledger
                            </span>
                            <button
                                type="button"
                                className="action-btn-sm"
                                onClick={exportStatementCsv}
                            >
                                <Icon type="download" size={13} />
                                <span>Download CSV Statement</span>
                            </button>
                        </div>

                        {ledgerEntries.length === 0 ? (
                            <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                                <Icon type="notes" size={32} color="#cbd5e1" />
                                <p style={{ marginTop: "12px", fontSize: "14px", fontWeight: 500 }}>No ledger transactions recorded.</p>
                            </div>
                        ) : (
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Type</th>
                                        <th>Reference</th>
                                        <th>Transaction Particulars</th>
                                        <th style={{ textAlign: "right" }}>Debit (+)</th>
                                        <th style={{ textAlign: "right" }}>Credit (-)</th>
                                        <th style={{ textAlign: "right" }}>Running Balance</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {ledgerEntries.map((ent, idx) => (
                                        <tr key={idx}>
                                            <td>{ent.date}</td>
                                            <td>
                                                <span className="badge" style={{
                                                    backgroundColor: ent.type === "INVOICE" ? "#eff6ff" : ent.type === "PAYMENT" ? "#ecfdf5" : "#f1f5f9",
                                                    color: ent.type === "INVOICE" ? "var(--primary)" : ent.type === "PAYMENT" ? "#065f46" : "var(--text-secondary)"
                                                }}>
                                                    {ent.type}
                                                </span>
                                            </td>
                                            <td style={{ fontWeight: 600 }}>{ent.reference}</td>
                                            <td style={{ color: "var(--text-secondary)" }}>{ent.description}</td>
                                            <td className="table-num" style={{ color: ent.debit > 0 ? "var(--text-primary)" : "var(--text-muted)" }}>
                                                {ent.debit > 0 ? fmt(ent.debit) : "—"}
                                            </td>
                                            <td className="table-num" style={{ color: ent.credit > 0 ? "var(--success)" : "var(--text-muted)", fontWeight: ent.credit > 0 ? 600 : 400 }}>
                                                {ent.credit > 0 ? fmt(ent.credit) : "—"}
                                            </td>
                                            <td className="table-num" style={{
                                                fontWeight: 700,
                                                color: ent.runningBalance > 0 ? "var(--danger)" : ent.runningBalance < 0 ? "var(--success)" : "var(--text-primary)"
                                            }}>
                                                {fmt(ent.runningBalance)}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}
