import { useEffect, useState } from "react";
import {
    getBusinesses,
    getCustomers,
    getProducts,
    createInvoice
} from "../services/api";

export default function CreateInvoice({ onBack, onCreated, onNavigate }) {
    const [businesses, setBusinesses] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [products, setProducts] = useState([]);

    const [businessId, setBusinessId] = useState("");
    const [customerId, setCustomerId] = useState("");
    const [invoiceDate, setInvoiceDate] = useState(new Date().toISOString().split("T")[0]);
    const [dueDate, setDueDate] = useState("");
    const [discountAmount, setDiscountAmount] = useState("0");
    const [notes, setNotes] = useState("");
    const [termsAndConditions, setTermsAndConditions] = useState("");

    const [items, setItems] = useState([
        {
            productId: "",
            quantity: 1,
            discount: 0
        }
    ]);

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState({ text: "", type: "" });

    useEffect(() => {
        async function loadFormData() {
            try {
                setLoading(true);
                const [bizList, custList, prodList] = await Promise.all([
                    getBusinesses(),
                    getCustomers(),
                    getProducts()
                ]);

                setBusinesses(bizList || []);
                setCustomers(custList || []);
                setProducts(prodList || []);

                if (bizList && bizList.length > 0) {
                    setBusinessId(String(bizList[0].id));
                }
            } catch (err) {
                setMessage({ text: err.message || "Failed to load form dependencies", type: "error" });
            } finally {
                setLoading(false);
            }
        }

        loadFormData();
    }, []);

    function updateItem(index, field, value) {
        const next = [...items];
        next[index] = {
            ...next[index],
            [field]: value
        };
        setItems(next);
    }

    function addItem() {
        setItems([
            ...items,
            {
                productId: "",
                quantity: 1
            }
        ]);
    }

    function removeItem(index) {
        if (items.length <= 1) return;
        setItems(items.filter((_, i) => i !== index));
    }

    // Real-time calculation of totals
    function calculateSummary() {
        let taxableAmount = 0;
        let totalTax = 0;

        items.forEach(item => {
            const prod = products.find(p => p.id === Number(item.productId));
            if (!prod) return;

            const qty = Number(item.quantity) || 0;
            const lineTaxable = prod.price * qty;
            const lineTax = lineTaxable * (prod.gstRate / 100);

            taxableAmount += lineTaxable;
            totalTax += lineTax;
        });

        const selectedBiz = businesses.find(b => b.id === Number(businessId));
        const selectedCust = customers.find(c => c.id === Number(customerId));

        let cgst = 0;
        let sgst = 0;
        let igst = 0;

        if (selectedBiz && selectedCust && selectedBiz.state && selectedCust.state) {
            const isIntraState = selectedBiz.state.trim().toLowerCase() === selectedCust.state.trim().toLowerCase();
            if (isIntraState) {
                cgst = totalTax / 2;
                sgst = totalTax / 2;
            } else {
                igst = totalTax;
            }
        } else {
            // Default to intra-state split if state unknown
            cgst = totalTax / 2;
            sgst = totalTax / 2;
        }

        const grandTotal = taxableAmount + totalTax;

        return {
            taxableAmount,
            totalTax,
            cgst,
            sgst,
            igst,
            grandTotal
        };
    }

    const summary = calculateSummary();

    async function handleCreateInvoice(e, desiredStatus = "ISSUED") {
        if (e && e.preventDefault) e.preventDefault();
        setMessage({ text: "", type: "" });

        if (!customerId) {
            setMessage({ text: "Please select a Customer (*)", type: "error" });
            return;
        }

        if (items.some(item => !item.productId || Number(item.quantity) <= 0)) {
            setMessage({ text: "Please choose valid products and quantities for all lines (*)", type: "error" });
            return;
        }

        setSaving(true);

        const payload = {
            customerId: Number(customerId),
            invoiceDate: invoiceDate,
            dueDate: dueDate || null,
            discountAmount: Number(discountAmount || 0),
            notes: notes,
            termsAndConditions: termsAndConditions,
            status: desiredStatus,
            items: items.map(i => ({
                productId: Number(i.productId),
                quantity: Number(i.quantity),
                discount: Number(i.discount || 0)
            }))
        };

        try {
            const created = await createInvoice(payload);
            setMessage({ text: `Invoice #${created.invoiceNumber} created (${created.status})!`, type: "success" });
            setTimeout(() => {
                if (onCreated) {
                    onCreated(created.id);
                } else if (onBack) {
                    onBack();
                }
            }, 1000);
        } catch (err) {
            setMessage({ text: err.message || "Failed to create invoice", type: "error" });
        } finally {
            setSaving(false);
        }
    }

    function formatCurrency(val) {
        return `₹${Number(val || 0).toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })}`;
    }

    return (
        <div className="create-invoice-page">
            <button
                type="button"
                className="text-button"
                style={{ marginBottom: "16px" }}
                onClick={onBack}
            >
                ← Back to Overview
            </button>

            <div className="page-heading">
                <div>
                    <span className="eyebrow">NEW INVOICE</span>
                    <h1>Generate Tax Invoice</h1>
                    <p>Select your customer, add products, review real-time GST calculations and generate.</p>
                </div>
            </div>

            {message.text && (
                <div className={message.type === "success" ? "form-success" : "form-error"}>
                    {message.text}
                </div>
            )}

            {loading ? (
                <div className="empty-state">
                    <p>Loading business and customer records...</p>
                </div>
            ) : customers.length === 0 ? (
                <div className="empty-state" style={{ background: "#ffffff", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "36px 24px", textAlign: "center" }}>
                    <h3 style={{ margin: "0 0 8px", color: "var(--text-primary)", fontSize: "18px" }}>No customers found!</h3>
                    <p style={{ margin: "0 0 16px", color: "var(--text-muted)" }}>You need to create at least one customer before generating an invoice.</p>
                    {onNavigate && (
                        <button type="button" className="primary-button" onClick={() => onNavigate("customers")}>
                            + Add New Customer
                        </button>
                    )}
                </div>
            ) : products.length === 0 ? (
                <div className="empty-state" style={{ background: "#ffffff", border: "1px solid var(--border-color)", borderRadius: "var(--radius-md)", padding: "36px 24px", textAlign: "center" }}>
                    <h3 style={{ margin: "0 0 8px", color: "var(--text-primary)", fontSize: "18px" }}>No products found!</h3>
                    <p style={{ margin: "0 0 16px", color: "var(--text-muted)" }}>You need to add at least one product or service before creating an invoice.</p>
                    {onNavigate && (
                        <button type="button" className="primary-button" onClick={() => onNavigate("products")}>
                            + Add First Product
                        </button>
                    )}
                </div>
            ) : (
                <div className="management-layout">
                    {/* LEFT PANEL: Form Inputs */}
                    <section className="form-panel">
                        <div className="form-panel-header">
                            <h2>Invoice Information</h2>
                        </div>

                        <form onSubmit={handleCreateInvoice}>
                            <div className="form-field">
                                <label htmlFor="inv-business">
                                    Supplier / Business <span className="req-star">*</span>
                                </label>
                                <select
                                    id="inv-business"
                                    value={businessId}
                                    onChange={e => setBusinessId(e.target.value)}
                                    required
                                >
                                    {businesses.map(b => (
                                        <option key={b.id} value={b.id}>
                                            {b.name} ({b.state || "State N/A"})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-field">
                                <label htmlFor="inv-customer">
                                    Customer / Bill To <span className="req-star">*</span>
                                </label>
                                <select
                                    id="inv-customer"
                                    value={customerId}
                                    onChange={e => setCustomerId(e.target.value)}
                                    required
                                >
                                    <option value="">-- Choose Customer --</option>
                                    {customers.map(c => (
                                        <option key={c.id} value={c.id}>
                                            {c.name} ({c.state || "No State"}{c.phone ? ` · 📞 ${c.phone}` : ""})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div className="form-field">
                                    <label htmlFor="inv-date">
                                        Invoice Date <span className="req-star">*</span>
                                    </label>
                                    <input
                                        id="inv-date"
                                        type="date"
                                        value={invoiceDate}
                                        onChange={e => setInvoiceDate(e.target.value)}
                                        required
                                    />
                                </div>
                                <div className="form-field">
                                    <label htmlFor="inv-due-date">
                                        Due Date (Payment Due)
                                    </label>
                                    <input
                                        id="inv-due-date"
                                        type="date"
                                        value={dueDate}
                                        onChange={e => setDueDate(e.target.value)}
                                    />
                                </div>
                            </div>

                            <div className="form-field">
                                <label htmlFor="inv-discount">
                                    Invoice Overall Discount (₹)
                                </label>
                                <input
                                    id="inv-discount"
                                    type="number"
                                    step="0.01"
                                    value={discountAmount}
                                    onChange={e => setDiscountAmount(e.target.value)}
                                    placeholder="0.00"
                                />
                            </div>

                            {/* LINE ITEMS */}
                            <div className="invoice-items-builder">
                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                                    <label style={{ fontSize: "14px", fontWeight: "700", color: "var(--text-primary)" }}>
                                        Invoice Items <span className="req-star">*</span>
                                    </label>
                                    <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                                        {items.length} {items.length === 1 ? "item" : "items"}
                                    </span>
                                </div>

                                {items.map((item, index) => {
                                    const selectedProd = products.find(p => p.id === Number(item.productId));
                                    const lineTotal = selectedProd ? selectedProd.price * (Number(item.quantity) || 0) : 0;

                                    return (
                                        <div className="invoice-item-row" key={index}>
                                            <select
                                                value={item.productId}
                                                onChange={e => updateItem(index, "productId", e.target.value)}
                                                required
                                            >
                                                <option value="">Select Product *</option>
                                                {products.map(p => (
                                                    <option key={p.id} value={p.id}>
                                                        {p.name} — ₹{p.price} ({p.gstRate}% GST)
                                                    </option>
                                                ))}
                                            </select>

                                            <input
                                                type="number"
                                                min="1"
                                                placeholder="Qty *"
                                                value={item.quantity}
                                                onChange={e => updateItem(index, "quantity", e.target.value)}
                                                required
                                            />

                                            {items.length > 1 && (
                                                <button
                                                    type="button"
                                                    className="remove-item-btn"
                                                    title="Remove item"
                                                    onClick={() => removeItem(index)}
                                                >
                                                    ×
                                                </button>
                                            )}
                                        </div>
                                    );
                                })}

                                <button
                                    type="button"
                                    className="add-item-btn"
                                    onClick={addItem}
                                >
                                    + Add Another Product
                                </button>
                            </div>

                            <div style={{ display: "flex", gap: "10px", marginTop: "24px" }}>
                                <button
                                    type="button"
                                    className="secondary-button"
                                    style={{ flex: 1 }}
                                    disabled={saving}
                                    onClick={e => handleCreateInvoice(e, "DRAFT")}
                                >
                                    Save as Draft
                                </button>
                                <button
                                    type="button"
                                    className="primary-button"
                                    style={{ flex: 1 }}
                                    disabled={saving}
                                    onClick={e => handleCreateInvoice(e, "ISSUED")}
                                >
                                    {saving ? "Creating..." : "Create & Issue"}
                                </button>
                            </div>
                        </form>
                    </section>

                    {/* RIGHT PANEL: Live Summary */}
                    <section className="data-panel" style={{ padding: "24px" }}>
                        <span className="eyebrow">PREVIEW</span>
                        <h2 style={{ fontSize: "18px", fontWeight: "700", marginBottom: "16px" }}>Bill Breakdown</h2>

                        {items.every(i => !i.productId) ? (
                            <p style={{ color: "var(--text-muted)", fontSize: "14px" }}>
                                Select a customer and add products on the left to see instant tax calculations.
                            </p>
                        ) : (
                            <div>
                                <div style={{ borderBottom: "1px solid var(--border-color)", paddingBottom: "12px", marginBottom: "12px" }}>
                                    {items.map((item, index) => {
                                        const prod = products.find(p => p.id === Number(item.productId));
                                        if (!prod) return null;
                                        const qty = Number(item.quantity) || 0;
                                        const lineAmount = prod.price * qty;

                                        return (
                                            <div
                                                key={index}
                                                style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px", fontSize: "14px" }}
                                            >
                                                <div>
                                                    <strong>{prod.name}</strong>
                                                    <span style={{ display: "block", color: "var(--text-muted)", fontSize: "12px" }}>
                                                        {qty} × {formatCurrency(prod.price)} · GST {prod.gstRate}%
                                                    </span>
                                                </div>
                                                <div style={{ fontWeight: "600" }}>{formatCurrency(lineAmount)}</div>
                                            </div>
                                        );
                                    })}
                                </div>

                                <div className="invoice-summary-box">
                                    <div className="summary-line">
                                        <span>Taxable Amount</span>
                                        <strong>{formatCurrency(summary.taxableAmount)}</strong>
                                    </div>

                                    {summary.cgst > 0 && (
                                        <div className="summary-line">
                                            <span>CGST</span>
                                            <span>{formatCurrency(summary.cgst)}</span>
                                        </div>
                                    )}

                                    {summary.sgst > 0 && (
                                        <div className="summary-line">
                                            <span>SGST</span>
                                            <span>{formatCurrency(summary.sgst)}</span>
                                        </div>
                                    )}

                                    {summary.igst > 0 && (
                                        <div className="summary-line">
                                            <span>IGST</span>
                                            <span>{formatCurrency(summary.igst)}</span>
                                        </div>
                                    )}

                                    <div className="summary-line">
                                        <span>Total GST</span>
                                        <span>{formatCurrency(summary.totalTax)}</span>
                                    </div>

                                    <div className="summary-line total">
                                        <span>Grand Total</span>
                                        <span style={{ color: "var(--primary)" }}>{formatCurrency(summary.grandTotal)}</span>
                                    </div>
                                </div>
                            </div>
                        )}
                    </section>
                </div>
            )}
        </div>
    );
}
