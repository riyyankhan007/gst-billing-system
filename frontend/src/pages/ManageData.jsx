import { useEffect, useState } from "react";
import Icon from "../components/Icon";
import {
    createCustomer,
    createProduct,
    getCustomers,
    getProducts,
    deleteCustomer,
    deleteProduct,
    getCustomerDetails
} from "../services/api";

const GST_RATES = [0, 5, 12, 18, 28];
const COMMON_UNITS = ["PCS", "NOS", "KGS", "MTR", "BOX", "SET", "LTR", "PKT", "BAG"];

export default function ManageData({ kind, onViewCustomer }) {
    const isCustomer = kind === "customers";

    const [items, setItems] = useState([]);
    const [form, setForm] = useState({});
    const [search, setSearch] = useState("");
    const [filterType, setFilterType] = useState("ALL");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [saving, setSaving] = useState(false);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);

    // Customer Details modal
    const [selectedCustomerDetails, setSelectedCustomerDetails] = useState(null);
    const [detailsLoading, setDetailsLoading] = useState(false);

    const loadData = async () => {
        try {
            setLoading(true);
            const data = isCustomer ? await getCustomers(search) : await getProducts(search);
            setItems(data || []);
            setError("");
        } catch (err) {
            setError(err.message || "Failed to load data");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        setForm(isCustomer ? {
            name: "",
            customerType: "B2B",
            gstin: "",
            pan: "",
            phone: "",
            email: "",
            state: "Maharashtra",
            stateCode: "27",
            billingAddress: "",
            shippingAddress: "",
            creditLimit: "0",
            paymentTerms: "15",
            openingBalance: "0"
        } : {
            name: "",
            productType: "PRODUCT",
            sku: "",
            hsnCode: "",
            price: "",
            gstRate: 18,
            unit: "PCS",
            discount: "0",
            taxInclusive: false,
            stockQuantity: "100",
            lowStockThreshold: "10"
        });
        setSearch("");
        setFilterType("ALL");
        setError("");
        setSuccess("");
        loadData();
    }, [kind]);

    const handleInputChange = (field, value) => {
        setForm(prev => ({
            ...prev,
            [field]: value
        }));
        if (error) setError("");
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setSuccess("");
        setSaving(true);

        try {
            const body = { ...form };

            if (isCustomer) {
                if (!body.name?.trim()) throw new Error("Customer name is required (*)");
                if (!body.state?.trim()) throw new Error("State is required (*)");

                if (body.gstin) body.gstin = body.gstin.trim().toUpperCase();
                if (body.pan) body.pan = body.pan.trim().toUpperCase();
                body.creditLimit = Number(body.creditLimit || 0);
                body.openingBalance = Number(body.openingBalance || 0);

                await createCustomer(body);
                setSuccess(`Customer "${body.name}" added successfully!`);
            } else {
                if (!body.name?.trim()) throw new Error("Product name is required (*)");
                if (body.price === undefined || body.price === "") throw new Error("Price is required (*)");

                body.price = Number(body.price);
                body.gstRate = Number(body.gstRate);
                body.discount = Number(body.discount || 0);
                body.stockQuantity = Number(body.stockQuantity || 0);
                body.lowStockThreshold = Number(body.lowStockThreshold || 5);
                body.taxInclusive = Boolean(body.taxInclusive);

                await createProduct(body);
                setSuccess(`Product "${body.name}" added successfully!`);
            }

            // Reset form
            setForm(isCustomer ? {
                name: "",
                customerType: "B2B",
                gstin: "",
                pan: "",
                phone: "",
                email: "",
                state: "Maharashtra",
                stateCode: "27",
                billingAddress: "",
                shippingAddress: "",
                creditLimit: "0",
                paymentTerms: "15",
                openingBalance: "0"
            } : {
                name: "",
                productType: "PRODUCT",
                sku: "",
                hsnCode: "",
                price: "",
                gstRate: 18,
                unit: "PCS",
                discount: "0",
                taxInclusive: false,
                stockQuantity: "100",
                lowStockThreshold: "10"
            });
            await loadData();
        } catch (err) {
            setError(err.message || "Failed to save record");
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id, name) => {
        const itemType = isCustomer ? "customer" : "product";
        if (!window.confirm(`Are you sure you want to delete ${name || thisItem}?`)) {
            return;
        }

        try {
            if (isCustomer) {
                await deleteCustomer(id);
            } else {
                await deleteProduct(id);
            }
            setSuccess(`${isCustomer ? "Customer" : "Product"} deleted successfully.`);
            await loadData();
        } catch (err) {
            setError(err.message || `Failed to delete ${itemType}`);
        }
    };

    const handleViewCustomerDetails = async (id) => {
        try {
            setDetailsLoading(true);
            const data = await getCustomerDetails(id);
            setSelectedCustomerDetails(data);
        } catch (err) {
            alert(err.message || "Failed to load customer details");
        } finally {
            setDetailsLoading(false);
        }
    };

    const fmt = val => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(val || 0));

    const filteredItems = items.filter(item => {
        const q = search.toLowerCase();
        const matchesSearch = !q
            || (item.name && item.name.toLowerCase().includes(q))
            || (item.gstin && item.gstin.toLowerCase().includes(q))
            || (item.phone && item.phone.toLowerCase().includes(q))
            || (item.sku && item.sku.toLowerCase().includes(q))
            || (item.hsnCode && item.hsnCode.toLowerCase().includes(q));

        if (!matchesSearch) return false;

        if (isCustomer) {
            return filterType === "ALL" || (item.customerType && item.customerType.toUpperCase() === filterType);
        } else {
            return filterType === "ALL" || (item.productType && item.productType.toUpperCase() === filterType);
        }
    });

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {/* Page Header */}
            <div className="page-heading">
                <div>
                    <span className="eyebrow">{isCustomer ? "CLIENT MASTER" : "INVENTORY & MASTER"}</span>
                    <h1>{isCustomer ? "Customer Directory & B2B/B2C Profiles" : "Products & Services Catalog"}</h1>
                    <p>
                        {isCustomer
                            ? "Manage customer GSTINs, official billing addresses, credit terms, and ledger history."
                            : "Define catalog items, HSN/SAC codes, standard GST rates, and inventory stock."}
                    </p>
                </div>
                <button
                    type="button"
                    className={showForm ? "secondary-button" : "primary-button"}
                    onClick={() => setShowForm(!showForm)}
                    style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}
                >
                    {showForm ? <Icon type="close" size={14} /> : <Icon type="plus" size={14} />}
                    <span>{showForm ? "Hide Form" : isCustomer ? "Add New Customer" : "Add New Item"}</span>
                </button>
            </div>

            {/* Error & Success Banners */}
            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.12)", color: "var(--danger)", borderRadius: "var(--radius-sm)", border: "1px solid #fecaca", fontWeight: 500 }}>
                    {error}
                </div>
            )}
            {success && (
                <div style={{ padding: "12px 16px", background: "rgba(16, 185, 129, 0.12)", color: "#065f46", borderRadius: "var(--radius-sm)", border: "1px solid #a7f3d0", fontWeight: 600, display: "flex", alignItems: "center", gap: "8px" }}>
                    <Icon type="checkCircle" size={16} color="#065f46" /> {success}
                </div>
            )}

            {/* Creation Form Card (Toggleable) */}
            {showForm && (
                <div className="form-card" style={{ animation: "fadeIn 0.2s ease" }}>
                    <div className="form-card-title" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <Icon type="plus" size={16} color="var(--primary)" />
                        {isCustomer ? "Add New Customer / Business Client" : "Add New Product / Service"}
                    </div>

                    <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
                        {/* CUSTOMER FORM FIELDS */}
                        {isCustomer && (
                            <>
                                {/* Section 1: Basic Profile & Classification */}
                                <div className="form-section-divider">
                                    <h4 className="form-section-title">1. Basic Profile & Customer Classification</h4>
                                    <span className="form-section-subtitle">Legal business identity and trading classification</span>
                                </div>
                                <div className="form-grid-4">
                                    <div className="form-field col-span-2">
                                        <label>Customer / Legal Company Name <span className="req-star">*</span></label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={form.name || ""}
                                            onChange={e => handleInputChange("name", e.target.value)}
                                            placeholder="e.g. Apex Enterprises Pvt Ltd"
                                            required
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Customer Type <span className="req-star">*</span></label>
                                        <select
                                            className="form-control"
                                            value={form.customerType || "B2B"}
                                            onChange={e => handleInputChange("customerType", e.target.value)}
                                        >
                                            <option value="B2B">B2B (Registered Business)</option>
                                            <option value="B2C">B2C (Consumer / Unregistered)</option>
                                            <option value="EXPORT">EXPORT (Overseas / SEZ)</option>
                                        </select>
                                    </div>
                                    <div className="form-field">
                                        <label>Payment Terms (Days)</label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            value={form.paymentTerms || "15"}
                                            onChange={e => handleInputChange("paymentTerms", e.target.value)}
                                            placeholder="15"
                                        />
                                    </div>
                                </div>

                                {/* Section 2: Tax Identification & Place of Supply */}
                                <div className="form-section-divider">
                                    <h4 className="form-section-title">2. Tax Identification & Place of Supply</h4>
                                    <span className="form-section-subtitle">GSTIN, PAN and official State of registration</span>
                                </div>
                                <div className="form-grid-4">
                                    <div className="form-field">
                                        <label>GSTIN (15 Digits)</label>
                                        <input
                                            type="text"
                                            maxLength="15"
                                            className="form-control"
                                            value={form.gstin || ""}
                                            onChange={e => handleInputChange("gstin", e.target.value.toUpperCase())}
                                            placeholder="e.g. 27AABCT3518Q1ZV"
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>PAN Number (10 Digits)</label>
                                        <input
                                            type="text"
                                            maxLength="10"
                                            className="form-control"
                                            value={form.pan || ""}
                                            onChange={e => handleInputChange("pan", e.target.value.toUpperCase())}
                                            placeholder="e.g. AABCT3518Q"
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>State / Place of Supply <span className="req-star">*</span></label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={form.state || ""}
                                            onChange={e => handleInputChange("state", e.target.value)}
                                            placeholder="e.g. Maharashtra"
                                            required
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>State Code (2 Digits)</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={form.stateCode || ""}
                                            onChange={e => handleInputChange("stateCode", e.target.value)}
                                            placeholder="27"
                                        />
                                    </div>
                                </div>

                                {/* Section 3: Contact & Financial Limits */}
                                <div className="form-section-divider">
                                    <h4 className="form-section-title">3. Contact Details & Credit Ledger</h4>
                                    <span className="form-section-subtitle">Primary billing email, phone, and credit controls</span>
                                </div>
                                <div className="form-grid-4">
                                    <div className="form-field">
                                        <label>Phone Number (WhatsApp)</label>
                                        <input
                                            type="tel"
                                            className="form-control"
                                            value={form.phone || ""}
                                            onChange={e => handleInputChange("phone", e.target.value)}
                                            placeholder="e.g. 9876543210"
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Billing Email Address</label>
                                        <input
                                            type="email"
                                            className="form-control"
                                            value={form.email || ""}
                                            onChange={e => handleInputChange("email", e.target.value)}
                                            placeholder="billing@customer.com"
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Credit Limit (₹)</label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            value={form.creditLimit || "0"}
                                            onChange={e => handleInputChange("creditLimit", e.target.value)}
                                            placeholder="0.00"
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Opening Balance (₹)</label>
                                        <input
                                            type="number"
                                            className="form-control"
                                            value={form.openingBalance || "0"}
                                            onChange={e => handleInputChange("openingBalance", e.target.value)}
                                            placeholder="0.00"
                                        />
                                    </div>
                                </div>

                                {/* Section 4: Registered Addresses */}
                                <div className="form-section-divider">
                                    <h4 className="form-section-title">4. Registered Addresses</h4>
                                    <span className="form-section-subtitle">Tax invoice billing address and delivery destination</span>
                                </div>
                                <div className="form-grid-2">
                                    <div className="form-field">
                                        <label>Registered Billing Address</label>
                                        <textarea
                                            className="form-control"
                                            rows="2"
                                            value={form.billingAddress || ""}
                                            onChange={e => handleInputChange("billingAddress", e.target.value)}
                                            placeholder="Registered corporate address for tax invoice"
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Shipping / Delivery Address (Optional)</label>
                                        <textarea
                                            className="form-control"
                                            rows="2"
                                            value={form.shippingAddress || ""}
                                            onChange={e => handleInputChange("shippingAddress", e.target.value)}
                                            placeholder="Warehouse or delivery destination (if different)"
                                        />
                                    </div>
                                </div>
                            </>
                        )}

                        {/* PRODUCT FORM FIELDS */}
                        {!isCustomer && (
                            <>
                                {/* Section 1: Item Identification */}
                                <div className="form-section-divider">
                                    <h4 className="form-section-title">1. Item Identification & Classification</h4>
                                    <span className="form-section-subtitle">Catalog name, type, SKU code, and GST HSN/SAC</span>
                                </div>
                                <div className="form-grid-4">
                                    <div className="form-field col-span-2">
                                        <label>Item / Product Name <span className="req-star">*</span></label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={form.name || ""}
                                            onChange={e => handleInputChange("name", e.target.value)}
                                            placeholder="e.g. Dell Monitor 27-inch 4K or IT Consulting"
                                            required
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Classification Type <span className="req-star">*</span></label>
                                        <select
                                            className="form-control"
                                            value={form.productType || "PRODUCT"}
                                            onChange={e => handleInputChange("productType", e.target.value)}
                                        >
                                            <option value="PRODUCT">Goods (Physical Product)</option>
                                            <option value="SERVICE">Service (Non-stock)</option>
                                        </select>
                                    </div>
                                    <div className="form-field">
                                        <label>SKU / Part Code</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={form.sku || ""}
                                            onChange={e => handleInputChange("sku", e.target.value)}
                                            placeholder="e.g. DELL-27-4K"
                                        />
                                    </div>
                                </div>

                                {/* Section 2: Pricing & Taxation */}
                                <div className="form-section-divider">
                                    <h4 className="form-section-title">2. Pricing & GST Taxation</h4>
                                    <span className="form-section-subtitle">Tax rates, measurement units, and price settings</span>
                                </div>
                                <div className="form-grid-4">
                                    <div className="form-field">
                                        <label>HSN / SAC Code</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={form.hsnCode || ""}
                                            onChange={e => handleInputChange("hsnCode", e.target.value)}
                                            placeholder="e.g. 8471 or 9983"
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Unit Base Price (₹) <span className="req-star">*</span></label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            className="form-control"
                                            value={form.price || ""}
                                            onChange={e => handleInputChange("price", e.target.value)}
                                            placeholder="0.00"
                                            required
                                        />
                                    </div>
                                    <div className="form-field">
                                        <label>Standard GST Rate <span className="req-star">*</span></label>
                                        <select
                                            className="form-control"
                                            value={form.gstRate || 18}
                                            onChange={e => handleInputChange("gstRate", Number(e.target.value))}
                                        >
                                            {GST_RATES.map(r => (
                                                <option key={r} value={r}>{r}% GST</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="form-field">
                                        <label>Unit of Measurement</label>
                                        <select
                                            className="form-control"
                                            value={form.unit || "PCS"}
                                            onChange={e => handleInputChange("unit", e.target.value)}
                                        >
                                            {COMMON_UNITS.map(u => (
                                                <option key={u} value={u}>{u}</option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                {/* Section 3: Inventory & Stock Control (For physical goods) */}
                                {form.productType === "PRODUCT" && (
                                    <>
                                        <div className="form-section-divider">
                                            <h4 className="form-section-title">3. Inventory & Stock Tracking</h4>
                                            <span className="form-section-subtitle">Initial warehouse counts and low stock warning thresholds</span>
                                        </div>
                                        <div className="form-grid-4">
                                            <div className="form-field">
                                                <label>Initial Stock Quantity</label>
                                                <input
                                                    type="number"
                                                    step="1"
                                                    className="form-control"
                                                    value={form.stockQuantity || "0"}
                                                    onChange={e => handleInputChange("stockQuantity", e.target.value)}
                                                    placeholder="100"
                                                />
                                            </div>
                                            <div className="form-field">
                                                <label>Low-stock Alert Threshold</label>
                                                <input
                                                    type="number"
                                                    step="1"
                                                    className="form-control"
                                                    value={form.lowStockThreshold || "5"}
                                                    onChange={e => handleInputChange("lowStockThreshold", e.target.value)}
                                                    placeholder="10"
                                                />
                                            </div>
                                            <div className="form-field col-span-2" style={{ justifyContent: "center" }}>
                                                <label className="form-checkbox-wrap">
                                                    <input
                                                        type="checkbox"
                                                        checked={Boolean(form.taxInclusive)}
                                                        onChange={e => handleInputChange("taxInclusive", e.target.checked)}
                                                    />
                                                    <span>Price is Tax-Inclusive</span>
                                                </label>
                                            </div>
                                        </div>
                                    </>
                                )}
                            </>
                        )}

                        <div className="form-actions-bar">
                            <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                                * Indicates mandatory fields for GST compliance
                            </span>
                            <div style={{ display: "flex", gap: "10px" }}>
                                <button type="button" className="secondary-button" onClick={() => setShowForm(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className="primary-button" disabled={saving}>
                                    {saving ? "Saving..." : isCustomer ? "Save Customer" : "Save Product"}
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            )}

            {/* List & Search Filter Bar */}
            <div className="table-card">
                <div className="table-header-card">
                    <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", flex: 1, alignItems: "center" }}>
                        <div className="search-input-wrapper" style={{ minWidth: "280px", maxWidth: "420px", flex: 1 }}>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="11" cy="11" r="8" />
                                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                            </svg>
                            <input
                                type="text"
                                className="search-input"
                                placeholder={isCustomer ? "Search by customer name, GSTIN, phone..." : "Search by product name, SKU, HSN..."}
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
                        <select
                            className="form-control"
                            value={filterType}
                            onChange={e => setFilterType(e.target.value)}
                            style={{ width: "170px", height: "38px" }}
                        >
                            {isCustomer ? (
                                <>
                                    <option value="ALL">All Customers</option>
                                    <option value="B2B">B2B Only</option>
                                    <option value="B2C">B2C Only</option>
                                    <option value="EXPORT">Export Only</option>
                                </>
                            ) : (
                                <>
                                    <option value="ALL">All Items</option>
                                    <option value="PRODUCT">Products Only</option>
                                    <option value="SERVICE">Services Only</option>
                                </>
                            )}
                        </select>
                    </div>
                    <span style={{ fontSize: "13px", color: "var(--text-muted)", fontWeight: 500 }}>
                        Showing {filteredItems.length} records
                    </span>
                </div>

                {/* Items Table */}
                <div className="table-container">
                    {loading ? (
                        <div style={{ padding: "40px", textAlign: "center" }}>
                            <div className="spinner" style={{ margin: "0 auto 12px" }} />
                            <p>Loading records...</p>
                        </div>
                    ) : filteredItems.length === 0 ? (
                        <div className="empty-state">
                            <div className="empty-state-icon">
                                <Icon type={isCustomer ? "customer" : "product"} size={26} />
                            </div>
                            <h3>No {isCustomer ? "customers" : "products"} found</h3>
                            <p>
                                {isCustomer
                                    ? "Add your client directory with GSTIN, billing address, and contact information."
                                    : "Add your product catalog items with HSN codes, unit pricing, and standard GST tax rates."}
                            </p>
                            <button
                                type="button"
                                className="primary-button"
                                onClick={() => setShowForm(true)}
                            >
                                <Icon type="plus" size={13} />
                                <span>{isCustomer ? "Add First Customer" : "Add First Product"}</span>
                            </button>
                        </div>
                    ) : isCustomer ? (
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Customer Name</th>
                                    <th>Type</th>
                                    <th>GSTIN</th>
                                    <th>State</th>
                                    <th>Phone</th>
                                    <th>Email</th>
                                    <th style={{ textAlign: "right" }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredItems.map(c => (
                                    <tr key={c.id}>
                                        <td>
                                            <button
                                                type="button"
                                                onClick={() => onViewCustomer ? onViewCustomer(c.id) : handleViewCustomerDetails(c.id)}
                                                style={{
                                                    background: "none",
                                                    border: "none",
                                                    padding: 0,
                                                    textAlign: "left",
                                                    fontWeight: "700",
                                                    color: "var(--primary)",
                                                    cursor: "pointer",
                                                    fontSize: "14px"
                                                }}
                                                title="Click to view full customer profile, orders & ledger"
                                            >
                                                {c.name}
                                            </button>
                                        </td>
                                        <td>
                                            <span className="badge" style={{ backgroundColor: "#e0f2fe", color: "#0369a1" }}>
                                                {c.customerType || "B2B"}
                                            </span>
                                        </td>
                                        <td style={{ letterSpacing: "0.2px", fontVariantNumeric: "tabular-nums" }}>
                                            {c.gstin || <span style={{ color: "var(--text-muted)" }}>Unregistered</span>}
                                        </td>
                                        <td>{c.state}</td>
                                        <td>{c.phone || "—"}</td>
                                        <td>{c.email || "—"}</td>
                                        <td style={{ textAlign: "right" }}>
                                            <div style={{ display: "inline-flex", gap: "8px", justifyContent: "flex-end" }}>
                                                <button
                                                    type="button"
                                                    className="action-btn-sm"
                                                    onClick={() => onViewCustomer ? onViewCustomer(c.id) : handleViewCustomerDetails(c.id)}
                                                    title="View full customer profile, orders and ledger"
                                                >
                                                    View Profile
                                                </button>
                                                <button
                                                    type="button"
                                                    className="action-btn-sm"
                                                    style={{ color: "var(--danger)" }}
                                                    onClick={() => handleDelete(c.id, c.name)}
                                                >
                                                    Delete
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : (
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Item Name</th>
                                    <th>Type</th>
                                    <th>SKU / HSN</th>
                                    <th style={{ textAlign: "right" }}>Unit Price</th>
                                    <th style={{ textAlign: "center" }}>GST %</th>
                                    <th>Stock</th>
                                    <th style={{ textAlign: "right" }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredItems.map(p => {
                                    const isLowStock = p.productType?.toUpperCase() === "PRODUCT"
                                        && Number(p.stockQuantity || 0) <= Number(p.lowStockThreshold || 5);

                                    return (
                                        <tr key={p.id}>
                                            <td style={{ fontWeight: "600" }}>{p.name}</td>
                                            <td>
                                                <span className="badge" style={{
                                                    background: p.productType === "SERVICE" ? "rgba(99,102,241,0.12)" : "rgba(16,185,129,0.12)",
                                                    color: p.productType === "SERVICE" ? "#4338ca" : "#065f46"
                                                }}>
                                                    {p.productType || "PRODUCT"}
                                                </span>
                                            </td>
                                            <td style={{ letterSpacing: "0.2px" }}>
                                                {p.sku ? `${p.sku} / ` : ""}{p.hsnCode || "—"}
                                            </td>
                                            <td className="table-num" style={{ textAlign: "right" }}>
                                                {fmt(p.price)}
                                                {p.taxInclusive && (
                                                    <span className="badge" style={{ display: "inline-block", fontSize: "10px", marginLeft: "6px", padding: "1px 5px", background: "rgba(59,130,246,0.12)", color: "#1d4ed8" }}>
                                                        Incl.
                                                    </span>
                                                )}
                                            </td>
                                            <td style={{ textAlign: "center", fontWeight: 600 }}>
                                                {p.gstRate}%
                                            </td>
                                            <td>
                                                {p.productType === "SERVICE" ? (
                                                    <span style={{ color: "var(--text-muted)" }}>N/A (Service)</span>
                                                ) : (
                                                    <span style={{
                                                        color: isLowStock ? "#ef4444" : "var(--text-primary)",
                                                        fontWeight: isLowStock ? 700 : 500
                                                    }}>
                                                        {p.stockQuantity || 0} {p.unit || ""}
                                                        {isLowStock && (
                                                            <span className="status-badge status-draft" style={{ marginLeft: "6px", fontSize: "10px" }}>
                                                                Low Stock
                                                            </span>
                                                        )}
                                                    </span>
                                                )}
                                            </td>
                                            <td style={{ textAlign: "right" }}>
                                                <button
                                                    type="button"
                                                    className="action-btn-sm"
                                                    style={{ color: "var(--danger)" }}
                                                    onClick={() => handleDelete(p.id, p.name)}
                                                >
                                                    Delete
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>

            {/* Customer Details Drawer / Modal */}
            {selectedCustomerDetails && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "750px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <div>
                                <h3 style={{ margin: 0, fontSize: "18px" }}>{selectedCustomerDetails.customer.name}</h3>
                                <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                                    {selectedCustomerDetails.customer.customerType || "B2B"} | GSTIN: {selectedCustomerDetails.customer.gstin || "Unregistered"} | {selectedCustomerDetails.customer.state}
                                </span>
                            </div>
                            <button type="button" className="modal-close-btn" onClick={() => setSelectedCustomerDetails(null)}><Icon type="close" size={14} /></button>
                        </div>

                        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "10px", marginBottom: "20px" }}>
                            <div style={{ background: "var(--bg-subtle, #f8fafc)", padding: "10px", borderRadius: "6px" }}>
                                <div style={{ fontSize: "11px", color: "var(--text-muted)" }}>Total Invoiced</div>
                                <div style={{ fontSize: "15px", fontWeight: "700" }}>{fmt(selectedCustomerDetails.totalInvoiced)}</div>
                            </div>
                            <div style={{ background: "rgba(16, 185, 129, 0.1)", padding: "10px", borderRadius: "6px" }}>
                                <div style={{ fontSize: "11px", color: "#10b981", fontWeight: "600" }}>Total Paid</div>
                                <div style={{ fontSize: "15px", fontWeight: "700", color: "#10b981" }}>{fmt(selectedCustomerDetails.totalPaid)}</div>
                            </div>
                            <div style={{ background: "rgba(239, 68, 68, 0.1)", padding: "10px", borderRadius: "6px" }}>
                                <div style={{ fontSize: "11px", color: "#ef4444", fontWeight: "600" }}>Outstanding</div>
                                <div style={{ fontSize: "15px", fontWeight: "700", color: "#ef4444" }}>{fmt(selectedCustomerDetails.outstandingBalance)}</div>
                            </div>
                            <div style={{ background: "rgba(245, 158, 11, 0.1)", padding: "10px", borderRadius: "6px" }}>
                                <div style={{ fontSize: "11px", color: "#b45309", fontWeight: "600" }}>Overdue Amount</div>
                                <div style={{ fontSize: "15px", fontWeight: "700", color: "#b45309" }}>{fmt(selectedCustomerDetails.overdueAmount)}</div>
                            </div>
                        </div>

                        <h4 style={{ margin: "0 0 10px", fontSize: "14px" }}>
                            Invoice History ({selectedCustomerDetails.invoices ? selectedCustomerDetails.invoices.length : 0})
                        </h4>
                        {(!selectedCustomerDetails.invoices || selectedCustomerDetails.invoices.length === 0) ? (
                            <p style={{ color: "var(--text-muted)", fontSize: "13px" }}>No invoices for this customer yet.</p>
                        ) : (
                            <div className="table-container">
                                <table className="table" style={{ fontSize: "12px" }}>
                                    <thead>
                                        <tr>
                                            <th>Date</th>
                                            <th>Invoice #</th>
                                            <th style={{ textAlign: "right" }}>Grand Total</th>
                                            <th style={{ textAlign: "right" }}>Paid</th>
                                            <th style={{ textAlign: "right" }}>Balance</th>
                                            <th>Status</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {selectedCustomerDetails.invoices.map(inv => (
                                            <tr key={inv.id}>
                                                <td>{inv.invoiceDate}</td>
                                                <td style={{ fontWeight: "600", color: "var(--primary)" }}>{inv.invoiceNumber}</td>
                                                <td className="table-num" style={{ textAlign: "right" }}>{fmt(inv.grandTotal)}</td>
                                                <td className="table-num" style={{ textAlign: "right", color: "var(--success)" }}>{fmt(inv.paidAmount)}</td>
                                                <td className="table-num" style={{ textAlign: "right", color: "var(--danger)", fontWeight: 600 }}>{fmt(inv.balanceAmount)}</td>
                                                <td><span className={`status-badge status-${(inv.status || "DRAFT").toLowerCase()}`}>{inv.status}</span></td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "20px" }}>
                            {onViewCustomer ? (
                                <button
                                    type="button"
                                    className="secondary-button"
                                    onClick={() => {
                                        const id = selectedCustomerDetails.customer.id;
                                        setSelectedCustomerDetails(null);
                                        onViewCustomer(id);
                                    }}
                                >
                                    Open Full 360° Profile Page →
                                </button>
                            ) : <div />}
                            <button type="button" className="primary-button" onClick={() => setSelectedCustomerDetails(null)}>Close</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}