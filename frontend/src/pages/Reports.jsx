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
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">COMPLIANCE & ANALYTICS</span>
                    <h1>Business & GST Reports</h1>
                    <p>Comprehensive sales analytics, GSTR-1 summaries, and CSV data exports</p>
                </div>
                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                    <button className="secondary-button" onClick={() => handleExport("invoices")}>
                        Export Invoices CSV
                    </button>
                    <button className="btn btn-secondary btn-sm" onClick={() => handleExport("customers")}>
                        Export Customers CSV
                    </button>
                    <button className="btn btn-secondary btn-sm" onClick={() => handleExport("products")}>
                        Export Products CSV
                    </button>
                    <button className="btn btn-secondary btn-sm" onClick={() => handleExport("payments")}>
                        Export Payments CSV
                    </button>
                </div>
            </div>

            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.15)", color: "var(--danger)", borderRadius: "8px" }}>
                    {error}
                </div>
            )}

            {/* Navigation Tabs */}
            <div style={{ display: "flex", gap: "10px", borderBottom: "1px solid var(--border)", paddingBottom: "8px", flexWrap: "wrap" }}>
                <button
                    className={`btn ${tab === "sales" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("sales")}
                >
                    Sales Report
                </button>
                <button
                    className={`btn ${tab === "gst" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("gst")}
                >
                    GST / GSTR-1 Summary
                </button>
                <button
                    className={`btn ${tab === "customers" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("customers")}
                >
                    Customer Ledger
                </button>
                <button
                    className={`btn ${tab === "products" ? "btn-primary" : "btn-secondary"}`}
                    onClick={() => setTab("products")}
                >
                    Product Sales
                </button>
            </div>

            {/* Date Range Picker for Sales & GST tabs */}
            {(tab === "sales" || tab === "gst") && (
                <div className="card" style={{ padding: "14px 20px" }}>
                    <div style={{ display: "flex", gap: "16px", alignItems: "center", flexWrap: "wrap" }}>
                        <span style={{ fontSize: "13px", fontWeight: "600", color: "var(--muted)" }}>Date Range:</span>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                            <label style={{ fontSize: "12px" }}>From:</label>
                            <input
                                type="date"
                                className="input"
                                value={startDate}
                                onChange={e => setStartDate(e.target.value)}
                                style={{ maxWidth: "160px" }}
                            />
                        </div>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                            <label style={{ fontSize: "12px" }}>To:</label>
                            <input
                                type="date"
                                className="input"
                                value={endDate}
                                onChange={e => setEndDate(e.target.value)}
                                style={{ maxWidth: "160px" }}
                            />
                        </div>
                        <button className="btn btn-secondary btn-sm" onClick={() => loadReport()}>
                            Refresh
                        </button>
                    </div>
                </div>
            )}

            {/* Content Area */}
            {loading ? (
                <div className="card" style={{ padding: "40px", textAlign: "center" }}>
                    <div className="spinner" style={{ margin: "0 auto 12px" }} />
                    <p>Generating report...</p>
                </div>
            ) : (
                <>
                    {/* TAB 1: SALES REPORT */}
                    {tab === "sales" && salesData && (
                        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "14px" }}>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Invoices Generated</span>
                                    <div style={{ fontSize: "22px", fontWeight: "700" }}>{salesData.totalInvoices}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Taxable Value</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700" }}>{fmt(salesData.totalTaxableAmount)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>GST Tax</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700", color: "#6366f1" }}>{fmt(salesData.totalTaxAmount)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Gross Sales</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700", color: "var(--primary)" }}>{fmt(salesData.totalSalesAmount)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Total Collected</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700", color: "#10b981" }}>{fmt(salesData.totalCollected)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Balance Due</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700", color: "#ef4444" }}>{fmt(salesData.totalOutstanding)}</div>
                                </div>
                            </div>

                            <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                                <div style={{ padding: "14px 20px", borderBottom: "1px solid var(--border)" }}>
                                    <h4 style={{ margin: 0, fontSize: "15px" }}>Daily Sales Breakdown</h4>
                                </div>
                                {salesData.dailyBreakdown.length === 0 ? (
                                    <p style={{ padding: "30px", textAlign: "center", color: "var(--muted)" }}>No sales in this date range.</p>
                                ) : (
                                    <table className="table" style={{ width: "100%", margin: 0 }}>
                                        <thead>
                                            <tr>
                                                <th>Date</th>
                                                <th>Invoices Count</th>
                                                <th style={{ textAlign: "right" }}>Taxable Amount</th>
                                                <th style={{ textAlign: "right" }}>Tax Amount</th>
                                                <th style={{ textAlign: "right" }}>Total Sales</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {salesData.dailyBreakdown.map((d, i) => (
                                                <tr key={i}>
                                                    <td>{d.date}</td>
                                                    <td>{d.invoiceCount}</td>
                                                    <td style={{ textAlign: "right" }}>{fmt(d.taxableAmount)}</td>
                                                    <td style={{ textAlign: "right", color: "#6366f1" }}>{fmt(d.taxAmount)}</td>
                                                    <td style={{ textAlign: "right", fontWeight: "700" }}>{fmt(d.totalAmount)}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    )}

                    {/* TAB 2: GST REPORT */}
                    {tab === "gst" && gstData && (
                        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                            {/* Tax Metrics */}
                            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "14px" }}>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Total Taxable Turnover</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700" }}>{fmt(gstData.totalTaxableAmount)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>CGST</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700" }}>{fmt(gstData.totalCgst)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>SGST</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700" }}>{fmt(gstData.totalSgst)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>IGST</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700" }}>{fmt(gstData.totalIgst)}</div>
                                </div>
                                <div className="card" style={{ padding: "16px" }}>
                                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Total Output GST</span>
                                    <div style={{ fontSize: "20px", fontWeight: "700", color: "#6366f1" }}>{fmt(gstData.totalTax)}</div>
                                </div>
                            </div>

                            {/* B2B / B2C / Export Section */}
                            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "16px" }}>
                                <div className="card" style={{ padding: "16px" }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                        <h4 style={{ margin: 0, fontSize: "15px" }}>B2B Invoices (Registered)</h4>
                                        <span className="badge">{gstData.b2bCount} invoices</span>
                                    </div>
                                    <div style={{ fontSize: "13px", color: "var(--muted)" }}>Taxable: {fmt(gstData.b2bTaxable)}</div>
                                    <div style={{ fontSize: "13px", color: "var(--muted)" }}>Tax: {fmt(gstData.b2bTax)}</div>
                                    <div style={{ fontSize: "16px", fontWeight: "700", marginTop: "6px" }}>Total: {fmt(gstData.b2bTotal)}</div>
                                </div>

                                <div className="card" style={{ padding: "16px" }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                        <h4 style={{ margin: 0, fontSize: "15px" }}>B2C Invoices (Consumers)</h4>
                                        <span className="badge">{gstData.b2cCount} invoices</span>
                                    </div>
                                    <div style={{ fontSize: "13px", color: "var(--muted)" }}>Taxable: {fmt(gstData.b2cTaxable)}</div>
                                    <div style={{ fontSize: "13px", color: "var(--muted)" }}>Tax: {fmt(gstData.b2cTax)}</div>
                                    <div style={{ fontSize: "16px", fontWeight: "700", marginTop: "6px" }}>Total: {fmt(gstData.b2cTotal)}</div>
                                </div>

                                <div className="card" style={{ padding: "16px" }}>
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                                        <h4 style={{ margin: 0, fontSize: "15px" }}>Export Transactions</h4>
                                        <span className="badge">{gstData.exportCount} invoices</span>
                                    </div>
                                    <div style={{ fontSize: "13px", color: "var(--muted)" }}>Taxable: {fmt(gstData.exportTaxable)}</div>
                                    <div style={{ fontSize: "16px", fontWeight: "700", marginTop: "6px" }}>Total: {fmt(gstData.exportTotal)}</div>
                                </div>
                            </div>

                            {/* HSN/SAC Summary Table */}
                            <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                                <div style={{ padding: "14px 20px", borderBottom: "1px solid var(--border)" }}>
                                    <h4 style={{ margin: 0, fontSize: "15px" }}>HSN / SAC Summary (GSTR-1 Table 12)</h4>
                                </div>
                                {gstData.hsnSummary.length === 0 ? (
                                    <p style={{ padding: "30px", textAlign: "center", color: "var(--muted)" }}>No HSN items in this date range.</p>
                                ) : (
                                    <table className="table" style={{ width: "100%", margin: 0 }}>
                                        <thead>
                                            <tr>
                                                <th>HSN/SAC</th>
                                                <th>Description</th>
                                                <th>UQC / Unit</th>
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
                                                    <td style={{ fontWeight: "600", fontFamily: "monospace" }}>{h.hsnCode}</td>
                                                    <td>{h.description || "-"}</td>
                                                    <td>{h.uqc}</td>
                                                    <td style={{ textAlign: "right" }}>{h.totalQuantity}</td>
                                                    <td style={{ textAlign: "right" }}>{fmt(h.taxableValue)}</td>
                                                    <td style={{ textAlign: "right" }}>{fmt(h.cgstAmount)}</td>
                                                    <td style={{ textAlign: "right" }}>{fmt(h.sgstAmount)}</td>
                                                    <td style={{ textAlign: "right" }}>{fmt(h.igstAmount)}</td>
                                                    <td style={{ textAlign: "right", fontWeight: "700", color: "#6366f1" }}>{fmt(h.totalTax)}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    )}

                    {/* TAB 3: CUSTOMER REPORT */}
                    {tab === "customers" && (
                        <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                            <div style={{ padding: "14px 20px", borderBottom: "1px solid var(--border)" }}>
                                <h4 style={{ margin: 0, fontSize: "15px" }}>Customer Balances & Turnover</h4>
                            </div>
                            {customerData.length === 0 ? (
                                <p style={{ padding: "30px", textAlign: "center", color: "var(--muted)" }}>No customers found.</p>
                            ) : (
                                <table className="table" style={{ width: "100%", margin: 0 }}>
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
                                                <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{c.gstin || "B2C"}</td>
                                                <td><span className="badge">{c.customerType || "B2B"}</span></td>
                                                <td>{c.state || "-"}</td>
                                                <td>{c.totalInvoices}</td>
                                                <td style={{ textAlign: "right", fontWeight: "600" }}>{fmt(c.totalInvoiced)}</td>
                                                <td style={{ textAlign: "right", color: "#10b981" }}>{fmt(c.totalPaid)}</td>
                                                <td style={{ textAlign: "right", color: Number(c.outstandingBalance) > 0 ? "#ef4444" : "inherit", fontWeight: "700" }}>
                                                    {fmt(c.outstandingBalance)}
                                                </td>
                                                <td style={{ textAlign: "right", color: Number(c.overdueBalance) > 0 ? "var(--danger)" : "var(--muted)" }}>
                                                    {fmt(c.overdueBalance)}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    )}

                    {/* TAB 4: PRODUCT REPORT */}
                    {tab === "products" && (
                        <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                            <div style={{ padding: "14px 20px", borderBottom: "1px solid var(--border)" }}>
                                <h4 style={{ margin: 0, fontSize: "15px" }}>Product Performance & Revenue</h4>
                            </div>
                            {productData.length === 0 ? (
                                <p style={{ padding: "30px", textAlign: "center", color: "var(--muted)" }}>No products found.</p>
                            ) : (
                                <table className="table" style={{ width: "100%", margin: 0 }}>
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
                                                <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{p.sku || "-"}</td>
                                                <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{p.hsnCode || "-"}</td>
                                                <td>{p.currentStock !== null ? `${p.currentStock} ${p.unit || ""}` : "-"}</td>
                                                <td style={{ textAlign: "right", fontWeight: "600" }}>{p.totalQuantitySold}</td>
                                                <td style={{ textAlign: "right", color: "#6366f1" }}>{fmt(p.totalTaxCollected)}</td>
                                                <td style={{ textAlign: "right", fontWeight: "700", color: "var(--primary)" }}>{fmt(p.totalRevenue)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
