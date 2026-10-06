import React, { useEffect, useState } from "react";
import Icon from "../components/Icon";
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier, getSupplierDetails } from "../services/api";

export default function Suppliers() {
    const [suppliers, setSuppliers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [search, setSearch] = useState("");

    // Modal
    const [showModal, setShowModal] = useState(false);
    const [editingSupplier, setEditingSupplier] = useState(null);
    const [formData, setFormData] = useState({
        name: "",
        gstin: "",
        pan: "",
        phone: "",
        email: "",
        address: "",
        state: "Maharashtra",
        stateCode: "27",
        paymentTerms: "30"
    });
    const [submitting, setSubmitting] = useState(false);
    const [modalError, setModalError] = useState("");

    // Details Modal
    const [detailsSupplier, setDetailsSupplier] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);

    useEffect(() => {
        loadSuppliers();
    }, [search]);

    async function loadSuppliers() {
        try {
            setLoading(true);
            setError("");
            const list = await getSuppliers(search);
            setSuppliers(list);
        } catch (err) {
            setError(err.message || "Failed to load suppliers");
        } finally {
            setLoading(false);
        }
    }

    function openModal(sup = null) {
        setModalError("");
        if (sup) {
            setEditingSupplier(sup);
            setFormData({
                name: sup.name || "",
                gstin: sup.gstin || "",
                pan: sup.pan || "",
                phone: sup.phone || "",
                email: sup.email || "",
                address: sup.address || "",
                state: sup.state || "Maharashtra",
                stateCode: sup.stateCode || "27",
                paymentTerms: sup.paymentTerms || "30"
            });
        } else {
            setEditingSupplier(null);
            setFormData({
                name: "",
                gstin: "",
                pan: "",
                phone: "",
                email: "",
                address: "",
                state: "Maharashtra",
                stateCode: "27",
                paymentTerms: "30"
            });
        }
        setShowModal(true);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        try {
            setSubmitting(true);
            setModalError("");
            if (editingSupplier) {
                await updateSupplier(editingSupplier.id, formData);
                setSuccessMessage("Supplier updated successfully!");
            } else {
                await createSupplier(formData);
                setSuccessMessage("Supplier created successfully!");
            }
            setShowModal(false);
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadSuppliers();
        } catch (err) {
            setModalError(err.message || "Failed to save supplier");
        } finally {
            setSubmitting(false);
        }
    }

    async function handleDelete(id) {
        if (!window.confirm("Are you sure you want to delete this supplier?")) return;
        try {
            await deleteSupplier(id);
            setSuccessMessage("Supplier deleted successfully.");
            setTimeout(() => setSuccessMessage(""), 4000);
            await loadSuppliers();
        } catch (err) {
            setError(err.message || "Failed to delete supplier");
        }
    }

    async function viewDetails(id) {
        try {
            setDetailsLoading(true);
            const details = await getSupplierDetails(id);
            setDetailsSupplier(details);
        } catch (err) {
            setError(err.message || "Failed to load supplier details");
        } finally {
            setDetailsLoading(false);
        }
    }

    const fmt = val => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(val || 0));

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">VENDOR MASTER</span>
                    <h1>Suppliers & Vendors</h1>
                    <p>Manage vendor records, GSTINs, and purchase credit histories</p>
                </div>
                <button className="primary-button" onClick={() => openModal()}>
                    <Icon type="plus" size={13} />
                    <span>Add Supplier</span>
                </button>
            </div>

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

            <div className="card" style={{ padding: "16px 20px" }}>
                <div className="search-input-wrapper" style={{ minWidth: "280px", maxWidth: "420px" }}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8" />
                        <line x1="21" y1="21" x2="16.65" y2="16.65" />
                    </svg>
                    <input
                        type="text"
                        className="search-input"
                        placeholder="Search by supplier name, GSTIN, or phone..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                    />
                    {search && (
                        <button
                            type="button"
                            className="search-clear-btn"
                            onClick={() => setSearch("")}
                            title="Clear search"
                        >
                            <Icon type="close" size={13} />
                        </button>
                    )}
                </div>
            </div>

            <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center" }}>
                        <div className="spinner" style={{ margin: "0 auto 12px" }} />
                        <p>Loading suppliers...</p>
                    </div>
                ) : suppliers.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-state-icon">
                            <Icon type="supplier" size={26} />
                        </div>
                        <h3>No suppliers found</h3>
                        <p>Maintain vendor directories, track purchase terms, and record inward bills.</p>
                        <button className="primary-button" onClick={() => openModal()}>
                            <Icon type="plus" size={13} />
                            <span>Add First Supplier</span>
                        </button>
                    </div>
                ) : (
                    <table className="table" style={{ width: "100%", margin: 0 }}>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>GSTIN</th>
                                <th>PAN</th>
                                <th>State</th>
                                <th>Phone</th>
                                <th>Terms</th>
                                <th style={{ textAlign: "center" }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {suppliers.map(s => (
                                <tr key={s.id}>
                                    <td style={{ fontWeight: "600" }}>{s.name}</td>
                                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{s.gstin || "Unregistered"}</td>
                                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{s.pan || "-"}</td>
                                    <td>{s.state || "-"}</td>
                                    <td>{s.phone || "-"}</td>
                                    <td>{s.paymentTerms ? `${s.paymentTerms} Days` : "-"}</td>
                                    <td style={{ textAlign: "center" }}>
                                        <div style={{ display: "inline-flex", gap: "6px" }}>
                                            <button className="btn btn-secondary btn-sm" style={{ padding: "3px 8px", fontSize: "11px" }} onClick={() => viewDetails(s.id)}>
                                                Ledger
                                            </button>
                                            <button className="btn btn-secondary btn-sm" style={{ padding: "3px 8px", fontSize: "11px" }} onClick={() => openModal(s)}>
                                                Edit
                                            </button>
                                            <button className="btn btn-danger btn-sm" style={{ padding: "3px 8px", fontSize: "11px" }} onClick={() => handleDelete(s.id)}>
                                                Delete
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Add / Edit Supplier Modal */}
            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "550px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px" }}>
                                {editingSupplier ? "Edit Supplier" : "Add New Supplier"}
                            </h3>
                            <button className="btn-close" onClick={() => setShowModal(false)}><Icon type="close" size={14} /></button>
                        </div>

                        {modalError && (
                            <div style={{ padding: "10px", background: "rgba(239,68,68,0.15)", color: "var(--danger)", borderRadius: "6px", marginBottom: "12px", fontSize: "13px" }}>
                                {modalError}
                            </div>
                        )}

                        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Supplier / Business Name *
                                </label>
                                <input
                                    type="text"
                                    className="input"
                                    value={formData.name}
                                    onChange={e => setFormData({ ...formData, name: e.target.value })}
                                    placeholder="e.g. Acme Supplies Pvt Ltd"
                                    required
                                />
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        GSTIN (Optional)
                                    </label>
                                    <input
                                        type="text"
                                        maxLength="15"
                                        className="input"
                                        value={formData.gstin}
                                        onChange={e => setFormData({ ...formData, gstin: e.target.value.toUpperCase() })}
                                        placeholder="15-digit GSTIN"
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        PAN (Optional)
                                    </label>
                                    <input
                                        type="text"
                                        maxLength="10"
                                        className="input"
                                        value={formData.pan}
                                        onChange={e => setFormData({ ...formData, pan: e.target.value.toUpperCase() })}
                                        placeholder="10-digit PAN"
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Phone Number
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={formData.phone}
                                        onChange={e => setFormData({ ...formData, phone: e.target.value })}
                                        placeholder="e.g. 9876543210"
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Email Address
                                    </label>
                                    <input
                                        type="email"
                                        className="input"
                                        value={formData.email}
                                        onChange={e => setFormData({ ...formData, email: e.target.value })}
                                        placeholder="billing@supplier.com"
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: "12px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        State
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={formData.state}
                                        onChange={e => setFormData({ ...formData, state: e.target.value })}
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Payment Terms (Days)
                                    </label>
                                    <input
                                        type="number"
                                        className="input"
                                        value={formData.paymentTerms}
                                        onChange={e => setFormData({ ...formData, paymentTerms: e.target.value })}
                                        placeholder="30"
                                    />
                                </div>
                            </div>

                            <div>
                                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                    Address
                                </label>
                                <textarea
                                    className="input"
                                    rows="2"
                                    value={formData.address}
                                    onChange={e => setFormData({ ...formData, address: e.target.value })}
                                    placeholder="Street, City, Pincode"
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary" disabled={submitting}>
                                    {submitting ? "Saving..." : "Save Supplier"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Supplier Details / Ledger Drawer */}
            {detailsSupplier && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "700px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <div>
                                <h3 style={{ margin: 0, fontSize: "18px" }}>{detailsSupplier.supplier.name}</h3>
                                <span style={{ fontSize: "12px", color: "var(--muted)" }}>
                                    GSTIN: {detailsSupplier.supplier.gstin || "Unregistered"} | {detailsSupplier.supplier.state}
                                </span>
                            </div>
                            <button className="btn-close" onClick={() => setDetailsSupplier(null)}><Icon type="close" size={14} /></button>
                        </div>

                        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "12px", marginBottom: "20px" }}>
                            <div style={{ background: "var(--bg-subtle, #f8fafc)", padding: "12px", borderRadius: "8px" }}>
                                <div style={{ fontSize: "11px", color: "var(--muted)" }}>Total Purchases</div>
                                <div style={{ fontSize: "16px", fontWeight: "700" }}>{fmt(detailsSupplier.totalPurchasesAmount)}</div>
                            </div>
                            <div style={{ background: "rgba(16, 185, 129, 0.1)", padding: "12px", borderRadius: "8px" }}>
                                <div style={{ fontSize: "11px", color: "#10b981", fontWeight: "600" }}>Paid Amount</div>
                                <div style={{ fontSize: "16px", fontWeight: "700", color: "#10b981" }}>{fmt(detailsSupplier.totalPaidAmount)}</div>
                            </div>
                            <div style={{ background: "rgba(239, 68, 68, 0.1)", padding: "12px", borderRadius: "8px" }}>
                                <div style={{ fontSize: "11px", color: "#ef4444", fontWeight: "600" }}>Outstanding Balance</div>
                                <div style={{ fontSize: "16px", fontWeight: "700", color: "#ef4444" }}>{fmt(detailsSupplier.totalOutstandingAmount)}</div>
                            </div>
                        </div>

                        <h4 style={{ margin: "0 0 10px", fontSize: "14px" }}>Purchase History ({detailsSupplier.totalPurchasesCount})</h4>
                        {detailsSupplier.purchases.length === 0 ? (
                            <p style={{ color: "var(--muted)", fontSize: "13px" }}>No purchases recorded for this supplier.</p>
                        ) : (
                            <table className="table" style={{ width: "100%", fontSize: "13px" }}>
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Bill #</th>
                                        <th style={{ textAlign: "right" }}>Total</th>
                                        <th style={{ textAlign: "right" }}>Paid</th>
                                        <th style={{ textAlign: "right" }}>Balance</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {detailsSupplier.purchases.map(p => (
                                        <tr key={p.id}>
                                            <td>{p.purchaseDate}</td>
                                            <td style={{ fontWeight: "600" }}>{p.supplierInvoiceNumber}</td>
                                            <td style={{ textAlign: "right" }}>{fmt(p.grandTotal)}</td>
                                            <td style={{ textAlign: "right", color: "#10b981" }}>{fmt(p.paidAmount)}</td>
                                            <td style={{ textAlign: "right", color: "#ef4444", fontWeight: "600" }}>{fmt(p.balanceAmount)}</td>
                                            <td><span className={`badge badge-${(p.paymentStatus || "UNPAID").toLowerCase()}`}>{p.paymentStatus}</span></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}

                        <div style={{ textAlign: "right", marginTop: "16px" }}>
                            <button className="btn btn-secondary" onClick={() => setDetailsSupplier(null)}>Close</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
