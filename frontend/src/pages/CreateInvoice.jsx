import { useEffect, useState } from "react";
import Icon from "../components/Icon";
import {
    getBusinesses,
    getCustomers,
    getProducts,
    getWarehouses,
    createInvoice
} from "../services/api";
import { Skeleton, SkeletonCard } from "../components/ui/Skeleton";
import EmptyState from "../components/ui/EmptyState";
import { useToast } from "../context/ToastContext";

export default function CreateInvoice({ onBack, onCreated, onNavigate }) {
    const toast = useToast();
    const [businesses, setBusinesses] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [products, setProducts] = useState([]);
    const [warehouses, setWarehouses] = useState([]);

    const [businessId, setBusinessId] = useState("");
    const [warehouseId, setWarehouseId] = useState("");
    const [customerId, setCustomerId] = useState("");
    const [invoiceDate, setInvoiceDate] = useState(() => new Date().toISOString().split("T")[0]);
    const [dueDate, setDueDate] = useState("");
    const [discountAmount, setDiscountAmount] = useState("0");
    const [notes, _setNotes] = useState("");
    const [termsAndConditions, _setTermsAndConditions] = useState("");

    const [items, setItems] = useState([
        {
            productId: "",
            quantity: 1,
            discount: 0
        }
    ]);

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [savingStatus, setSavingStatus] = useState(""); // "DRAFT" | "ISSUED" | ""
    const [message, setMessage] = useState({ text: "", type: "" });


    useEffect(() => {
        async function loadFormData() {
            try {
                setLoading(true);
                const [bizList, custList, prodList, whList] = await Promise.all([
                    getBusinesses(),
                    getCustomers(),
                    getProducts(),
                    getWarehouses().catch(() => [])
                ]);

                setBusinesses(bizList || []);
                setCustomers(custList || []);
                setProducts(prodList || []);
                setWarehouses(whList || []);

                if (bizList && bizList.length > 0) {
                    setBusinessId(String(bizList[0].id));
                }
                if (whList && whList.length > 0) {
                    const defWh = whList.find(w => w.isDefault) || whList[0];
                    setWarehouseId(String(defWh.id));
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

    // Real-time calculation of totals matching GstCalculationService
    function calculateSummary() {
        let sumTaxable = 0;
        let sumTotalTax = 0;

        items.forEach(item => {
            const prod = products.find(p => p.id === Number(item.productId));
            if (!prod) return;

            const qty = Number(item.quantity) || 0;
            const price = Number(prod.price) || 0;
            const rate = Number(prod.gstRate) || 0;
            const discount = Number(item.discount) || 0;

            const baseTotal = qty * price;
            const lineAfterDiscount = Math.max(0, baseTotal - discount);

            let lineTaxable = 0;
            let lineTax = 0;

            if (prod.taxInclusive && rate > 0) {
                const divisor = 1 + rate / 100;
                lineTaxable = Number((lineAfterDiscount / divisor).toFixed(2));
                lineTax = Number((lineAfterDiscount - lineTaxable).toFixed(2));
            } else {
                lineTaxable = lineAfterDiscount;
                lineTax = Number((lineTaxable * (rate / 100)).toFixed(2));
            }

            sumTaxable += lineTaxable;
            sumTotalTax += lineTax;
        });

        const invoiceDisc = Number(discountAmount) || 0;
        if (invoiceDisc > 0) {
            sumTaxable = Math.max(0, sumTaxable - invoiceDisc);
        }

        const selectedBiz = businesses.find(b => b.id === Number(businessId));
        const selectedCust = customers.find(c => c.id === Number(customerId));

        let cgst = 0;
        let sgst = 0;
        let igst = 0;

        if (selectedBiz && selectedCust && selectedBiz.state && selectedCust.state) {
            const isIntraState = selectedBiz.state.trim().toLowerCase() === selectedCust.state.trim().toLowerCase();
            if (isIntraState) {
                cgst = Number((sumTotalTax / 2).toFixed(2));
                sgst = Number((sumTotalTax - cgst).toFixed(2));
            } else {
                igst = sumTotalTax;
            }
        } else {
            cgst = Number((sumTotalTax / 2).toFixed(2));
            sgst = Number((sumTotalTax - cgst).toFixed(2));
        }

        const rawGrandTotal = sumTaxable + sumTotalTax;
        const grandTotal = Math.round(rawGrandTotal);
        const roundOff = Number((grandTotal - rawGrandTotal).toFixed(2));

        return {
            taxableAmount: sumTaxable,
            totalTax: sumTotalTax,
            cgst,
            sgst,
            igst,
            roundOff,
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
        setSavingStatus(desiredStatus);

        const payload = {
            customerId: Number(customerId),
            warehouseId: warehouseId ? Number(warehouseId) : null,
            invoiceDate: invoiceDate,
            dueDate: dueDate || null,
            discountAmount: Number(discountAmount || 0),
            notes: notes,
            termsAndConditions: termsAndConditions,
            status: desiredStatus,
            items: items.map(i => {
                const prod = products.find(p => p.id === Number(i.productId));
                return {
                    productId: Number(i.productId),
                    quantity: Number(i.quantity),
                    discount: Number(i.discount || 0),
                    taxInclusive: prod ? Boolean(prod.taxInclusive) : false
                };
            })
        };

        const idempotencyKey = (typeof crypto !== "undefined" && crypto.randomUUID)
            ? crypto.randomUUID()
            : ("inv_" + Date.now() + "_" + Math.random().toString(36).substring(2, 9));

        try {
            const created = await createInvoice(payload, idempotencyKey);
            toast.success(`Invoice #${created.invoiceNumber} created (${created.status})!`);
            setMessage({ text: `Invoice #${created.invoiceNumber} created (${created.status})!`, type: "success" });
            setTimeout(() => {
                if (onCreated) {
                    onCreated(created.id);
                } else if (onBack) {
                    onBack();
                }
            }, 800);
        } catch (err) {
            toast.error(err.message || "Failed to create invoice");
            setMessage({ text: err.message || "Failed to create invoice", type: "error" });
        } finally {
            setSaving(false);
            setSavingStatus("");
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
                className="action-btn-sm"
                style={{ marginBottom: "16px" }}
                onClick={onBack}
                title="Back to Invoices"
            >
                <Icon type="arrowLeft" size={13} />
                <span>Back to Invoices</span>
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
                <div className="management-layout" aria-busy="true">
                    <section className="form-panel" style={{ padding: "24px" }}>
                        <Skeleton width="180px" height="22px" style={{ marginBottom: "20px" }} />
                        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                            <div>
                                <Skeleton width="120px" height="13px" style={{ marginBottom: "6px" }} />
                                <Skeleton width="100%" height="40px" borderRadius="6px" />
                            </div>
                            <div>
                                <Skeleton width="140px" height="13px" style={{ marginBottom: "6px" }} />
                                <Skeleton width="100%" height="40px" borderRadius="6px" />
                            </div>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <Skeleton width="100px" height="13px" style={{ marginBottom: "6px" }} />
                                    <Skeleton width="100%" height="40px" borderRadius="6px" />
                                </div>
                                <div>
                                    <Skeleton width="100px" height="13px" style={{ marginBottom: "6px" }} />
                                    <Skeleton width="100%" height="40px" borderRadius="6px" />
                                </div>
                            </div>
                            <div>
                                <Skeleton width="150px" height="16px" style={{ marginBottom: "12px" }} />
                                <Skeleton width="100%" height="52px" borderRadius="6px" />
                            </div>
                            <div style={{ display: "flex", gap: "10px", marginTop: "12px" }}>
                                <Skeleton width="50%" height="40px" borderRadius="6px" />
                                <Skeleton width="50%" height="40px" borderRadius="6px" />
                            </div>
                        </div>
                    </section>
                    <section className="data-panel" style={{ padding: "24px" }}>
                        <Skeleton width="100px" height="12px" style={{ marginBottom: "8px" }} />
                        <Skeleton width="160px" height="22px" style={{ marginBottom: "20px" }} />
                        <SkeletonCard lines={4} hasHeader={false} />
                    </section>
                </div>
            ) : customers.length === 0 ? (
                <EmptyState
                    icon="customer"
                    title="No customers registered yet"
                    description="You need to create at least one customer before generating a GST invoice."
                    actionLabel="Add New Customer"
                    onAction={() => onNavigate && onNavigate("customers")}
                />
            ) : products.length === 0 ? (
                <EmptyState
                    icon="product"
                    title="No catalogue products found"
                    description="You need to add at least one product or service before generating a GST invoice."
                    actionLabel="Add First Product"
                    onAction={() => onNavigate && onNavigate("products")}
                />
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

                            {warehouses.length > 0 && (
                                <div className="form-field">
                                    <label htmlFor="inv-warehouse">
                                        Dispatch Warehouse / Location <span className="req-star">*</span>
                                    </label>
                                    <select
                                        id="inv-warehouse"
                                        value={warehouseId}
                                        onChange={e => setWarehouseId(e.target.value)}
                                        required
                                    >
                                        {warehouses.map(w => (
                                            <option key={w.id} value={w.id}>
                                                {w.name} ({w.code}){w.isDefault ? " (Default)" : ""}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}

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
                                            {c.name} ({c.state || "No State"}{c.phone ? ` · Ph: ${c.phone}` : ""})
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
                                                        {p.name}{p.hsnCode ? ` [${p.productType === "SERVICE" ? "SAC" : "HSN"}: ${p.hsnCode}]` : ""} — ₹{p.price} ({p.gstRate}% GST{p.taxInclusive ? " · Incl." : ""})
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
                                                    <Icon type="close" size={13} />
                                                </button>
                                            )}
                                        </div>
                                    );
                                })}

                                <button
                                    type="button"
                                    className="add-item-btn"
                                    onClick={addItem}
                                    style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
                                >
                                    <Icon type="plus" size={13} />
                                    <span>Add Another Product</span>
                                </button>
                            </div>

                            <div style={{ display: "flex", gap: "10px", marginTop: "24px" }}>
                                <button
                                    type="button"
                                    className={`secondary-button ${savingStatus === "DRAFT" ? "btn-loading" : ""}`}
                                    style={{ flex: 1, display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "6px" }}
                                    disabled={saving}
                                    onClick={e => handleCreateInvoice(e, "DRAFT")}
                                >
                                    {savingStatus === "DRAFT" && (
                                        <span
                                            className="inline-spinner"
                                            style={{
                                                width: "12px",
                                                height: "12px",
                                                border: "2px solid var(--text-muted)",
                                                borderTopColor: "var(--text-primary)",
                                                borderRadius: "50%"
                                            }}
                                        />
                                    )}
                                    <span>{savingStatus === "DRAFT" ? "Saving Draft..." : "Save as Draft"}</span>
                                </button>
                                <button
                                    type="button"
                                    className={`primary-button ${savingStatus === "ISSUED" ? "btn-loading" : ""}`}
                                    style={{ flex: 1, display: "inline-flex", alignItems: "center", justifyContent: "center", gap: "6px" }}
                                    disabled={saving}
                                    onClick={e => handleCreateInvoice(e, "ISSUED")}
                                >
                                    {savingStatus === "ISSUED" && (
                                        <span
                                            className="inline-spinner"
                                            style={{
                                                width: "12px",
                                                height: "12px",
                                                border: "2px solid rgba(255, 255, 255, 0.4)",
                                                borderTopColor: "#ffffff",
                                                borderRadius: "50%"
                                            }}
                                        />
                                    )}
                                    <span>{savingStatus === "ISSUED" ? "Creating & Issuing..." : "Create & Issue"}</span>
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
