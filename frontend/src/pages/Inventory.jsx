import React, { useEffect, useState } from "react";
import Icon from "../components/Icon";
import {
    getInventoryMovements,
    getProducts,
    getWarehouses,
    getWarehouseStock,
    createWarehouse,
    recordOpeningStock,
    transferWarehouseStock,
    adjustWarehouseStock
} from "../services/api";
import { Skeleton, SkeletonKpi, SkeletonTable } from "../components/ui/Skeleton";
import EmptyState from "../components/ui/EmptyState";
import ErrorState from "../components/ui/ErrorState";
import { useToast } from "../context/ToastContext";

export default function Inventory({ defaultTab = "stock", onTabChange }) {
    const toast = useToast();
    const [activeTab, setActiveTab] = useState(defaultTab); // "stock" | "movements" | "warehouses"
    const [warehouses, setWarehouses] = useState([]);
    const [selectedWarehouseId, setSelectedWarehouseId] = useState("");
    const [warehouseStocks, setWarehouseStocks] = useState([]);
    const [movements, setMovements] = useState([]);
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [stockLoading, setStockLoading] = useState(false);
    const [movementsLoading, setMovementsLoading] = useState(false);
    const [error, setError] = useState("");
    const [movementsError, setMovementsError] = useState("");
    const [search, setSearch] = useState("");
    const [movementFilter, setMovementFilter] = useState("ALL");
    const [movementWarehouseFilter, setMovementWarehouseFilter] = useState("ALL");

    // Adjust Stock Modal State
    const [showAdjustModal, setShowAdjustModal] = useState(false);
    const [adjustWarehouseId, setAdjustWarehouseId] = useState("");
    const [adjustProductId, setAdjustProductId] = useState("");
    const [adjustQty, setAdjustQty] = useState("");
    const [adjustReason, setAdjustReason] = useState("");
    const [adjustSubmitting, setAdjustSubmitting] = useState(false);
    const [adjustError, setAdjustError] = useState("");

    // Transfer Stock Modal State
    const [showTransferModal, setShowTransferModal] = useState(false);
    const [transferSourceId, setTransferSourceId] = useState("");
    const [transferDestId, setTransferDestId] = useState("");
    const [transferProductId, setTransferProductId] = useState("");
    const [transferQty, setTransferQty] = useState("");
    const [transferNotes, setTransferNotes] = useState("");
    const [transferSubmitting, setTransferSubmitting] = useState(false);
    const [transferError, setTransferError] = useState("");

    // Opening Stock Modal State
    const [showOpeningModal, setShowOpeningModal] = useState(false);
    const [openingWarehouseId, setOpeningWarehouseId] = useState("");
    const [openingProductId, setOpeningProductId] = useState("");
    const [openingQty, setOpeningQty] = useState("");
    const [openingRef, setOpeningRef] = useState("");
    const [openingNotes, setOpeningNotes] = useState("");
    const [openingSubmitting, setOpeningSubmitting] = useState(false);
    const [openingError, setOpeningError] = useState("");

    // Create Warehouse Modal State
    const [showWarehouseModal, setShowWarehouseModal] = useState(false);
    const [whName, setWhName] = useState("");
    const [whCode, setWhCode] = useState("");
    const [whAddress, setWhAddress] = useState("");
    const [whCity, setWhCity] = useState("");
    const [whState, setWhState] = useState("");
    const [whPincode, setWhPincode] = useState("");
    const [whIsDefault, setWhIsDefault] = useState(false);
    const [whSubmitting, setWhSubmitting] = useState(false);
    const [whError, setWhError] = useState("");

    useEffect(() => {
        if (defaultTab && defaultTab !== activeTab) {
            setActiveTab(defaultTab);
        }
    }, [defaultTab]);

    const switchTab = (tab) => {
        setActiveTab(tab);
        if (onTabChange) {
            onTabChange(tab);
        }
    };

    useEffect(() => {
        loadInitialData();
    }, []);

    useEffect(() => {
        const handleBizSwitch = () => {
            setError("");
            setMovementsError("");
            loadInitialData();
        };
        window.addEventListener("business-switched", handleBizSwitch);
        return () => window.removeEventListener("business-switched", handleBizSwitch);
    }, []);

    useEffect(() => {
        if (selectedWarehouseId) {
            loadStockForWarehouse(selectedWarehouseId);
        }
    }, [selectedWarehouseId]);

    async function loadMovements() {
        try {
            setMovementsLoading(true);
            setMovementsError("");
            const movs = await getInventoryMovements();
            setMovements(Array.isArray(movs) ? movs : []);
        } catch (err) {
            console.error("Failed to load inventory movements:", err);
            setMovementsError(err.message || "Failed to load stock audit trail records");
            setMovements([]);
        } finally {
            setMovementsLoading(false);
        }
    }

    async function loadInitialData() {
        try {
            setLoading(true);
            setError("");

            const [whResult, prodResult] = await Promise.allSettled([
                getWarehouses(),
                getProducts()
            ]);

            if (whResult.status === "rejected" && prodResult.status === "rejected") {
                throw new Error(whResult.reason?.message || "Failed to connect to inventory services");
            }

            const whList = whResult.status === "fulfilled" && Array.isArray(whResult.value) ? whResult.value : [];
            const prods = prodResult.status === "fulfilled" && Array.isArray(prodResult.value) ? prodResult.value : [];

            setWarehouses(whList);
            setProducts(prods.filter(p => p.productType?.toUpperCase() !== "SERVICE"));

            if (whList.length > 0) {
                const defaultWh = whList.find(w => w.isDefault) || whList[0];
                setSelectedWarehouseId(String(defaultWh.id));
            }
        } catch (err) {
            setError(err.message || "Failed to load inventory data");
        } finally {
            setLoading(false);
        }

        // Independently load movements so an audit trail failure never crashes the inventory page
        loadMovements();
    }

    async function loadStockForWarehouse(whId) {
        if (!whId) return;
        try {
            setStockLoading(true);
            const stocks = await getWarehouseStock(whId);
            setWarehouseStocks(Array.isArray(stocks) ? stocks : []);
        } catch (err) {
            console.error("Failed to load warehouse stock:", err);
            setWarehouseStocks([]);
        } finally {
            setStockLoading(false);
        }
    }

    // Adjust Stock
    function openAdjust(prod = null, whId = null) {
        setAdjustError("");
        setAdjustWarehouseId(whId ? String(whId) : (selectedWarehouseId || (warehouses[0]?.id ? String(warehouses[0].id) : "")));
        if (prod) {
            setAdjustProductId(String(prod.id));
            setAdjustQty(prod.quantityOnHand ?? prod.stockQuantity ?? "0");
        } else if (products.length > 0) {
            setAdjustProductId(String(products[0].id));
            setAdjustQty(products[0].stockQuantity || "0");
        }
        setAdjustReason("Physical audit count adjustment");
        setShowAdjustModal(true);
    }

    async function handleAdjustSubmit(e) {
        e.preventDefault();
        if (!adjustWarehouseId || !adjustProductId) return;

        try {
            setAdjustSubmitting(true);
            setAdjustError("");
            await adjustWarehouseStock({
                warehouseId: Number(adjustWarehouseId),
                productId: Number(adjustProductId),
                newQuantity: Number(adjustQty),
                reason: adjustReason || "Manual adjustment"
            });
            setShowAdjustModal(false);
            toast.success("Stock quantity adjusted successfully!");
            await Promise.all([
                loadStockForWarehouse(selectedWarehouseId),
                loadMovements()
            ]);
        } catch (err) {
            setAdjustError(err.message || "Failed to adjust stock");
        } finally {
            setAdjustSubmitting(false);
        }
    }

    // Transfer Stock
    function openTransfer() {
        setTransferError("");
        const src = selectedWarehouseId || (warehouses[0]?.id ? String(warehouses[0].id) : "");
        setTransferSourceId(src);
        const dest = warehouses.find(w => String(w.id) !== src);
        setTransferDestId(dest ? String(dest.id) : "");
        if (products.length > 0) {
            setTransferProductId(String(products[0].id));
        }
        setTransferQty("1");
        setTransferNotes("");
        setShowTransferModal(true);
    }

    async function handleTransferSubmit(e) {
        e.preventDefault();
        if (!transferSourceId || !transferDestId || !transferProductId) return;
        if (transferSourceId === transferDestId) {
            setTransferError("Source and destination warehouses cannot be the same");
            return;
        }

        try {
            setTransferSubmitting(true);
            setTransferError("");
            await transferWarehouseStock({
                sourceWarehouseId: Number(transferSourceId),
                destinationWarehouseId: Number(transferDestId),
                productId: Number(transferProductId),
                quantity: Number(transferQty),
                notes: transferNotes || "Inter-warehouse transfer"
            });
            setShowTransferModal(false);
            toast.success("Stock transferred successfully between warehouses!");
            await Promise.all([
                loadStockForWarehouse(selectedWarehouseId),
                loadMovements()
            ]);
        } catch (err) {
            setTransferError(err.message || "Failed to transfer stock");
        } finally {
            setTransferSubmitting(false);
        }
    }

    // Opening Stock
    function openOpening() {
        setOpeningError("");
        setOpeningWarehouseId(selectedWarehouseId || (warehouses[0]?.id ? String(warehouses[0].id) : ""));
        if (products.length > 0) {
            setOpeningProductId(String(products[0].id));
        }
        setOpeningQty("0");
        setOpeningRef("OPENING-FY");
        setOpeningNotes("Initial opening stock entry");
        setShowOpeningModal(true);
    }

    async function handleOpeningSubmit(e) {
        e.preventDefault();
        if (!openingWarehouseId || !openingProductId) return;

        try {
            setOpeningSubmitting(true);
            setOpeningError("");
            await recordOpeningStock({
                warehouseId: Number(openingWarehouseId),
                productId: Number(openingProductId),
                quantity: Number(openingQty),
                reference: openingRef || "OPENING",
                notes: openingNotes || "Opening stock balance"
            });
            setShowOpeningModal(false);
            toast.success("Opening stock recorded successfully!");
            await Promise.all([
                loadStockForWarehouse(selectedWarehouseId),
                loadMovements()
            ]);
        } catch (err) {
            setOpeningError(err.message || "Failed to record opening stock");
        } finally {
            setOpeningSubmitting(false);
        }
    }

    // Create Warehouse
    function openCreateWarehouse() {
        setWhError("");
        setWhName("");
        setWhCode("");
        setWhAddress("");
        setWhCity("");
        setWhState("");
        setWhPincode("");
        setWhIsDefault(false);
        setShowWarehouseModal(true);
    }

    async function handleWarehouseSubmit(e) {
        e.preventDefault();
        if (!whName.trim() || !whCode.trim()) {
            setWhError("Warehouse Name and Unique Code are required");
            return;
        }

        try {
            setWhSubmitting(true);
            setWhError("");
            const created = await createWarehouse({
                name: whName.trim(),
                code: whCode.trim().toUpperCase(),
                address: whAddress.trim() || null,
                city: whCity.trim() || null,
                state: whState.trim() || null,
                pincode: whPincode.trim() || null,
                isDefault: whIsDefault
            });
            setShowWarehouseModal(false);
            toast.success(`Warehouse "${created.name}" (${created.code}) created successfully!`);
            const updatedList = await getWarehouses();
            setWarehouses(Array.isArray(updatedList) ? updatedList : []);
            if (created.id) {
                setSelectedWarehouseId(String(created.id));
            }
        } catch (err) {
            setWhError(err.message || "Failed to create warehouse");
        } finally {
            setWhSubmitting(false);
        }
    }

    const currentWarehouse = warehouses.find(w => String(w.id) === String(selectedWarehouseId));

    const filteredMovements = movements.filter(m => {
        const q = search.toLowerCase();
        const prodName = m.product?.name?.toLowerCase() || "";
        const prodSku = m.product?.sku?.toLowerCase() || "";
        const refNum = m.referenceNumber?.toLowerCase() || "";
        const notes = m.notes?.toLowerCase() || "";
        const whName = m.warehouse?.name?.toLowerCase() || "";
        const whCode = m.warehouse?.code?.toLowerCase() || "";
        const targetWh = m.targetWarehouse?.name?.toLowerCase() || "";
        const targetWhCode = m.targetWarehouse?.code?.toLowerCase() || "";

        const matchesSearch = !q ||
            prodName.includes(q) ||
            prodSku.includes(q) ||
            refNum.includes(q) ||
            notes.includes(q) ||
            whName.includes(q) ||
            whCode.includes(q) ||
            targetWh.includes(q) ||
            targetWhCode.includes(q);

        if (!matchesSearch) return false;
        if (movementFilter !== "ALL" && m.movementType !== movementFilter) return false;

        if (movementWarehouseFilter !== "ALL") {
            const matchesWh = String(m.warehouse?.id) === String(movementWarehouseFilter) ||
                              String(m.targetWarehouse?.id) === String(movementWarehouseFilter);
            if (!matchesWh) return false;
        }

        return true;
    });

    const filteredStock = warehouseStocks.filter(ws => {
        const q = search.toLowerCase();
        const name = ws.product?.name?.toLowerCase() || "";
        const sku = ws.product?.sku?.toLowerCase() || "";
        const hsn = ws.product?.hsnSac?.code?.toLowerCase() || ws.product?.hsnCode?.toLowerCase() || "";
        return !q || name.includes(q) || sku.includes(q) || hsn.includes(q);
    });

    // Summary stats for selected warehouse
    const totalOnHand = warehouseStocks.reduce((sum, ws) => sum + Number(ws.quantityOnHand || 0), 0);
    const totalReserved = warehouseStocks.reduce((sum, ws) => sum + Number(ws.quantityReserved || 0), 0);
    const totalAvailable = warehouseStocks.reduce((sum, ws) => sum + Number(ws.availableQuantity || ws.quantityOnHand || 0), 0);

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            {/* Page Header */}
            <div className="page-heading">
                <div>
                    <span className="eyebrow">MULTI-WAREHOUSE INVENTORY & AUDIT</span>
                    <h1>Warehouse & Inventory Management</h1>
                    <p>Track real-time stock balances per warehouse, inter-warehouse transfers, and immutable audit logs</p>
                </div>
                <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                    <button type="button" className="action-btn-sm" onClick={openOpening} title="Record Opening Stock Balance">
                        <Icon type="plus" size={13} />
                        <span>Opening Stock</span>
                    </button>
                    <button type="button" className="action-btn-sm" onClick={openTransfer} disabled={warehouses.length < 2} title={warehouses.length < 2 ? "Requires at least 2 warehouses" : "Transfer stock between warehouses"}>
                        <Icon type="truck" size={13} />
                        <span>Transfer Stock</span>
                    </button>
                    <button type="button" className="action-btn-sm" onClick={() => openAdjust()} disabled={products.length === 0} title="Perform physical stock audit count adjustment">
                        <Icon type="settings" size={13} />
                        <span>Adjust Stock</span>
                    </button>
                    <button type="button" className="primary-button" onClick={openCreateWarehouse} title="Add a new warehouse facility">
                        <Icon type="plus" size={13} />
                        <span>New Warehouse</span>
                    </button>
                </div>
            </div>

            {error && (
                <ErrorState
                    title="Unable to load inventory data"
                    message={error}
                    onRetry={loadInitialData}
                    retryLabel="Retry Loading Inventory"
                />
            )}

            {/* Warehouse Selector & Metric Cards */}
            {loading ? (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                    <div className="card" style={{ padding: "16px" }}>
                        <Skeleton width="110px" height="12px" style={{ marginBottom: "12px" }} />
                        <Skeleton width="100%" height="38px" borderRadius="var(--radius-sm)" style={{ marginBottom: "8px" }} />
                        <Skeleton width="140px" height="12px" />
                    </div>
                    <SkeletonKpi />
                    <SkeletonKpi />
                    <SkeletonKpi />
                </div>
            ) : (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                    {/* Active Warehouse Dropdown Card */}
                    <div className="card" style={{ padding: "16px" }}>
                        <div style={{ fontSize: "11px", fontWeight: 600, color: "var(--text-muted)", textTransform: "uppercase", marginBottom: "8px" }}>
                            Active Warehouse
                        </div>
                        <select
                            className="form-control"
                            value={selectedWarehouseId}
                            onChange={(e) => setSelectedWarehouseId(e.target.value)}
                            style={{ fontWeight: 600, fontSize: "14px" }}
                        >
                            {warehouses.map(w => (
                                <option key={w.id} value={w.id}>
                                    {w.name} ({w.code}){w.isDefault ? " (Default)" : ""}
                                </option>
                            ))}
                        </select>
                        {currentWarehouse && (
                            <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "6px" }}>
                                {currentWarehouse.city ? `${currentWarehouse.city}, ` : ""}{currentWarehouse.state || "Active facility"}
                            </div>
                        )}
                    </div>

                    {/* Metric: Stock On Hand */}
                    <div className="card" style={{ padding: "16px" }}>
                        <div style={{ fontSize: "11px", fontWeight: 600, color: "var(--text-muted)", textTransform: "uppercase", marginBottom: "4px" }}>
                            Stock On Hand (Physical)
                        </div>
                        <div style={{ fontSize: "24px", fontWeight: 700, color: "var(--text-primary)" }}>
                            {totalOnHand.toLocaleString("en-IN")}
                        </div>
                        <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "4px" }}>
                            Across {warehouseStocks.length} tracked SKUs
                        </div>
                    </div>

                    {/* Metric: Reserved */}
                    <div className="card" style={{ padding: "16px" }}>
                        <div style={{ fontSize: "11px", fontWeight: 600, color: "var(--text-muted)", textTransform: "uppercase", marginBottom: "4px" }}>
                            Reserved (Pending Delivery)
                        </div>
                        <div style={{ fontSize: "24px", fontWeight: 700, color: "#f59e0b" }}>
                            {totalReserved.toLocaleString("en-IN")}
                        </div>
                        <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "4px" }}>
                            Allocated for dispatch
                        </div>
                    </div>

                    {/* Metric: Available for Sale */}
                    <div className="card" style={{ padding: "16px" }}>
                        <div style={{ fontSize: "11px", fontWeight: 600, color: "var(--text-muted)", textTransform: "uppercase", marginBottom: "4px" }}>
                            Available for Sale
                        </div>
                        <div style={{ fontSize: "24px", fontWeight: 700, color: "#10b981" }}>
                            {totalAvailable.toLocaleString("en-IN")}
                        </div>
                        <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "4px" }}>
                            On-hand minus reserved
                        </div>
                    </div>
                </div>
            )}

            {/* Navigation Tabs */}
            <div className="settings-tabs" style={{ borderBottom: "1px solid var(--border-color)", paddingBottom: "8px" }}>
                <button
                    type="button"
                    className={`action-btn-sm ${activeTab === "stock" ? "primary-button" : ""}`}
                    onClick={() => switchTab("stock")}
                >
                    <Icon type="inventory" size={14} />
                    <span>Warehouse Stock ({warehouseStocks.length})</span>
                </button>
                <button
                    type="button"
                    className={`action-btn-sm ${activeTab === "movements" ? "primary-button" : ""}`}
                    onClick={() => switchTab("movements")}
                >
                    <Icon type="fileText" size={14} />
                    <span>Stock Audit Trail ({movements.length})</span>
                </button>
                <button
                    type="button"
                    className={`action-btn-sm ${activeTab === "warehouses" ? "primary-button" : ""}`}
                    onClick={() => switchTab("warehouses")}
                >
                    <Icon type="bank" size={14} />
                    <span>All Facilities ({warehouses.length})</span>
                </button>
            </div>

            {/* TAB 1: Warehouse Stock */}
            {activeTab === "stock" && (
                <div className="table-card">
                    <div className="table-header-card">
                        <div className="search-input-wrapper" style={{ maxWidth: "420px", flex: 1 }}>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="11" cy="11" r="8" />
                                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                            </svg>
                            <input
                                type="text"
                                className="search-input"
                                placeholder="Search by SKU, product name, or HSN..."
                                value={search}
                                onChange={e => setSearch(e.target.value)}
                            />
                            {search && (
                                <button type="button" className="search-clear-btn" onClick={() => setSearch("")}>
                                    <Icon type="close" size={13} />
                                </button>
                            )}
                        </div>
                        <span style={{ fontSize: "13px", color: "var(--text-muted)", fontWeight: 500 }}>
                            {filteredStock.length} items in {currentWarehouse?.name || "Warehouse"}
                        </span>
                    </div>

                    {stockLoading || loading ? (
                        <div style={{ padding: "16px" }}>
                            <SkeletonTable rows={5} columns={8} />
                        </div>
                    ) : filteredStock.length === 0 ? (
                        <EmptyState
                            icon="inventory"
                            title={warehouseStocks.length === 0 ? "No inventory tracked in this warehouse" : "No matching inventory found"}
                            message={
                                warehouseStocks.length === 0
                                    ? 'Use "Opening Stock" or record an inward Purchase to initialize stock balances for this warehouse.'
                                    : "No items match your search filter."
                            }
                            action={
                                warehouseStocks.length === 0
                                    ? { label: "Record Opening Stock", onClick: openOpening, icon: "plus" }
                                    : search
                                    ? { label: "Clear Search", onClick: () => setSearch("") }
                                    : null
                            }
                        />
                    ) : (
                        <div className="table-container">
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Product / Item</th>
                                        <th>HSN / SAC</th>
                                        <th>Unit</th>
                                        <th style={{ textAlign: "right" }}>On Hand</th>
                                        <th style={{ textAlign: "right" }}>Reserved</th>
                                        <th style={{ textAlign: "right" }}>Available</th>
                                        <th>Status</th>
                                        <th style={{ textAlign: "right" }}>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredStock.map(ws => {
                                        const p = ws.product;
                                        const onHand = Number(ws.quantityOnHand || 0);
                                        const reserved = Number(ws.quantityReserved || 0);
                                        const available = Number(ws.availableQuantity || ws.quantityOnHand || 0);
                                        const isLow = available <= Number(p?.lowStockThreshold || 5);

                                        return (
                                            <tr key={ws.id}>
                                                <td>
                                                    <strong>{p?.name || "Product"}</strong>
                                                    {p?.sku && (
                                                        <div style={{ fontSize: "11px", color: "var(--text-muted)" }}>
                                                            SKU: {p.sku}
                                                        </div>
                                                    )}
                                                </td>
                                                <td>{p?.hsnSac?.code || p?.hsnCode || "—"}</td>
                                                <td>{p?.unit || "units"}</td>
                                                <td className="table-num" style={{ textAlign: "right", fontWeight: 600 }}>
                                                    {onHand.toLocaleString("en-IN")}
                                                </td>
                                                <td className="table-num" style={{ textAlign: "right", color: reserved > 0 ? "#f59e0b" : "var(--text-muted)" }}>
                                                    {reserved.toLocaleString("en-IN")}
                                                </td>
                                                <td className="table-num" style={{ textAlign: "right", fontWeight: 700, color: available <= 0 ? "#ef4444" : "#10b981" }}>
                                                    {available.toLocaleString("en-IN")}
                                                </td>
                                                <td>
                                                    {available <= 0 ? (
                                                        <span className="badge" style={{ background: "rgba(239, 68, 68, 0.15)", color: "#ef4444" }}>Out of Stock</span>
                                                    ) : isLow ? (
                                                        <span className="badge" style={{ background: "rgba(245, 158, 11, 0.15)", color: "#b45309" }}>Low Stock</span>
                                                    ) : (
                                                        <span className="badge" style={{ background: "rgba(16, 185, 129, 0.15)", color: "#065f46" }}>Healthy</span>
                                                    )}
                                                </td>
                                                <td style={{ textAlign: "right" }}>
                                                    <button
                                                        type="button"
                                                        className="action-btn-sm"
                                                        onClick={() => openAdjust(p, selectedWarehouseId)}
                                                        title="Adjust count in this warehouse"
                                                    >
                                                        Adjust
                                                    </button>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

            {/* TAB 2: Stock Movements Audit Trail */}
            {activeTab === "movements" && (
                <div className="table-card">
                    <div className="table-header-card">
                        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", flex: 1, alignItems: "center" }}>
                            <div className="search-input-wrapper" style={{ maxWidth: "360px", flex: 1 }}>
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <circle cx="11" cy="11" r="8" />
                                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                                </svg>
                                <input
                                    type="text"
                                    className="search-input"
                                    placeholder="Search by product, ref #, warehouse, reason..."
                                    value={search}
                                    onChange={e => setSearch(e.target.value)}
                                />
                                {search && (
                                    <button type="button" className="search-clear-btn" onClick={() => setSearch("")}>
                                        <Icon type="close" size={13} />
                                    </button>
                                )}
                            </div>

                            {/* Movement Type Filter */}
                            <select
                                className="form-control"
                                value={movementFilter}
                                onChange={e => setMovementFilter(e.target.value)}
                                style={{ width: "190px", height: "38px" }}
                            >
                                <option value="ALL">All Movement Types</option>
                                <option value="INVOICE">Invoices (Outward)</option>
                                <option value="INVOICE_CANCELLED">Invoice Reversals</option>
                                <option value="PURCHASE">Purchases (Inward)</option>
                                <option value="PURCHASE_CANCELLED">Purchase Reversals</option>
                                <option value="TRANSFER_OUT">Transfer (Outward)</option>
                                <option value="TRANSFER_IN">Transfer (Inward)</option>
                                <option value="OPENING_STOCK">Opening Stock</option>
                                <option value="MANUAL_ADJUSTMENT">Manual Adjustments</option>
                            </select>

                            {/* Warehouse Facility Filter */}
                            <select
                                className="form-control"
                                value={movementWarehouseFilter}
                                onChange={e => setMovementWarehouseFilter(e.target.value)}
                                style={{ width: "190px", height: "38px" }}
                            >
                                <option value="ALL">All Facilities</option>
                                {warehouses.map(w => (
                                    <option key={w.id} value={w.id}>
                                        {w.name} ({w.code})
                                    </option>
                                ))}
                            </select>

                            <button
                                type="button"
                                className="action-btn-sm"
                                onClick={loadMovements}
                                title="Refresh Audit Trail"
                                disabled={movementsLoading}
                            >
                                <Icon type="refresh" size={13} />
                                <span>Refresh</span>
                            </button>
                        </div>
                        <span style={{ fontSize: "13px", color: "var(--text-muted)", fontWeight: 500 }}>
                            Showing {filteredMovements.length} audit records
                        </span>
                    </div>

                    {movementsLoading ? (
                        <div style={{ padding: "16px" }}>
                            <SkeletonTable rows={6} columns={9} />
                        </div>
                    ) : movementsError ? (
                        <div style={{ padding: "16px" }}>
                            <ErrorState
                                title="Failed to load stock audit trail"
                                message={movementsError}
                                onRetry={loadMovements}
                                retryLabel="Retry Audit Log"
                            />
                        </div>
                    ) : filteredMovements.length === 0 ? (
                        <EmptyState
                            icon="fileText"
                            title={movements.length === 0 ? "No stock movements recorded yet" : "No matching movements found"}
                            message={
                                movements.length === 0
                                    ? "All stock adjustments, invoice deductions, inward purchases, and transfers will be tracked here with immutable timestamps and audit details."
                                    : "No audit records match the current filter or search criteria."
                            }
                            action={
                                search || movementFilter !== "ALL" || movementWarehouseFilter !== "ALL"
                                    ? { label: "Clear Filters", onClick: () => { setSearch(""); setMovementFilter("ALL"); setMovementWarehouseFilter("ALL"); } }
                                    : null
                            }
                        />
                    ) : (
                        <div className="table-container">
                            <table className="table">
                                <thead>
                                    <tr>
                                        <th>Timestamp</th>
                                        <th>Product</th>
                                        <th>Warehouse</th>
                                        <th>Movement</th>
                                        <th>Ref #</th>
                                        <th style={{ textAlign: "right" }}>Quantity</th>
                                        <th style={{ textAlign: "right" }}>Stock Before</th>
                                        <th style={{ textAlign: "right" }}>Stock After</th>
                                        <th style={{ paddingLeft: "16px" }}>Reason / Transfer Route</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {filteredMovements.map(m => {
                                        const qtyNum = Number(m.quantity || 0);
                                        const isPositive = qtyNum > 0;
                                        return (
                                            <tr key={m.id}>
                                                <td style={{ fontSize: "12.5px", color: "var(--text-secondary)", whiteSpace: "nowrap" }}>
                                                    {m.createdAt ? new Date(m.createdAt).toLocaleString("en-IN", { dateStyle: "short", timeStyle: "medium" }) : "—"}
                                                </td>
                                                <td>
                                                    <strong>{m.product?.name || "Product"}</strong>
                                                    {m.product?.sku && (
                                                        <div style={{ fontSize: "11px", color: "var(--text-muted)" }}>
                                                            SKU: {m.product.sku}
                                                        </div>
                                                    )}
                                                </td>
                                                <td>
                                                    <span style={{ fontWeight: 600 }}>{m.warehouse?.name || "Main Warehouse"}</span>
                                                    {m.warehouse?.code && (
                                                        <span style={{ fontSize: "11px", color: "var(--text-muted)", marginLeft: "4px" }}>({m.warehouse.code})</span>
                                                    )}
                                                </td>
                                                <td>
                                                    <span className="badge" style={{
                                                        background: m.movementType === "PURCHASE" || m.movementType === "TRANSFER_IN" || m.movementType === "OPENING_STOCK" ? "rgba(16,185,129,0.12)" : m.movementType === "INVOICE" ? "rgba(99,102,241,0.12)" : "rgba(245,158,11,0.12)",
                                                        color: m.movementType === "PURCHASE" || m.movementType === "TRANSFER_IN" || m.movementType === "OPENING_STOCK" ? "#065f46" : m.movementType === "INVOICE" ? "#4338ca" : "#b45309",
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
                                                    {m.stockBefore !== null && m.stockBefore !== undefined ? m.stockBefore : "—"}
                                                </td>
                                                <td className="table-num" style={{ textAlign: "right", fontWeight: "700" }}>
                                                    {m.stockAfter !== null && m.stockAfter !== undefined ? m.stockAfter : "—"}
                                                </td>
                                                <td style={{ paddingLeft: "16px", fontSize: "12.5px", color: "var(--text-secondary)" }}>
                                                    {m.targetWarehouse ? (
                                                        <span>Route: <strong>{m.warehouse?.name || "Source"} &rarr; {m.targetWarehouse.name} ({m.targetWarehouse.code})</strong></span>
                                                    ) : (
                                                        m.notes || "—"
                                                    )}
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

            {/* TAB 3: Warehouses Facilities List */}
            {activeTab === "warehouses" && (
                <div className="table-card">
                    <div className="table-header-card">
                        <h3>Registered Warehouse Facilities</h3>
                        <button type="button" className="primary-button" onClick={openCreateWarehouse}>
                            <Icon type="plus" size={13} />
                            <span>Add Facility</span>
                        </button>
                    </div>

                    <div className="table-container">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Code</th>
                                    <th>Warehouse Name</th>
                                    <th>Address / City</th>
                                    <th>State</th>
                                    <th>Status</th>
                                    <th>Default</th>
                                </tr>
                            </thead>
                            <tbody>
                                {warehouses.map(w => (
                                    <tr key={w.id}>
                                        <td>
                                            <span style={{ fontWeight: 700, letterSpacing: "0.5px" }}>{w.code}</span>
                                        </td>
                                        <td><strong>{w.name}</strong></td>
                                        <td>{w.address ? `${w.address}, ` : ""}{w.city || "—"}</td>
                                        <td>{w.state || "—"}</td>
                                        <td>
                                            <span className="badge" style={{ background: w.isActive !== false ? "rgba(16,185,129,0.12)" : "rgba(239,68,68,0.12)", color: w.isActive !== false ? "#065f46" : "#ef4444" }}>
                                                {w.isActive !== false ? "Active" : "Inactive"}
                                            </span>
                                        </td>
                                        <td>
                                            {w.isDefault ? (
                                                <span className="badge" style={{ background: "rgba(59,130,246,0.12)", color: "#2563eb", fontWeight: 700, display: "inline-flex", alignItems: "center", gap: "4px" }}>
                                                    <Icon type="star" size={12} color="#2563eb" />
                                                    <span>Default Facility</span>
                                                </span>
                                            ) : (
                                                <span style={{ color: "var(--text-muted)" }}>Secondary</span>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* MODAL 1: Adjust Stock */}
            {showAdjustModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "480px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "700" }}>Physical Stock Audit Adjustment</h3>
                            <button type="button" className="modal-close-btn" onClick={() => setShowAdjustModal(false)}>
                                <Icon type="close" size={14} />
                            </button>
                        </div>

                        {adjustError && (
                            <div style={{ padding: "10px 14px", background: "rgba(239,68,68,0.12)", color: "var(--danger)", borderRadius: "var(--radius-sm)", border: "1px solid #fecaca", marginBottom: "14px", fontSize: "13px" }}>
                                {adjustError}
                            </div>
                        )}

                        <form onSubmit={handleAdjustSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            <div className="form-field">
                                <label>Warehouse Facility <span className="req-star">*</span></label>
                                <select
                                    className="form-control"
                                    value={adjustWarehouseId}
                                    onChange={e => setAdjustWarehouseId(e.target.value)}
                                    required
                                >
                                    {warehouses.map(w => (
                                        <option key={w.id} value={w.id}>
                                            {w.name} ({w.code})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-field">
                                <label>Product Item <span className="req-star">*</span></label>
                                <select
                                    className="form-control"
                                    value={adjustProductId}
                                    onChange={e => {
                                        setAdjustProductId(e.target.value);
                                        const p = products.find(prod => String(prod.id) === e.target.value);
                                        if (p) setAdjustQty(p.stockQuantity || "0");
                                    }}
                                    required
                                >
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.name} ({p.unit || "units"})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-field">
                                <label>New Physical Count / Quantity <span className="req-star">*</span></label>
                                <input
                                    type="number"
                                    step="0.01"
                                    className="form-control"
                                    value={adjustQty}
                                    onChange={e => setAdjustQty(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="form-field">
                                <label>Audit Notes / Reason</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={adjustReason}
                                    onChange={e => setAdjustReason(e.target.value)}
                                    placeholder="e.g. Physical inventory recount, damaged units"
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                                <button type="button" className="secondary-button" onClick={() => setShowAdjustModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className={`primary-button ${adjustSubmitting ? "btn-loading" : ""}`} disabled={adjustSubmitting}>
                                    {adjustSubmitting ? (
                                        <>
                                            <span className="inline-spinner" />
                                            <span>Adjusting...</span>
                                        </>
                                    ) : (
                                        "Commit Adjustment"
                                    )}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* MODAL 2: Inter-Warehouse Transfer */}
            {showTransferModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "500px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "700" }}>Inter-Warehouse Stock Transfer</h3>
                            <button type="button" className="modal-close-btn" onClick={() => setShowTransferModal(false)}>
                                <Icon type="close" size={14} />
                            </button>
                        </div>

                        {transferError && (
                            <div style={{ padding: "10px 14px", background: "rgba(239,68,68,0.12)", color: "var(--danger)", borderRadius: "var(--radius-sm)", border: "1px solid #fecaca", marginBottom: "14px", fontSize: "13px" }}>
                                {transferError}
                            </div>
                        )}

                        <form onSubmit={handleTransferSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                                <div className="form-field">
                                    <label>Source Warehouse <span className="req-star">*</span></label>
                                    <select
                                        className="form-control"
                                        value={transferSourceId}
                                        onChange={e => setTransferSourceId(e.target.value)}
                                        required
                                    >
                                        {warehouses.map(w => (
                                            <option key={w.id} value={w.id}>
                                                {w.name} ({w.code})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-field">
                                    <label>Destination Warehouse <span className="req-star">*</span></label>
                                    <select
                                        className="form-control"
                                        value={transferDestId}
                                        onChange={e => setTransferDestId(e.target.value)}
                                        required
                                    >
                                        {warehouses.map(w => (
                                            <option key={w.id} value={w.id} disabled={String(w.id) === transferSourceId}>
                                                {w.name} ({w.code})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div className="form-field">
                                <label>Product Item <span className="req-star">*</span></label>
                                <select
                                    className="form-control"
                                    value={transferProductId}
                                    onChange={e => setTransferProductId(e.target.value)}
                                    required
                                >
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.name} ({p.unit || "units"})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-field">
                                <label>Transfer Quantity <span className="req-star">*</span></label>
                                <input
                                    type="number"
                                    step="0.01"
                                    min="0.01"
                                    className="form-control"
                                    value={transferQty}
                                    onChange={e => setTransferQty(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="form-field">
                                <label>Transfer Notes / Reason</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={transferNotes}
                                    onChange={e => setTransferNotes(e.target.value)}
                                    placeholder="e.g. Branch replenishment, transit order"
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                                <button type="button" className="secondary-button" onClick={() => setShowTransferModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className={`primary-button ${transferSubmitting ? "btn-loading" : ""}`} disabled={transferSubmitting}>
                                    {transferSubmitting ? (
                                        <>
                                            <span className="inline-spinner" />
                                            <span>Transferring...</span>
                                        </>
                                    ) : (
                                        "Execute Transfer"
                                    )}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* MODAL 3: Opening Stock */}
            {showOpeningModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "480px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "700" }}>Record Opening Stock Balance</h3>
                            <button type="button" className="modal-close-btn" onClick={() => setShowOpeningModal(false)}>
                                <Icon type="close" size={14} />
                            </button>
                        </div>

                        {openingError && (
                            <div style={{ padding: "10px 14px", background: "rgba(239,68,68,0.12)", color: "var(--danger)", borderRadius: "var(--radius-sm)", border: "1px solid #fecaca", marginBottom: "14px", fontSize: "13px" }}>
                                {openingError}
                            </div>
                        )}

                        <form onSubmit={handleOpeningSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            <div className="form-field">
                                <label>Warehouse Facility <span className="req-star">*</span></label>
                                <select
                                    className="form-control"
                                    value={openingWarehouseId}
                                    onChange={e => setOpeningWarehouseId(e.target.value)}
                                    required
                                >
                                    {warehouses.map(w => (
                                        <option key={w.id} value={w.id}>
                                            {w.name} ({w.code})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-field">
                                <label>Product Item <span className="req-star">*</span></label>
                                <select
                                    className="form-control"
                                    value={openingProductId}
                                    onChange={e => setOpeningProductId(e.target.value)}
                                    required
                                >
                                    {products.map(p => (
                                        <option key={p.id} value={p.id}>
                                            {p.name} ({p.unit || "units"})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-field">
                                <label>Opening Quantity <span className="req-star">*</span></label>
                                <input
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    className="form-control"
                                    value={openingQty}
                                    onChange={e => setOpeningQty(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="form-field">
                                <label>Reference # / Batch</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={openingRef}
                                    onChange={e => setOpeningRef(e.target.value)}
                                    placeholder="e.g. OPENING-2026"
                                />
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                                <button type="button" className="secondary-button" onClick={() => setShowOpeningModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className={`primary-button ${openingSubmitting ? "btn-loading" : ""}`} disabled={openingSubmitting}>
                                    {openingSubmitting ? (
                                        <>
                                            <span className="inline-spinner" />
                                            <span>Recording...</span>
                                        </>
                                    ) : (
                                        "Record Balance"
                                    )}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* MODAL 4: Create Warehouse Facility */}
            {showWarehouseModal && (
                <div className="modal-backdrop">
                    <div className="modal-card" style={{ maxWidth: "480px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                            <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "700" }}>Register Warehouse Facility</h3>
                            <button type="button" className="modal-close-btn" onClick={() => setShowWarehouseModal(false)}>
                                <Icon type="close" size={14} />
                            </button>
                        </div>

                        {whError && (
                            <div style={{ padding: "10px 14px", background: "rgba(239,68,68,0.12)", color: "var(--danger)", borderRadius: "var(--radius-sm)", border: "1px solid #fecaca", marginBottom: "14px", fontSize: "13px" }}>
                                {whError}
                            </div>
                        )}

                        <form onSubmit={handleWarehouseSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: "12px" }}>
                                <div className="form-field">
                                    <label>Warehouse Name <span className="req-star">*</span></label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={whName}
                                        onChange={e => setWhName(e.target.value)}
                                        placeholder="e.g. Pune Central Depot"
                                        required
                                        autoFocus
                                    />
                                </div>
                                <div className="form-field">
                                    <label>Code <span className="req-star">*</span></label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={whCode}
                                        onChange={e => setWhCode(e.target.value.toUpperCase())}
                                        placeholder="PUN-01"
                                        required
                                        maxLength={10}
                                    />
                                </div>
                            </div>

                            <div className="form-field">
                                <label>Street Address</label>
                                <input
                                    type="text"
                                    className="form-control"
                                    value={whAddress}
                                    onChange={e => setWhAddress(e.target.value)}
                                    placeholder="Plot No. 45, MIDC Bhosari"
                                />
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "10px" }}>
                                <div className="form-field">
                                    <label>City</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={whCity}
                                        onChange={e => setWhCity(e.target.value)}
                                        placeholder="Pune"
                                    />
                                </div>
                                <div className="form-field">
                                    <label>State</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={whState}
                                        onChange={e => setWhState(e.target.value)}
                                        placeholder="Maharashtra"
                                    />
                                </div>
                                <div className="form-field">
                                    <label>Pincode</label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={whPincode}
                                        onChange={e => setWhPincode(e.target.value)}
                                        placeholder="411026"
                                        maxLength={6}
                                    />
                                </div>
                            </div>

                            <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "4px" }}>
                                <input
                                    type="checkbox"
                                    id="whDefault"
                                    checked={whIsDefault}
                                    onChange={e => setWhIsDefault(e.target.checked)}
                                />
                                <label htmlFor="whDefault" style={{ fontSize: "13px", cursor: "pointer" }}>
                                    Set as primary / default warehouse for this business
                                </label>
                            </div>

                            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                                <button type="button" className="secondary-button" onClick={() => setShowWarehouseModal(false)}>
                                    Cancel
                                </button>
                                <button type="submit" className={`primary-button ${whSubmitting ? "btn-loading" : ""}`} disabled={whSubmitting}>
                                    {whSubmitting ? (
                                        <>
                                            <span className="inline-spinner" />
                                            <span>Creating...</span>
                                        </>
                                    ) : (
                                        "Save Facility"
                                    )}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
