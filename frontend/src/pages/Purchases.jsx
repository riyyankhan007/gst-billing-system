import React, { useEffect, useState } from "react";
import Icon from "../components/Icon";
import { getPurchases, createPurchase, deletePurchase, recordPurchasePayment, getSuppliers, getProducts } from "../services/api";

export default function Purchases() {
    const [purchases, setPurchases] = useState([]);
    const [suppliers, setSuppliers] = useState([]);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    // Create Modal
    const [showModal, setShowModal] = useState(false);
    const [supplierId, setSupplierId] = useState("");
    const [supplierInvoiceNumber, setSupplierInvoiceNumber] = useState("");
    const [purchaseDate, setPurchaseDate] = useState(new Date().toISOString().split("T")[0]);
    const [dueDate, setDueDate] = useState("");
    const [paidAmount, setPaidAmount] = useState("0");
    const [items, setItems] = useState([
        { productId: "", productName: "", hsnCode: "", quantity: 1, unitPrice: "", gstRate: 18, discount: 0 }
    ]);
    const [submitting, setSubmitting] = useState(false);
    const [modalError, setModalError] = useState("");

    // Payment Modal
    const [payModalPurchase, setPayModalPurchase] = useState(null);
    const [payAmount, setPayAmount] = useState("");

    useEffect(() => {
        loadData();
    }, []);

    async function loadData() {
        try {
            setLoading(true);
            setError("");
            const [pList, sList, prodList] = await Promise.all([
                getPurchases(),
                getSuppliers(),
                getProducts()
            ]);
            setPurchases(pList);
            setSuppliers(sList);
            setProducts(prodList.filter(p => p.productType?.toUpperCase() !== "SERVICE"));
        } catch (err) {
            setError(err.message || "Failed to load purchases");
        } finally {
            setLoading(false);
        }
    }

    const fmt = val => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(val || 0));

    function openCreateModal() {
        setModalError("");
        if (suppliers.length > 0) {
            setSupplierId(suppliers[0].id);
        } else {
            setSupplierId("");
        }
        setSupplierInvoiceNumber("");
        setPurchaseDate(new Date().toISOString().split("T")[0]);
        setDueDate("");
        setPaidAmount("0");
        setItems([
            { productId: "", productName: "", hsnCode: "", quantity: 1, unitPrice: "", gstRate: 18, discount: 0 }
        ]);
        setShowModal(true);
    }

    function handleItemChange(idx, field, value) {
        const next = [...items];
        next[idx][field] = value;
        if (field === "productId" && value) {
            const p = products.find(prod => String(prod.id) === String(value));
            if (p) {
                next[idx].productName = p.name;
                next[idx].hsnCode = p.hsnCode || "";
                next[idx].unitPrice = p.price || "";
                next[idx].gstRate = p.gstRate !== undefined ? p.gstRate : 18;
            }
        }
        setItems(next);
    }

    function addItemRow() {
        setItems([
            ...items,
            { productId: "", productName: "", hsnCode: "", quantity: 1, unitPrice: "", gstRate: 18, discount: 0 }
        ]);
    }

    function removeItemRow(idx) {
        if (items.length > 1) {
            setItems(items.filter((_, i) => i !== idx));
        }
    }

    async function handleCreateSubmit(e) {
        e.preventDefault();
        if (!supplierId) {
            setModalError("Please select a supplier.");
            return;
        }
        if (!supplierInvoiceNumber.trim()) {
            setModalError("Supplier Bill/Invoice Number is required.");
            return;
        }

        try {
            setSubmitting(true);
            setModalError("");
            await createPurchase({
                supplierId: Number(supplierId),
                supplierInvoiceNumber: supplierInvoiceNumber.trim(),
                purchaseDate,
                dueDate: dueDate || null,
                paidAmount: Number(paidAmount || 0),
                items: items.map(it => ({
                    productId: it.productId ? Number(it.productId) : null,
                    productName: it.productName,
                    hsnCode: it.hsnCode,
                    quantity: Number(it.quantity || 1),
                    unitPrice: Number(it.unitPrice || 0),
                    gstRate: Number(it.gstRate || 0),
                    discount: Number(it.discount || 0)
                }))
            });

            setShowModal(false);
            setSuccessMessage("Purchase recorded and product stock automatically updated!");
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            setModalError(err.message || "Failed to save purchase bill");
        } finally {
            setSubmitting(false);
        }
    }

    async function handleRecordPayment(e) {
        e.preventDefault();
        if (!payModalPurchase || !payAmount) return;
        try {
            await recordPurchasePayment(payModalPurchase.id, Number(payAmount));
            setPayModalPurchase(null);
            setSuccessMessage("Payment to vendor recorded successfully.");
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            alert(err.message || "Failed to record payment");
        }
    }

    async function handleDelete(id) {
        if (!window.confirm("Deleting this purchase will revert added stock for these products. Are you sure?")) return;
        try {
            await deletePurchase(id);
            setSuccessMessage("Purchase deleted and stock reverted.");
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            setError(err.message || "Failed to delete purchase");
        }
    }

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">INWARD SUPPLIES & ITC</span>
                    <h1>Purchases & Inward Supplies</h1>
                    <p>Record vendor invoices, track Input Tax Credit (ITC), and automatically increase stock</p>
                </div>
                <button className="primary-button" onClick={openCreateModal} disabled={suppliers.length === 0}>
                    <Icon type="plus" size={13} />
                    <span>New Purchase Bill</span>
                </button>
            </div>

            {suppliers.length === 0 && (
                <div style={{ padding: "12px 16px", background: "rgba(245, 158, 11, 0.15)", color: "#b45309", borderRadius: "8px", display: "flex", alignItems: "center", gap: "8px" }}>
                    <Icon type="warning" size={16} color="#b45309" /> No suppliers configured yet. Please add a supplier first before recording purchases.
                </div>
            )}

            {successMessage && (
                <div style={{ padding: "12px 16px", background: "rgba(16, 185, 129, 0.15)", color: "#065f46", borderRadius: "8px", fontWeight: "600", display: "flex", alignItems: "center", gap: "8px" }}>
                    <Icon type="checkCircle" size={16} color="#065f46" /> {successMessage}
                </div>
            )}

            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.15)", color: "var(--danger)", borderRadius: "8px" }}>
                    {error}
                </div>
            )}

            <div className="table-card">
                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center" }}>
                        <div className="spinner" style={{ margin: "0 auto 12px" }} />
                        <p>Loading purchase records...</p>
                    </div>
                ) : purchases.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-state-icon">
                            <Icon type="purchase" size={26} />
                        </div>
                        <h3>No purchase bills recorded yet</h3>
                        <p>Record vendor inward bills to claim Input Tax Credit (ITC) and update inventory stock.</p>
                        {suppliers.length > 0 && (
                            <button className="primary-button" onClick={openCreateModal}>
                                <Icon type="plus" size={13} />
                                <span>Record First Purchase</span>
                            </button>
                        )}
                    </div>
                ) : (
                    <div className="table-container">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Bill #</th>
                                    <th>Supplier</th>
                                    <th style={{ textAlign: "right" }}>Taxable</th>
                                    <th style={{ textAlign: "right" }}>Input GST</th>
                                    <th style={{ textAlign: "right" }}>Total Bill</th>
                                    <th style={{ textAlign: "right" }}>Paid</th>
                                    <th style={{ textAlign: "right" }}>Balance</th>
                                    <th style={{ textAlign: "center" }}>Status</th>
                                    <th style={{ textAlign: "right" }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {purchases.map(p => (
                                    <tr key={p.id}>
                                        <td>{p.purchaseDate}</td>
                                        <td style={{ fontWeight: "600", letterSpacing: "0.2px" }}>{p.supplierInvoiceNumber}</td>
                                        <td><strong>{p.supplier ? p.supplier.name : "—"}</strong></td>
                                        <td className="table-num" style={{ textAlign: "right" }}>{fmt(p.taxableAmount)}</td>
                                        <td className="table-num" style={{ textAlign: "right", color: "#6366f1" }}>{fmt(p.totalTax)}</td>
                                        <td className="table-num" style={{ textAlign: "right", fontWeight: "700" }}>{fmt(p.grandTotal)}</td>
                                        <td className="table-num" style={{ textAlign: "right", color: "var(--success)" }}>{fmt(p.paidAmount)}</td>
                                        <td className="table-num" style={{ textAlign: "right", color: Number(p.balanceAmount) > 0 ? "var(--warning)" : "var(--success)", fontWeight: "600" }}>
                                            {fmt(p.balanceAmount)}
                                        </td>
                                        <td style={{ textAlign: "center" }}>
                                            <span className={`status-badge status-${(p.paymentStatus || "UNPAID").toLowerCase()}`}>
                                                {p.paymentStatus}
                                            </span>
                                        </td>
                                        <td style={{ textAlign: "right" }}>
                                            <div style={{ display: "inline-flex", gap: "6px", justifyContent: "flex-end" }}>
                                                {Number(p.balanceAmount) > 0 && (
                                                    <button
                                                        type="button"
                                                        className="action-btn-sm"
                                                        onClick={() => {
                                                            setPayModalPurchase(p);
                                                            setPayAmount(p.balanceAmount);
                                                        }}
                                                    >
                                                        Pay
                                                    </button>
                                                )}
                                                <button
                                                    type="button"
                                                    className="action-btn-sm"
                                                    style={{ color: "var(--danger)" }}
                                                    onClick={() => handleDelete(p.id)}
                                                >
                                                    Delete
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

            {/* Create Purchase Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "800px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px" }}>Record Inward Purchase Bill</h3>
                            <button className="btn-close" onClick={() => setShowModal(false)}><Icon type="close" size={14} /></button>
                        </div>

                        {modalError && (
                            <div style={{ padding: "10px", background: "rgba(239,68,68,0.15)", color: "var(--danger)", borderRadius: "6px", marginBottom: "12px", fontSize: "13px" }}>
                                {modalError}
                            </div>
                        )}

                        <form onSubmit={handleCreateSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Supplier / Vendor *
                                    </label>
                                    <select
                                        className="input"
                                        value={supplierId}
                                        onChange={e => setSupplierId(e.target.value)}
                                        required
                                    >
                                        {suppliers.map(s => (
                                            <option key={s.id} value={s.id}>
                                                {s.name} ({s.state || "State N/A"})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Supplier Invoice/Bill # *
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={supplierInvoiceNumber}
                                        onChange={e => setSupplierInvoiceNumber(e.target.value)}
                                        placeholder="e.g. BILL-9821"
                                        required
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Purchase Date *
                                    </label>
                                    <input
                                        type="date"
                                        className="input"
                                        value={purchaseDate}
                                        onChange={e => setPurchaseDate(e.target.value)}
                                        required
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Due Date (Optional)
                                    </label>
                                    <input
                                        type="date"
                                        className="input"
                                        value={dueDate}
                                        onChange={e => setDueDate(e.target.value)}
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Initial Amount Paid (₹)
                                    </label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        className="input"
                                        value={paidAmount}
                                        onChange={e => setPaidAmount(e.target.value)}
                                    />
                                </div>
                            </div>

                            {/* Item Rows */}
                            <div>
                                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                    <h4 style={{ margin: 0, fontSize: "14px" }}>Items Purchased</h4>
                                    <button type="button" className="btn btn-secondary btn-sm" onClick={addItemRow} style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
                                        <Icon type="plus" size={12} />
                                        <span>Add Item</span>
                                    </button>
                                </div>

                                <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                                    {items.map((it, idx) => (
                                        <div key={idx} style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1fr 1fr 1fr auto", gap: "8px", alignItems: "center" }}>
                                            <select
                                                className="input"
                                                value={it.productId}
                                                onChange={e => handleItemChange(idx, "productId", e.target.value)}
                                                required
                                            >
                                                <option value="">Select Existing Product...</option>
                                                {products.map(p => (
                                                    <option key={p.id} value={p.id}>
                                                        {p.name} (Cur. Stock: {p.stockQuantity || 0})
                                                    </option>
                                                ))}
                                            </select>

                                            <input
                                                type="number"
                                                min="1"
                                                step="0.01"
                                                className="input"
                                                placeholder="Qty"
                                                value={it.quantity}
                                                onChange={e => handleItemChange(idx, "quantity", e.target.value)}
                                                required
                                            />

                                            <input
                                                type="number"
                                                step="0.01"
                                                className="input"
                                                placeholder="Rate (₹)"
                                                value={it.unitPrice}
                                                onChange={e => handleItemChange(idx, "unitPrice", e.target.value)}
                                                required
                                            />

                                            <select
                                                className="input"
                                                value={it.gstRate}
                                                onChange={e => handleItemChange(idx, "gstRate", Number(e.target.value))}
                                            >
                                                <option value="0">0% GST</option>
                                                <option value="5">5% GST</option>
                                                <option value="12">12% GST</option>
                                                <option value="18">18% GST</option>
                                                <option value="28">28% GST</option>
                                            </select>

                                            <input
                                                type="number"
                                                step="0.01"
                                                className="input"
                                                placeholder="Disc"
                                                value={it.discount}
                                                onChange={e => handleItemChange(idx, "discount", e.target.value)}
                                            />

                                            <button
                                                type="button"
                                                className="btn btn-danger btn-sm"
                                                onClick={() => removeItemRow(idx)}
                                                disabled={items.length === 1}
                                                title="Remove item"
                                            >
                                                <Icon type="trash" size={12} />
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "12px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? "Saving & Updating Stock..." : "Save Purchase Bill"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Pay Purchase Balance Modal */}
            {payModalPurchase && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "450px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px" }}>Pay Vendor Bill</h3>
                            <button className="btn-close" onClick={() => setPayModalPurchase(null)}><Icon type="close" size={14} /></button>
                        </div>
                        <form onSubmit={handleRecordPayment} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                            <div>
                                <span style={{ fontSize: "13px", color: "var(--muted)" }}>Bill #{payModalPurchase.supplierInvoiceNumber}</span>
                                <div style={{ fontSize: "16px", fontWeight: "700", margin: "4px 0" }}>
                                    Outstanding: {fmt(payModalPurchase.balanceAmount)}
                                </div>
                            </div>
                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Amount to Pay (₹) *
                                </label>
                                <input
                                    type="number"
                                    step="0.01"
                                    max={payModalPurchase.balanceAmount}
                                    className="input"
                                    value={payAmount}
                                    onChange={e => setPayAmount(e.target.value)}
                                    required
                                />
                            </div>
                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setPayModalPurchase(null)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    Confirm Payment
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
