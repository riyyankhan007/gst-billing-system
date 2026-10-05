import React, { useEffect, useState } from "react";
import { getSalesReport, getGstReport, getCustomerReport, getProductReport, downloadExportCsv } from "../services/api";

export default function Reports() {
    const [tab, setTab] = useState("sales"); // "sales", "gst", "customers", "products"
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // Date filters
    const todayStr = new Date().toISOString().split("T")[0];
    const monthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0];
    const [startDate, setStartDate] = useState(monthAgo);
    const [endDate, setEndDate] = useState(todayStr);

    // Data states
    const [salesData, setSalesData] = useState(null);
    const [gstData, setGstData] = useState(null);
    const [customerData, setCustomerData] = useState([]);
    const [productData, setProductData] = useState([]);

    useEffect(() => {
        loadReport();
    }, [tab, startDate, endDate]);

    async function loadReport() {
        try {
            setLoading(true);
            setError("");
            if (tab === "sales") {
                const res = await getSalesReport(startDate, endDate);
                setSalesData(res);
            } else if (tab === "gst") {
                const res = await getGstReport(startDate, endDate);
                setGstData(res);
            } else if (tab === "customers") {
                const res = await getCustomerReport();
                setCustomerData(res);
            } else if (tab === "products") {
                const res = await getProductReport();
                setProductData(res);
            }
        } catch (err) {
            setError(err.message || "Failed to load report data");
        } finally {
            setLoading(false);
        }
    }

    async function handleExport(type) {
        try {
            const blob = await downloadExportCsv(type);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = `${type}_export_${todayStr}.csv`;
            document.body.appendChild(a);
            a.click();
            a.remove();
        } catch (err) {
            alert(err.message || "Export failed");
        }
    }

    const fmt = val => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(val || 0));

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {/* Page Header & CSV Export Actions */}
            <div className="page-heading">
                <div>
                    <span className="eyebrow">COMPLIANCE & AUDIT</span>
                    <h1>GST & Sales Reports</h1>
                    <p>Comprehensive sales turnover, GSTR-1 summaries, customer ledgers, and official CSV exports</p>
                </div>
                <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center" }}>
                    <button type="button" className="action-btn-sm" onClick={() => handleExport("invoices")}>
                        Export Invoices CSV
                    </button>
                    <button type="button" className="action-btn-sm" onClick={() => handleExport("customers")}>
                        Export Customers CSV
                    </button>
                    <button type="button" className="action-btn-sm" onClick={() => handleExport("products")}>
                        Export Products CSV
                    </button>
                    <button type="button" className="action-btn-sm" onClick={() => handleExport("payments")}>
                        Export Payments CSV
                    </button>
                </div>
            </div>

            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.12)", color: "var(--danger)", borderRadius: "var(--radius-sm)", border: "1px solid #fecaca", fontWeight: 500 }}>
                    {error}
                </div>
            )}

            {/* Filter & Navigation Bar */}
            <div className="table-card" style={{ padding: "14px 20px" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "16px" }}>
                    {/* Navigation Tabs */}
                    <div className="analytics-range-selector">
                        <button
                            type="button"
                            className={`range-tab-btn ${tab === "sales" ? "active" : ""}`}
                            onClick={() => setTab("sales")}
                        >
                            Sales Report
                        </button>
                        <button
                            type="button"
                            className={`range-tab-btn ${tab === "gst" ? "active" : ""}`}
                            onClick={() => setTab("gst")}
                        >
                            GST / GSTR-1 Summary
                        </button>
                        <button
                            type="button"
                            className={`range-tab-btn ${tab === "customers" ? "active" : ""}`}
                            onClick={() => setTab("customers")}
                        >
                            Customer Ledger
                        </button>
                        <button
                            type="button"
                            className={`range-tab-btn ${tab === "products" ? "active" : ""}`}
                            onClick={() => setTab("products")}
                        >
                            Product Sales
                        </button>
                    </div>

                    {/* Date Range Picker for Sales & GST tabs */}
                    {(tab === "sales" || tab === "gst") && (
                        <div className="custom-date-bar">
                            <span style={{ fontSize: "12px", fontWeight: "600", color: "var(--text-muted)" }}>Period:</span>
                            <input
                                type="date"
                                className="custom-date-input"
                                value={startDate}
                                onChange={e => setStartDate(e.target.value)}
                            />
                            <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>to</span>
                            <input
                                type="date"
                                className="custom-date-input"
                                value={endDate}
                                onChange={e => setEndDate(e.target.value)}
                            />
                            <button type="button" className="action-btn-sm primary-button" style={{ padding: "4px 10px", fontSize: "11.5px" }} onClick={() => loadReport()}>
                                Refresh
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* Content Area */}
            {loading ? (
                <div className="card loading-state" style={{ padding: "40px", textAlign: "center" }}>
                    <div className="spinner" style={{ margin: "0 auto 12px" }} />
                    <p>Generating report metrics...</p>
                </div>
            ) : (
                <>
                    {/* TAB 1: SALES REPORT */}
                    {tab === "sales" && salesData && (
                        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                            <div className="analytics-kpi-grid">
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#2563eb" }} />
                                    <div className="kpi-title">Gross Turnover</div>
                                    <div className="kpi-main-val" style={{ color: "#1e40af" }}>{fmt(salesData.totalSalesAmount)}</div>
                                    <div className="kpi-sub-row">{salesData.totalInvoices} invoices generated</div>
                                </div>
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#6366f1" }} />
                                    <div className="kpi-title">Taxable Value</div>
                                    <div className="kpi-main-val" style={{ color: "#4338ca" }}>{fmt(salesData.totalTaxableAmount)}</div>
                                    <div className="kpi-sub-row">Net billed value before GST</div>
                                </div>
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#10b981" }} />
                                    <div className="kpi-title">Payments Collected</div>
                                    <div className="kpi-main-val" style={{ color: "#065f46" }}>{fmt(salesData.totalCollected)}</div>
                                    <div className="kpi-sub-row">Settled payments received</div>
                                </div>
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#ef4444" }} />
                                    <div className="kpi-title">Balance Outstanding</div>
                                    <div className="kpi-main-val" style={{ color: "#991b1b" }}>{fmt(salesData.totalOutstanding)}</div>
                                    <div className="kpi-sub-row">Unpaid invoice receivables</div>
                                </div>
                            </div>

                            <div className="table-card">
                                <div className="table-header-card">
                                    <div>
                                        <h3 style={{ margin: 0, fontSize: "16px", fontWeight: "700" }}>Daily Sales Breakdown</h3>
                                        <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Date-wise turnover and tax aggregated for the period</span>
                                    </div>
                                </div>
                                {salesData.dailyBreakdown.length === 0 ? (
                                    <p style={{ padding: "30px", textAlign: "center", color: "var(--text-muted)" }}>No sales recorded in this date range.</p>
                                ) : (
                                    <div className="table-container">
                                        <table className="table">
                                            <thead>
                                                <tr>
                                                    <th>Date</th>
                                                    <th>Invoices Count</th>
                                                    <th style={{ textAlign: "right" }}>Taxable Amount</th>
                                                    <th style={{ textAlign: "right" }}>GST Tax</th>
                                                    <th style={{ textAlign: "right" }}>Total Sales</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {salesData.dailyBreakdown.map((d, i) => (
                                                    <tr key={i}>
                                                        <td>{d.date}</td>
                                                        <td><strong>{d.invoiceCount}</strong></td>
                                                        <td className="table-num" style={{ textAlign: "right" }}>{fmt(d.taxableAmount)}</td>
                                                        <td className="table-num" style={{ textAlign: "right", color: "#6366f1" }}>{fmt(d.taxAmount)}</td>
                                                        <td className="table-num" style={{ textAlign: "right", fontWeight: "700" }}>{fmt(d.totalAmount)}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {/* TAB 2: GST REPORT */}
                    {tab === "gst" && gstData && (
                        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                            {/* Tax Metrics */}
                            <div className="analytics-kpi-grid">
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#8b5cf6" }} />
                                    <div className="kpi-title">Total Output GST</div>
                                    <div className="kpi-main-val" style={{ color: "#6d28d9" }}>{fmt(gstData.totalTax)}</div>
                                    <div className="kpi-sub-row">On turnover {fmt(gstData.totalTaxableAmount)}</div>
                                </div>
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#4f46e5" }} />
                                    <div className="kpi-title">Central GST (CGST)</div>
                                    <div className="kpi-main-val" style={{ color: "#4338ca" }}>{fmt(gstData.totalCgst)}</div>
                                    <div className="kpi-sub-row">Intrastate Central Share</div>
                                </div>
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#7c3aed" }} />
                                    <div className="kpi-title">State GST (SGST)</div>
                                    <div className="kpi-main-val" style={{ color: "#5b21b6" }}>{fmt(gstData.totalSgst)}</div>
                                    <div className="kpi-sub-row">Intrastate State Share</div>
                                </div>
                                <div className="analytics-kpi-card">
                                    <div className="kpi-stripe" style={{ backgroundColor: "#ec4899" }} />
                                    <div className="kpi-title">Integrated GST (IGST)</div>
                                    <div className="kpi-main-val" style={{ color: "#be185d" }}>{fmt(gstData.totalIgst)}</div>
                                    <div className="kpi-sub-row">Interstate Supplies</div>
                                </div>
                            </div>

                            {/* B2B / B2C / Export Section */}
                            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "16px" }}>
                                <div className="card" style={{ padding: "20px" }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                        <h4 style={{ margin: 0, fontSize: "15px", fontWeight: "700" }}>B2B Invoices (Registered)</h4>
                                        <span className="badge" style={{ backgroundColor: "#e0f2fe", color: "#0369a1" }}>{gstData.b2bCount} invoices</span>
                                    </div>
                                    <div style={{ fontSize: "13px", color: "var(--text-muted)", marginTop: "8px" }}>Taxable: {fmt(gstData.b2bTaxable)}</div>
                                    <div style={{ fontSize: "13px", color: "var(--text-muted)" }}>Tax: {fmt(gstData.b2bTax)}</div>
                                    <div style={{ fontSize: "18px", fontWeight: "800", marginTop: "10px", color: "var(--text-primary)" }}>{fmt(gstData.b2bTotal)}</div>
                                </div>

                                <div className="card" style={{ padding: "20px" }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                        <h4 style={{ margin: 0, fontSize: "15px", fontWeight: "700" }}>B2C Invoices (Consumers)</h4>
                                        <span className="badge" style={{ backgroundColor: "#ede9fe", color: "#6d28d9" }}>{gstData.b2cCount} invoices</span>
                                    </div>
                                    <div style={{ fontSize: "13px", color: "var(--text-muted)", marginTop: "8px" }}>Taxable: {fmt(gstData.b2cTaxable)}</div>
                                    <div style={{ fontSize: "13px", color: "var(--text-muted)" }}>Tax: {fmt(gstData.b2cTax)}</div>
                                    <div style={{ fontSize: "18px", fontWeight: "800", marginTop: "10px", color: "var(--text-primary)" }}>{fmt(gstData.b2cTotal)}</div>
                                </div>

                                <div className="card" style={{ padding: "20px" }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                        <h4 style={{ margin: 0, fontSize: "15px", fontWeight: "700" }}>Export Transactions</h4>
                                        <span className="badge" style={{ backgroundColor: "#fef3c7", color: "#b45309" }}>{gstData.exportCount} invoices</span>
                                    </div>
                                    <div style={{ fontSize: "13px", color: "var(--text-muted)", marginTop: "8px" }}>Taxable: {fmt(gstData.exportTaxable)}</div>
                                    <div style={{ fontSize: "13px", color: "var(--text-muted)" }}>Tax: {fmt(gstData.exportTax)}</div>
                                    <div style={{ fontSize: "18px", fontWeight: "800", marginTop: "10px", color: "var(--text-primary)" }}>{fmt(gstData.exportTotal)}</div>
                                </div>
                            </div>

                            {/* HSN/SAC Summary Table */}
                            <div className="table-card">
                                <div className="table-header-card">
                                    <div>
                                        <h3 style={{ margin: 0, fontSize: "16px", fontWeight: "700" }}>HSN / SAC Summary (GSTR-1 Table 12)</h3>
                                        <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Aggregated quantities and tax amounts grouped by HSN/SAC codes</span>
                                    </div>
                                </div>
                                {gstData.hsnSummary.length === 0 ? (
                                    <p style={{ padding: "30px", textAlign: "center", color: "var(--text-muted)" }}>No HSN items in this date range.</p>
                                ) : (
                                    <div className="table-container">
                                        <table className="table">
                                            <thead>
                                                <tr>
                                                    <th>HSN/SAC</th>
                                                    <th>Description</th>
                                                    <th>UQC</th>
                                                    <th style={{ textAlign: "right" }}>Total Qty</th>
                                                    <th style={{ textAlign: "right" }}>Taxable Value</th>
                                                    <th style={{ textAlign: "right" }}>CGST</th>
                                                    <th style={{ textAlign: "right" }}>SGST</th>
                                                    <th style={{ textAlign: "right" }}>IGST</th>
                                                    <th style={{ textAlign: "right" }}>Total Tax</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {gstData.hsnSummary.map((h, i) => (
                                                    <tr key={i}>
                                                        <td style={{ fontWeight: "600", letterSpacing: "0.2px" }}>{h.hsnCode}</td>
                                                        <td>{h.description || "—"}</td>
                                                        <td>{h.uqc}</td>
                                                        <td className="table-num" style={{ textAlign: "right" }}>{h.totalQuantity}</td>
                                                        <td className="table-num" style={{ textAlign: "right" }}>{fmt(h.taxableValue)}</td>
                                                        <td className="table-num" style={{ textAlign: "right" }}>{fmt(h.cgstAmount)}</td>
                                                        <td className="table-num" style={{ textAlign: "right" }}>{fmt(h.sgstAmount)}</td>
                                                        <td className="table-num" style={{ textAlign: "right" }}>{fmt(h.igstAmount)}</td>
                                                        <td className="table-num" style={{ textAlign: "right", fontWeight: "700", color: "#6366f1" }}>{fmt(h.totalTax)}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}

                    {/* TAB 3: CUSTOMER REPORT */}
                    {tab === "customers" && (
                        <div className="table-card">
                            <div className="table-header-card">
                                <div>
                                    <h3 style={{ margin: 0, fontSize: "16px", fontWeight: "700" }}>Customer Balances & Turnover</h3>
                                    <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Customer receivables, paid amounts, and overdue ledger</span>
                                </div>
                            </div>
                            {customerData.length === 0 ? (
                                <p style={{ padding: "30px", textAlign: "center", color: "var(--text-muted)" }}>No customer records found.</p>
                            ) : (
                                <div className="table-container">
                                    <table className="table">
                                        <thead>
                                            <tr>
                                                <th>Customer Name</th>
                                                <th>GSTIN</th>
                                                <th>Type</th>
                                                <th>State</th>
                                                <th>Invoices</th>
                                                <th style={{ textAlign: "right" }}>Total Invoiced</th>
                                                <th style={{ textAlign: "right" }}>Total Paid</th>
                                                <th style={{ textAlign: "right" }}>Outstanding</th>
                                                <th style={{ textAlign: "right" }}>Overdue</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {customerData.map(c => (
                                                <tr key={c.customerId}>
                                                    <td style={{ fontWeight: "600" }}>{c.customerName}</td>
                                                    <td style={{ letterSpacing: "0.2px" }}>{c.gstin || "B2C"}</td>
                                                    <td>
                                                        <span className="badge" style={{ backgroundColor: "#e0f2fe", color: "#0369a1" }}>
                                                            {c.customerType || "B2B"}
                                                        </span>
                                                    </td>
                                                    <td>{c.state || "—"}</td>
                                                    <td>{c.totalInvoices}</td>
                                                    <td className="table-num" style={{ textAlign: "right", fontWeight: "600" }}>{fmt(c.totalInvoiced)}</td>
                                                    <td className="table-num" style={{ textAlign: "right", color: "var(--success)" }}>{fmt(c.totalPaid)}</td>
                                                    <td className="table-num" style={{ textAlign: "right", color: Number(c.outstandingBalance) > 0 ? "var(--warning)" : "var(--success)", fontWeight: "700" }}>
                                                        {fmt(c.outstandingBalance)}
                                                    </td>
                                                    <td className="table-num" style={{ textAlign: "right", color: Number(c.overdueBalance) > 0 ? "var(--danger)" : "var(--text-muted)", fontWeight: Number(c.overdueBalance) > 0 ? 700 : 400 }}>
                                                        {fmt(c.overdueBalance)}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}

                    {/* TAB 4: PRODUCT REPORT */}
                    {tab === "products" && (
                        <div className="table-card">
                            <div className="table-header-card">
                                <div>
                                    <h3 style={{ margin: 0, fontSize: "16px", fontWeight: "700" }}>Product Performance & Revenue</h3>
                                    <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Sales volume and revenue generated across catalog items</span>
                                </div>
                            </div>
                            {productData.length === 0 ? (
                                <p style={{ padding: "30px", textAlign: "center", color: "var(--text-muted)" }}>No products found.</p>
                            ) : (
                                <div className="table-container">
                                    <table className="table">
                                        <thead>
                                            <tr>
                                                <th>Product Name</th>
                                                <th>SKU</th>
                                                <th>HSN/SAC</th>
                                                <th>Current Stock</th>
                                                <th style={{ textAlign: "right" }}>Units Sold</th>
                                                <th style={{ textAlign: "right" }}>Tax Collected</th>
                                                <th style={{ textAlign: "right" }}>Revenue Generated</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {productData.map(p => (
                                                <tr key={p.productId}>
                                                    <td style={{ fontWeight: "600" }}>{p.productName}</td>
                                                    <td style={{ letterSpacing: "0.2px" }}>{p.sku || "—"}</td>
                                                    <td style={{ letterSpacing: "0.2px" }}>{p.hsnCode || "—"}</td>
                                                    <td>{p.currentStock !== null ? `${p.currentStock} ${p.unit || ""}` : "—"}</td>
                                                    <td className="table-num" style={{ textAlign: "right", fontWeight: "600" }}>{p.totalQuantitySold}</td>
                                                    <td className="table-num" style={{ textAlign: "right", color: "#6366f1" }}>{fmt(p.totalTaxCollected)}</td>
                                                    <td className="table-num" style={{ textAlign: "right", fontWeight: "700", color: "var(--primary)" }}>{fmt(p.totalRevenue)}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
