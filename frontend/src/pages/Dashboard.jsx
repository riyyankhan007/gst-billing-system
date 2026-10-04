import React, { useEffect, useState } from "react";
import { getDashboardMetrics, downloadInvoicePdf } from "../services/api";

export default function Dashboard({ onNavigate, onSelectInvoice }) {
    const [metrics, setMetrics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        loadMetrics();
    }, []);

    async function loadMetrics() {
        try {
            setLoading(true);
            setError("");
            const data = await getDashboardMetrics();
            setMetrics(data);
        } catch (err) {
            setError(err.message || "Failed to load dashboard metrics");
        } finally {
            setLoading(false);
        }
    }

    const fmt = (val) => {
        const num = Number(val || 0);
        return new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency: "INR",
            maximumFractionDigits: 2
        }).format(num);
    };

    if (loading) {
        return (
            <div className="card loading-state" style={{ padding: "40px", textAlign: "center" }}>
                <div className="spinner" style={{ margin: "0 auto 16px" }} />
                <p>Loading business analytics...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="card" style={{ padding: "24px", color: "var(--danger)" }}>
                <h3>Error Loading Dashboard</h3>
                <p>{error}</p>
                <button className="btn btn-secondary" onClick={loadMetrics} style={{ marginTop: "12px" }}>
                    Retry
                </button>
            </div>
        );
    }

    if (!metrics) return null;

    return (
        <div className="dashboard-content" style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {/* Header & Quick Actions */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "16px" }}>
                <div>
                    <h2 style={{ margin: 0, fontSize: "24px", fontWeight: "700", color: "var(--text-primary)" }}>Business Overview</h2>
                    <p style={{ margin: "4px 0 0", color: "var(--text-muted)" }}>Real-time GST metrics, sales revenue, and stock alerts</p>
                </div>
                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                    <button className="primary-button" onClick={() => onNavigate("create")}>
                        + New Invoice
                    </button>
                    <button className="secondary-button" onClick={() => onNavigate("payments")}>
                        Record Payment
                    </button>
                    <button className="secondary-button" onClick={() => onNavigate("reports")}>
                        View Reports
                    </button>
                </div>
            </div>

            {/* Top Sales KPI Cards */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                <div className="card" style={{ padding: "20px", borderLeft: "4px solid #6366f1" }}>
                    <span style={{ fontSize: "13px", color: "var(--muted)", textTransform: "uppercase", fontWeight: "600" }}>Today's Sales</span>
                    <h3 style={{ fontSize: "24px", margin: "8px 0 4px", color: "#6366f1" }}>{fmt(metrics.todaySales)}</h3>
                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Gross daily turnover</span>
                </div>

                <div className="card" style={{ padding: "20px", borderLeft: "4px solid #3b82f6" }}>
                    <span style={{ fontSize: "13px", color: "var(--muted)", textTransform: "uppercase", fontWeight: "600" }}>This Month</span>
                    <h3 style={{ fontSize: "24px", margin: "8px 0 4px", color: "#3b82f6" }}>{fmt(metrics.monthSales)}</h3>
                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Current calendar month</span>
                </div>

                <div className="card" style={{ padding: "20px", borderLeft: "4px solid #10b981" }}>
                    <span style={{ fontSize: "13px", color: "var(--muted)", textTransform: "uppercase", fontWeight: "600" }}>Total Collected</span>
                    <h3 style={{ fontSize: "24px", margin: "8px 0 4px", color: "#10b981" }}>{fmt(metrics.totalCollected)}</h3>
                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>Payments received</span>
                </div>

                <div className="card" style={{ padding: "20px", borderLeft: "4px solid #ef4444" }}>
                    <span style={{ fontSize: "13px", color: "var(--muted)", textTransform: "uppercase", fontWeight: "600" }}>Outstanding Due</span>
                    <h3 style={{ fontSize: "24px", margin: "8px 0 4px", color: "#ef4444" }}>{fmt(metrics.totalOutstanding)}</h3>
                    <span style={{ fontSize: "12px", color: "var(--muted)" }}>{metrics.customersWithOutstanding} customers with balance</span>
                </div>
            </div>

            {/* Invoices Status & GST Tax Breakdown */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "20px" }}>
                {/* Invoice Lifecycle Summary */}
                <div className="card" style={{ padding: "20px" }}>
                    <h4 style={{ margin: "0 0 16px", fontSize: "16px", fontWeight: "600" }}>Invoice Status Summary</h4>
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                        <div style={{ background: "var(--bg-subtle, #f8fafc)", padding: "12px", borderRadius: "8px" }}>
                            <div style={{ fontSize: "12px", color: "var(--muted)" }}>Total Invoices</div>
                            <div style={{ fontSize: "20px", fontWeight: "700" }}>{metrics.totalInvoices}</div>
                        </div>
                        <div style={{ background: "rgba(16, 185, 129, 0.1)", padding: "12px", borderRadius: "8px" }}>
                            <div style={{ fontSize: "12px", color: "#10b981", fontWeight: "600" }}>Paid Invoices</div>
                            <div style={{ fontSize: "20px", fontWeight: "700", color: "#10b981" }}>{metrics.paidInvoicesCount}</div>
                        </div>
                        <div style={{ background: "rgba(245, 158, 11, 0.1)", padding: "12px", borderRadius: "8px" }}>
                            <div style={{ fontSize: "12px", color: "#f59e0b", fontWeight: "600" }}>Unpaid / Issued</div>
                            <div style={{ fontSize: "20px", fontWeight: "700", color: "#f59e0b" }}>{metrics.unpaidInvoicesCount}</div>
                        </div>
                        <div style={{ background: "rgba(239, 68, 68, 0.1)", padding: "12px", borderRadius: "8px" }}>
                            <div style={{ fontSize: "12px", color: "#ef4444", fontWeight: "600" }}>Overdue Invoices</div>
                            <div style={{ fontSize: "20px", fontWeight: "700", color: "#ef4444" }}>{metrics.overdueInvoicesCount}</div>
                        </div>
                    </div>
                </div>

                {/* GST Tax Overview */}
                <div className="card" style={{ padding: "20px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                        <h4 style={{ margin: 0, fontSize: "16px", fontWeight: "600" }}>GST Tax Collected</h4>
                        <span style={{ fontWeight: "700", color: "#6366f1" }}>{fmt(metrics.totalTax)}</span>
                    </div>
                    <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 12px", background: "var(--bg-subtle, #f8fafc)", borderRadius: "6px" }}>
                            <span style={{ fontSize: "14px", color: "var(--muted)" }}>Central GST (CGST)</span>
                            <span style={{ fontWeight: "600" }}>{fmt(metrics.totalCgst)}</span>
                        </div>
                        <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 12px", background: "var(--bg-subtle, #f8fafc)", borderRadius: "6px" }}>
                            <span style={{ fontSize: "14px", color: "var(--muted)" }}>State GST (SGST)</span>
                            <span style={{ fontWeight: "600" }}>{fmt(metrics.totalSgst)}</span>
                        </div>
                        <div style={{ display: "flex", justifyContent: "space-between", padding: "8px 12px", background: "var(--bg-subtle, #f8fafc)", borderRadius: "6px" }}>
                            <span style={{ fontSize: "14px", color: "var(--muted)" }}>Integrated GST (IGST)</span>
                            <span style={{ fontWeight: "600" }}>{fmt(metrics.totalIgst)}</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Low-stock Alert banner if any */}
            {metrics.lowStockProductsCount > 0 && (
                <div style={{
                    padding: "16px",
                    background: "rgba(245, 158, 11, 0.15)",
                    border: "1px solid #f59e0b",
                    borderRadius: "8px",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center"
                }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <span style={{ fontSize: "20px" }}>⚠️</span>
                        <div>
                            <strong style={{ color: "#b45309" }}>Low Stock Alert:</strong> {metrics.lowStockProductsCount} item(s) are below re-order threshold!
                        </div>
                    </div>
                    <button className="btn btn-secondary btn-sm" onClick={() => onNavigate("inventory")}>
                        Manage Inventory
                    </button>
                </div>
            )}

            {/* Bottom Section: Top Selling Products & Recent Invoices */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "20px" }}>
                {/* Top Selling Products */}
                <div className="card" style={{ padding: "20px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
                        <h4 style={{ margin: 0, fontSize: "16px", fontWeight: "600" }}>Top Selling Products</h4>
                        <button className="btn-link" onClick={() => onNavigate("data")}>View All Products</button>
                    </div>
                    {metrics.topSellingProducts && metrics.topSellingProducts.length > 0 ? (
                        <table className="table" style={{ width: "100%", fontSize: "13px" }}>
                            <thead>
                                <tr>
                                    <th>Product</th>
                                    <th>Units Sold</th>
                                    <th style={{ textAlign: "right" }}>Revenue</th>
                                </tr>
                            </thead>
                            <tbody>
                                {metrics.topSellingProducts.map((p, idx) => (
                                    <tr key={idx}>
                                        <td style={{ fontWeight: "600" }}>{p.productName}</td>
                                        <td>{p.totalQuantity}</td>
                                        <td style={{ textAlign: "right", fontWeight: "600" }}>{fmt(p.totalRevenue)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : (
                        <p style={{ color: "var(--muted)", margin: "16px 0" }}>No product sales recorded yet.</p>
                    )}
                </div>

                {/* Recent Invoices */}
                <div className="card" style={{ padding: "20px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
                        <h4 style={{ margin: 0, fontSize: "16px", fontWeight: "600" }}>Recent Invoices</h4>
                        <button className="btn-link" onClick={() => onNavigate("invoices")}>View All Invoices</button>
                    </div>
                    {metrics.recentInvoices && metrics.recentInvoices.length > 0 ? (
                        <table className="table" style={{ width: "100%", fontSize: "13px" }}>
                            <thead>
                                <tr>
                                    <th>Invoice #</th>
                                    <th>Customer</th>
                                    <th>Amount</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {metrics.recentInvoices.map((inv) => (
                                    <tr key={inv.id} style={{ cursor: "pointer" }} onClick={() => onSelectInvoice(inv.id)}>
                                        <td style={{ fontWeight: "600", color: "var(--primary)" }}>{inv.invoiceNumber}</td>
                                        <td>{inv.customerName}</td>
                                        <td style={{ fontWeight: "600" }}>{fmt(inv.grandTotal)}</td>
                                        <td>
                                            <span className={`badge badge-${(inv.status || "DRAFT").toLowerCase()}`}>
                                                {inv.status}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    ) : (
                        <p style={{ color: "var(--muted)", margin: "16px 0" }}>No invoices created yet.</p>
                    )}
                </div>
            </div>
        </div>
    );
}
