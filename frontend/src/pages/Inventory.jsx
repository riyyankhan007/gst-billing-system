import React, { useEffect, useState } from "react";
import { getInventoryMovements, adjustStock, getProducts } from "../services/api";

export default function Inventory() {
    const [movements, setMovements] = useState([]);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    // Modal
    const [showModal, setShowModal] = useState(false);
    const [selectedProductId, setSelectedProductId] = useState("");
    const [newQty, setNewQty] = useState("");
    const [reason, setReason] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [modalError, setModalError] = useState("");

    useEffect(() => {
        loadData();
    }, []);

    async function loadData() {
        try {
            setLoading(true);
            setError("");
            const [movs, prods] = await Promise.all([
                getInventoryMovements(),
                getProducts()
            ]);
            setMovements(movs);
            setProducts(prods.filter(p => p.productType?.toUpperCase() !== "SERVICE"));
        } catch (err) {
            setError(err.message || "Failed to load inventory");
        } finally {
            setLoading(false);
        }
    }

    function openAdjustModal(p = null) {
        setModalError("");
        if (p) {
            setSelectedProductId(p.id);
            setNewQty(p.stockQuantity || "0");
        } else if (products.length > 0) {
            setSelectedProductId(products[0].id);
            setNewQty(products[0].stockQuantity || "0");
        }
        setReason("Physical stock audit adjustment");
        setShowModal(true);
    }

    async function handleAdjustSubmit(e) {
        e.preventDefault();
        if (!selectedProductId) return;

        try {
            setSubmitting(true);
            setModalError("");
            await adjustStock(Number(selectedProductId), Number(newQty), reason);
            setShowModal(false);
            setSuccessMessage("Stock quantity adjusted successfully!");
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadData();
        } catch (err) {
            setModalError(err.message || "Failed to adjust stock");
        } finally {
            setSubmitting(false);
        }
    }

    const lowStockItems = products.filter(p => Number(p.stockQuantity || 0) <= Number(p.lowStockThreshold || 5));

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">STOCK AUDIT TRAIL</span>
                    <h1>Inventory & Stock Movements</h1>
                    <p>Real-time stock audit trails, inward purchase additions, and invoice deductions</p>
                </div>
                <button className="primary-button" onClick={() => openAdjustModal()} disabled={products.length === 0}>
                    Adjust Stock Manually
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

            {/* Low stock alerts section */}
            {lowStockItems.length > 0 && (
                <div className="card" style={{ padding: "16px 20px", borderLeft: "4px solid #f59e0b" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                        <h4 style={{ margin: 0, color: "#b45309", fontSize: "15px" }}>
                            ⚠️ Low Stock Threshold Warnings ({lowStockItems.length})
                        </h4>
                    </div>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: "10px" }}>
                        {lowStockItems.map(p => (
                            <div key={p.id} style={{ background: "rgba(245, 158, 11, 0.1)", padding: "6px 12px", borderRadius: "6px", fontSize: "13px" }}>
                                <strong>{p.name}</strong>: Current <strong>{p.stockQuantity || 0}</strong> {p.unit || "units"} (Min: {p.lowStockThreshold || 5})
                                <button className="btn-link" style={{ marginLeft: "8px" }} onClick={() => openAdjustModal(p)}>Adjust</button>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Stock Movements Log Table */}
            <div className="table-card">
                <div className="table-header-card">
                    <div>
                        <h3 style={{ margin: 0, fontSize: "16px", fontWeight: "700" }}>Stock Audit Trail</h3>
                        <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Complete immutable ledger of all stock ins, outs, purchases, and manual adjustments</span>
                    </div>
                </div>
                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center" }}>
                        <div className="spinner" style={{ margin: "0 auto 12px" }} />
                        <p>Loading inventory history...</p>
                    </div>
                ) : movements.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                        <p>No inventory movements recorded yet. Issue invoices or record purchases to see stock flow.</p>
                    </div>
                ) : (
                    <div className="table-container">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>Product</th>
                                    <th>Movement Type</th>
                                    <th>Ref #</th>
                                    <th style={{ textAlign: "right" }}>Quantity</th>
                                    <th style={{ textAlign: "right" }}>Stock Before</th>
                                    <th style={{ textAlign: "right", paddingRight: "24px" }}>Stock After</th>
                                    <th style={{ paddingLeft: "16px" }}>Reason / Notes</th>
                                </tr>
                            </thead>
                            <tbody>
                                {movements.map(m => {
                                    const isPositive = Number(m.quantity) > 0;
                                    return (
                                        <tr key={m.id}>
                                            <td style={{ fontSize: "12.5px", color: "var(--text-secondary)", whiteSpace: "nowrap" }}>
                                                {m.createdAt ? new Date(m.createdAt).toLocaleString("en-IN", { dateStyle: "short", timeStyle: "medium" }) : "—"}
                                            </td>
                                            <td><strong>{m.product ? m.product.name : "Product"}</strong></td>
                                            <td>
                                                <span className="badge" style={{
                                                    background: m.movementType === "PURCHASE" ? "rgba(16,185,129,0.12)" : m.movementType === "INVOICE" ? "rgba(99,102,241,0.12)" : "rgba(245,158,11,0.12)",
                                                    color: m.movementType === "PURCHASE" ? "#065f46" : m.movementType === "INVOICE" ? "#4338ca" : "#b45309",
                                                    fontWeight: 600
                                                }}>
                                                    {m.movementType}
                                                </span>
                                            </td>
                                            <td style={{ letterSpacing: "0.2px", color: "var(--text-primary)", fontWeight: 500 }}>
                                                {m.referenceNumber || "—"}
                                            </td>
                                            <td className="table-num" style={{ textAlign: "right", fontWeight: "700", color: isPositive ? "#10b981" : "#ef4444" }}>
                                                {isPositive ? `+${m.quantity}` : m.quantity}
                                            </td>
                                            <td className="table-num" style={{ textAlign: "right" }}>
                                                {m.stockBefore !== null ? m.stockBefore : "—"}
                                            </td>
                                            <td className="table-num" style={{ textAlign: "right", paddingRight: "24px", fontWeight: "700" }}>
                                                {m.stockAfter !== null ? m.stockAfter : "—"}
                                            </td>
                                            <td style={{ paddingLeft: "16px", fontSize: "12.5px", color: "var(--text-secondary)" }}>
                                                {m.notes || "—"}
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Adjust Stock Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "450px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px" }}>Adjust Inventory Stock</h3>
                            <button className="btn-close" onClick={() => setShowModal(false)}>✕</button>
                        </div>

                        {modalError && (
                            <div style={{ padding: "10px", background: "rgba(239,68,68,0.15)", color: "var(--danger)", borderRadius: "6px", marginBottom: "12px", fontSize: "13px" }}>
                                {modalError}
                            </div>
                        )}

                        <form onSubmit={handleAdjustSubmit} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Product *
                                </label>
                                <select
                                    className="input"
                                    value={selectedProductId}
                                    onChange={e => {
                                        setSelectedProductId(e.target.value);
                                        const p = products.find(prod => String(prod.id) === e.target.value);
                                        if (p) setNewQty(p.stockQuantity || "0");
                                    }}
                                    required
                                >
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.name} (Current: {p.stockQuantity || 0} {p.unit || ""})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    New Count / Physical Quantity *
                                </label>
                                <input
                                    type="number"
                                    step="0.01"
                                    className="input"
                                    value={newQty}
                                    onChange={e => setNewQty(e.target.value)}
                                    required
                                />
                            </div>

                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Adjustment Reason / Notes
                                </label>
                                <input
                                    type="text"
                                    className="input"
                                    value={reason}
                                    onChange={e => setReason(e.target.value)}
                                    placeholder="e.g. Stock audit, damaged units, recount"
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? "Saving..." : "Save Stock Count"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
