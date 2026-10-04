import { useEffect, useState } from "react";
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

export default function ManageData({ kind }) {
    const isCustomer = kind === "customers";

    const [items, setItems] = useState([]);
    const [form, setForm] = useState({});
    const [search, setSearch] = useState("");
    const [filterType, setFilterType] = useState("ALL");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [saving, setSaving] = useState(false);
    const [loading, setLoading] = useState(true);

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

    const handleDelete = async (id) => {
        const itemType = isCustomer ? "customer" : "product";
        if (!window.confirm(`Are you sure you want to delete this ${itemType}?`)) {
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
                            ? "Manage customer GSTINs, billing addresses, credit limits, and balances"
                            : "Define catalog items, HSN/SAC codes, standard GST rates, and inventory stock"}
                    </p>
                </div>
            </div>

            {/* Error & Success Banners */}
            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.15)", color: "var(--danger)", borderRadius: "8px" }}>
                    {error}
                </div>
            )}
            {success && (
                <div style={{ padding: "12px 16px", background: "rgba(16, 185, 129, 0.15)", color: "#065f46", borderRadius: "8px", fontWeight: "600" }}>
                    ✓ {success}
                </div>
            )}

            {/* Creation Form Card */}
            <div className="card" style={{ padding: "24px" }}>
                <h3 style={{ margin: "0 0 16px", fontSize: "18px", fontWeight: "600" }}>
                    {isCustomer ? "+ Add New Customer" : "+ Add New Product / Service"}
                </h3>

                <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                    {/* CUSTOMER FORM FIELDS */}
                    {isCustomer && (
                        <>
                            <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: "16px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Customer / Company Name *
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={form.name || ""}
                                        onChange={e => handleInputChange("name", e.target.value)}
                                        placeholder="e.g. Apex Enterprises Pvt Ltd"
                                        required
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Customer Type *
                                    </label>
                                    <select
                                        className="input"
                                        value={form.customerType || "B2B"}
                                        onChange={e => handleInputChange("customerType", e.target.value)}
                                    >
                                        <option value="B2B">B2B (Registered Business)</option>
                                        <option value="B2C">B2C (Consumer)</option>
                                        <option value="EXPORT">EXPORT</option>
                                    </select>
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        GSTIN (Required for B2B)
                                    </label>
                                    <input
                                        type="text"
                                        maxLength="15"
                                        className="input"
                                        value={form.gstin || ""}
                                        onChange={e => handleInputChange("gstin", e.target.value.toUpperCase())}
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
                                        value={form.pan || ""}
                                        onChange={e => handleInputChange("pan", e.target.value.toUpperCase())}
                                        placeholder="10-digit PAN"
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Phone Number (for WhatsApp reminder)
                                    </label>
                                    <input
                                        type="tel"
                                        className="input"
                                        value={form.phone || ""}
                                        onChange={e => handleInputChange("phone", e.target.value)}
                                        placeholder="e.g. 9876543210"
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Email Address (for Email reminder)
                                    </label>
                                    <input
                                        type="email"
                                        className="input"
                                        value={form.email || ""}
                                        onChange={e => handleInputChange("email", e.target.value)}
                                        placeholder="billing@customer.com"
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1fr", gap: "16px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        State *
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={form.state || ""}
                                        onChange={e => handleInputChange("state", e.target.value)}
                                        required
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        State Code
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={form.stateCode || ""}
                                        onChange={e => handleInputChange("stateCode", e.target.value)}
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Credit Terms (Days)
                                    </label>
                                    <input
                                        type="number"
                                        className="input"
                                        value={form.paymentTerms || "15"}
                                        onChange={e => handleInputChange("paymentTerms", e.target.value)}
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Billing Address
                                    </label>
                                    <textarea
                                        className="input"
                                        rows="2"
                                        value={form.billingAddress || ""}
                                        onChange={e => handleInputChange("billingAddress", e.target.value)}
                                        placeholder="Official Billing Address"
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Shipping Address (Optional)
                                    </label>
                                    <textarea
                                        className="input"
                                        rows="2"
                                        value={form.shippingAddress || ""}
                                        onChange={e => handleInputChange("shippingAddress", e.target.value)}
                                        placeholder="Delivery destination address"
                                    />
                                </div>
                            </div>
                        </>
                    )}

                    {/* PRODUCT FORM FIELDS */}
                    {!isCustomer && (
                        <>
                            <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr 1fr", gap: "16px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Item / Product Name *
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={form.name || ""}
                                        onChange={e => handleInputChange("name", e.target.value)}
                                        placeholder="e.g. Dell Monitor 27-inch or IT Consulting"
                                        required
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Type *
                                    </label>
                                    <select
                                        className="input"
                                        value={form.productType || "PRODUCT"}
                                        onChange={e => handleInputChange("productType", e.target.value)}
                                    >
                                        <option value="PRODUCT">Goods (Physical Product)</option>
                                        <option value="SERVICE">Service (Non-stock)</option>
                                    </select>
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        SKU / Part Code
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={form.sku || ""}
                                        onChange={e => handleInputChange("sku", e.target.value)}
                                        placeholder="e.g. DELL-27-4K"
                                    />
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr", gap: "16px" }}>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        HSN / SAC Code
                                    </label>
                                    <input
                                        type="text"
                                        className="input"
                                        value={form.hsnCode || ""}
                                        onChange={e => handleInputChange("hsnCode", e.target.value)}
                                        placeholder="e.g. 8471 or 9983"
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Unit Price (₹) *
                                    </label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        className="input"
                                        value={form.price || ""}
                                        onChange={e => handleInputChange("price", e.target.value)}
                                        placeholder="0.00"
                                        required
                                    />
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Standard GST Rate *
                                    </label>
                                    <select
                                        className="input"
                                        value={form.gstRate || 18}
                                        onChange={e => handleInputChange("gstRate", Number(e.target.value))}
                                    >
                                        {GST_RATES.map(r => (
                                            <option key={r} value={r}>{r}% GST</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                        Unit of Measurement
                                    </label>
                                    <select
                                        className="input"
                                        value={form.unit || "PCS"}
                                        onChange={e => handleInputChange("unit", e.target.value)}
                                    >
                                        {COMMON_UNITS.map(u => (
                                            <option key={u} value={u}>{u}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            {form.productType === "PRODUCT" && (
                                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "16px" }}>
                                    <div>
                                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                            Initial Stock Quantity
                                        </label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            className="input"
                                            value={form.stockQuantity || "0"}
                                            onChange={e => handleInputChange("stockQuantity", e.target.value)}
                                        />
                                    </div>
                                    <div>
                                        <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "4px" }}>
                                            Low-stock Alert Threshold
                                        </label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            className="input"
                                            value={form.lowStockThreshold || "5"}
                                            onChange={e => handleInputChange("lowStockThreshold", e.target.value)}
                                        />
                                    </div>
                                    <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "24px" }}>
                                        <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "13px", cursor: "pointer" }}>
                                            <input
                                                type="checkbox"
                                                checked={Boolean(form.taxInclusive)}
                                                onChange={e => handleInputChange("taxInclusive", e.target.checked)}
                                            />
                                            Price is Tax-Inclusive
                                        </label>
                                    </div>
                                </div>
                            )}
                        </>
                    )}

                    <div style={{ display: "flex", justifyContent: "flex-end" }}>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? "Saving..." : isCustomer ? "Save Customer" : "Save Product"}
                        </button>
                    </div>
                </form>
            </div>

            {/* List & Search Filter */}
            <div className="card" style={{ padding: "16px 20px" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "16px" }}>
                    <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", flex: 1, alignItems: "center" }}>
                        <input
                            type="text"
                            className="search-input"
                            placeholder={isCustomer ? "Search by customer name, GSTIN, phone..." : "Search by product name, SKU, HSN..."}
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            style={{ minWidth: "320px" }}
                        />
                        <select
                            className="form-control"
                            value={filterType}
                            onChange={e => setFilterType(e.target.value)}
                            style={{ width: "160px" }}
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
                    <span style={{ fontSize: "13px", color: "var(--muted)" }}>
                        Showing {filteredItems.length} records
                    </span>
                </div>
            </div>

            {/* Items Table */}
            <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center" }}>
                        <div className="spinner" style={{ margin: "0 auto 12px" }} />
                        <p>Loading records...</p>
                    </div>
                ) : filteredItems.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--muted)" }}>
                        <p>No records found matching filters.</p>
                    </div>
                ) : isCustomer ? (
                    <table className="table" style={{ width: "100%", margin: 0 }}>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Type</th>
                                <th>GSTIN</th>
                                <th>State</th>
                                <th>Phone</th>
                                <th>Email</th>
                                <th style={{ textAlign: "center" }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredItems.map(c => (
                                <tr key={c.id}>
                                    <td style={{ fontWeight: "600" }}>{c.name}</td>
                                    <td><span className="badge">{c.customerType || "B2B"}</span></td>
                                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{c.gstin || "Unregistered"}</td>
                                    <td>{c.state}</td>
                                    <td>{c.phone || "-"}</td>
                                    <td>{c.email || "-"}</td>
                                    <td style={{ textAlign: "center" }}>
                                        <div style={{ display: "inline-flex", gap: "6px" }}>
                                            <button
                                                className="btn btn-secondary btn-sm"
                                                style={{ padding: "3px 8px", fontSize: "11px" }}
                                                onClick={() => handleViewCustomerDetails(c.id)}
                                            >
                                                Ledger & History
                                            </button>
                                            <button
                                                className="btn btn-danger btn-sm"
                                                style={{ padding: "3px 8px", fontSize: "11px" }}
                                                onClick={() => handleDelete(c.id)}
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
                    <table className="table" style={{ width: "100%", margin: 0 }}>
                        <thead>
                            <tr>
                                <th>Item Name</th>
                                <th>Type</th>
                                <th>SKU / HSN</th>
                                <th>Unit Price</th>
                                <th>GST %</th>
                                <th>Stock</th>
                                <th style={{ textAlign: "center" }}>Actions</th>
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
                                                background: p.productType === "SERVICE" ? "rgba(99,102,241,0.15)" : "rgba(16,185,129,0.15)",
                                                color: p.productType === "SERVICE" ? "#4338ca" : "#065f46"
                                            }}>
                                                {p.productType || "PRODUCT"}
                                            </span>
                                        </td>
                                        <td style={{ fontFamily: "monospace", fontSize: "12px" }}>
                                            {p.sku ? `${p.sku} / ` : ""}{p.hsnCode || "-"}
                                        </td>
                                        <td style={{ fontWeight: "600" }}>{fmt(p.price)}</td>
                                        <td>{p.gstRate}%</td>
                                        <td>
                                            {p.productType === "SERVICE" ? (
                                                <span style={{ color: "var(--muted)" }}>N/A (Service)</span>
                                            ) : (
                                                <span style={{
                                                    color: isLowStock ? "#ef4444" : "inherit",
                                                    fontWeight: isLowStock ? "700" : "normal"
                                                }}>
                                                    {p.stockQuantity || 0} {p.unit || ""}
                                                    {isLowStock && " ⚠️ Low"}
                                                </span>
                                            )}
                                        </td>
                                        <td style={{ textAlign: "center" }}>
                                            <button
                                                className="btn btn-danger btn-sm"
                                                style={{ padding: "3px 8px", fontSize: "11px" }}
                                                onClick={() => handleDelete(p.id)}
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

            {/* Customer Details Drawer / Modal */}
            {selectedCustomerDetails && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "750px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <div>
                                <h3 style={{ margin: 0, fontSize: "18px" }}>{selectedCustomerDetails.customer.name}</h3>
                                <span style={{ fontSize: "12px", color: "var(--muted)" }}>
                                    {selectedCustomerDetails.customer.customerType || "B2B"} | GSTIN: {selectedCustomerDetails.customer.gstin || "Unregistered"} | {selectedCustomerDetails.customer.state}
                                </span>
                            </div>
                            <button className="btn-close" onClick={() => setSelectedCustomerDetails(null)}>✕</button>
                        </div>

                        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "10px", marginBottom: "20px" }}>
                            <div style={{ background: "var(--bg-subtle, #f8fafc)", padding: "10px", borderRadius: "6px" }}>
                                <div style={{ fontSize: "11px", color: "var(--muted)" }}>Total Invoiced</div>
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
                            <p style={{ color: "var(--muted)", fontSize: "13px" }}>No invoices for this customer yet.</p>
                        ) : (
                            <table className="table" style={{ width: "100%", fontSize: "12px" }}>
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
                                            <td style={{ fontWeight: "600" }}>{inv.invoiceNumber}</td>
                                            <td style={{ textAlign: "right" }}>{fmt(inv.grandTotal)}</td>
                                            <td style={{ textAlign: "right", color: "#10b981" }}>{fmt(inv.paidAmount)}</td>
                                            <td style={{ textAlign: "right", color: "#ef4444", fontWeight: "600" }}>{fmt(inv.balanceAmount)}</td>
                                            <td><span className={`badge badge-${(inv.status || "DRAFT").toLowerCase()}`}>{inv.status}</span></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}

                        <div style={{ textAlign: "right", marginTop: "16px" }}>
                            <button className="btn btn-secondary" onClick={() => setSelectedCustomerDetails(null)}>Close</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}