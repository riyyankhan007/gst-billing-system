import React, { useEffect, useState } from "react";
import { getAnalytics, getDashboardMetrics } from "../services/api";

export default function Dashboard({ onNavigate, onSelectInvoice }) {
    const [range, setRange] = useState("MONTH");
    const [customStart, setCustomStart] = useState(() => {
        const d = new Date();
        d.setDate(d.getDate() - 30);
        return d.toISOString().split("T")[0];
    });
    const [customEnd, setCustomEnd] = useState(() => new Date().toISOString().split("T")[0]);
    const [data, setData] = useState(null);
    const [metrics, setMetrics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [hoverSales, setHoverSales] = useState(null);
    const [hoverGst, setHoverGst] = useState(null);

    const rangeLabels = {
        TODAY: "Today",
        "7D": "Last 7 Days",
        MONTH: "This Month",
        YEAR: "This Year",
        CUSTOM: "Custom Range"
    };

    useEffect(() => {
        loadAnalyticsData();
    }, [range]);

    async function loadAnalyticsData() {
        try {
            setLoading(true);
            setError("");
            const [analyticsRes, legacyMetrics] = await Promise.all([
                getAnalytics(range, range === "CUSTOM" ? customStart : null, range === "CUSTOM" ? customEnd : null),
                getDashboardMetrics().catch(() => null)
            ]);
            setData(analyticsRes);
            if (legacyMetrics) {
                setMetrics(legacyMetrics);
            }
        } catch (err) {
            setError(err.message || "Failed to load analytics dashboard");
        } finally {
            setLoading(false);
        }
    }

    const handleApplyCustom = (e) => {
        e.preventDefault();
        loadAnalyticsData();
    };

    const fmt = (val) => {
        const num = Number(val || 0);
        return new Intl.NumberFormat("en-IN", {
            style: "currency",
            currency: "INR",
            maximumFractionDigits: 2
        }).format(num);
    };

    const fmtShort = (val) => {
        const num = Number(val || 0);
        if (num >= 10000000) return `₹${(num / 10000000).toFixed(2)} Cr`;
        if (num >= 100000) return `₹${(num / 100000).toFixed(2)} L`;
        if (num >= 1000) return `₹${(num / 1000).toFixed(1)}k`;
        return `₹${num.toFixed(0)}`;
    };

    const formatTrendDate = (dateStr) => {
        if (!dateStr) return "";
        try {
            const parts = dateStr.split("-");
            if (parts.length === 3) {
                const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                return `${parseInt(parts[2], 10)} ${months[parseInt(parts[1], 10) - 1]}`;
            }
            return dateStr;
        } catch {
            return dateStr;
        }
    };

    if (loading && !data) {
        return (
            <div className="card loading-state" style={{ padding: "60px 20px", textAlign: "center" }}>
                <div className="spinner" style={{ margin: "0 auto 16px" }} />
                <h3 style={{ fontSize: "16px", color: "var(--text-primary)", fontWeight: 600 }}>Loading Business Analytics</h3>
                <p style={{ color: "var(--text-muted)", fontSize: "13px" }}>Aggregating multi-tenant GST data, sales records, and ledger balances...</p>
            </div>
        );
    }

    if (error && !data) {
        return (
            <div className="card" style={{ padding: "32px", textAlign: "center", border: "1px solid var(--danger)" }}>
                <h3 style={{ color: "var(--danger)", marginBottom: "8px" }}>Error Loading Analytics</h3>
                <p style={{ color: "var(--text-secondary)", marginBottom: "16px" }}>{error}</p>
                <button className="primary-button" onClick={loadAnalyticsData}>
                    Retry
                </button>
            </div>
        );
    }

    const sales = data?.sales || data?.salesSummary || {};
    const gst = data?.gst || data?.gstSummary || {};
    const invoices = data?.invoices || data?.invoiceSummary || {};
    const outstanding = data?.outstanding || data?.outstandingSummary || {};
    const topCustomers = data?.topCustomers || [];
    const topProducts = data?.topProducts || [];
    const salesTrend = data?.salesTrend || [];
    const gstTrend = data?.gstTrend || [];

    // SVG Chart Dimensions
    const svgWidth = 720;
    const svgHeight = 220;
    const paddingLeft = 60;
    const paddingRight = 24;
    const paddingTop = 20;
    const paddingBottom = 35;
    const plotWidth = svgWidth - paddingLeft - paddingRight;
    const plotHeight = svgHeight - paddingTop - paddingBottom;

    // Sales Trend Calculations
    const maxSales = Math.max(...salesTrend.map(d => Number(d.sales || 0)), 1000);
    const salesPoints = salesTrend.map((d, i) => {
        const x = salesTrend.length > 1
            ? paddingLeft + (i / (salesTrend.length - 1)) * plotWidth
            : paddingLeft + plotWidth / 2;
        const val = Math.max(0, Number(d.sales || 0));
        const y = paddingTop + plotHeight - Math.min(plotHeight, (val / maxSales) * plotHeight);
        return { x, y, ...d };
    });

    const salesPathD = salesPoints.length > 0
        ? `M ${salesPoints[0].x} ${salesPoints[0].y} ` + salesPoints.slice(1).map(p => `L ${p.x} ${p.y}`).join(" ")
        : "";

    const salesAreaD = salesPoints.length > 0
        ? `${salesPathD} L ${salesPoints[salesPoints.length - 1].x} ${paddingTop + plotHeight} L ${salesPoints[0].x} ${paddingTop + plotHeight} Z`
        : "";

    // GST Trend Calculations - dynamically compute scale and prevent division overflow
    const maxGst = Math.max(
        ...gstTrend.map(d => {
            const total = Number(d.totalGst || d.totalTax || 0);
            const sumParts = Number(d.cgst || 0) + Number(d.sgst || 0) + Number(d.igst || 0);
            return Math.max(total, sumParts);
        }),
        1000
    );

    return (
        <div className="dashboard-content" style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {/* Header & Date Range Filter Bar */}
            <div className="analytics-header">
                <div>
                    <span className="eyebrow">ENTERPRISE PERFORMANCE</span>
                    <h1 style={{ margin: "2px 0 6px", fontSize: "24px", fontWeight: "800", color: "var(--text-primary)", letterSpacing: "-0.5px" }}>
                        Business Analytics
                    </h1>
                    <p style={{ margin: 0, color: "var(--text-muted)", fontSize: "13.5px" }}>
                        Real-time tenant isolated revenue tracking, GST tax liability, receivables, and customer metrics.
                    </p>
                </div>

                {/* Date-range filters */}
                <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
                    <div className="analytics-range-selector">
                        {["TODAY", "7D", "MONTH", "YEAR", "CUSTOM"].map((rKey) => (
                            <button
                                key={rKey}
                                type="button"
                                className={`range-tab-btn ${range === rKey ? "active" : ""}`}
                                onClick={() => setRange(rKey)}
                            >
                                {rangeLabels[rKey]}
                            </button>
                        ))}
                    </div>

                    {range === "CUSTOM" && (
                        <form onSubmit={handleApplyCustom} className="custom-date-bar">
                            <input
                                type="date"
                                className="custom-date-input"
                                value={customStart}
                                onChange={e => setCustomStart(e.target.value)}
                            />
                            <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>to</span>
                            <input
                                type="date"
                                className="custom-date-input"
                                value={customEnd}
                                onChange={e => setCustomEnd(e.target.value)}
                            />
                            <button type="submit" className="action-btn-sm primary-button" style={{ padding: "4px 10px", fontSize: "11.5px" }}>
                                Apply
                            </button>
                        </form>
                    )}
                </div>
            </div>

            {/* 4 Core Metric KPI Summary Cards */}
            <div className="analytics-kpi-grid">
                {/* 1. Total Sales */}
                <div className="analytics-kpi-card">
                    <div className="kpi-stripe" style={{ backgroundColor: "#2563eb" }} />
                    <div className="kpi-title">Total Sales ({rangeLabels[range]})</div>
                    <div className="kpi-main-val" style={{ color: "#1e40af" }}>
                        {fmt(sales.selectedPeriod ?? sales.selectedPeriodSales)}
                    </div>
                    <div className="kpi-sub-row">
                        <span style={{ fontWeight: 600, color: "#334155" }}>
                            {invoices.total ?? invoices.totalCount ?? 0} Invoices
                        </span>
                        <span style={{ color: "#cbd5e1" }}>•</span>
                        <span>Today: <strong>{fmtShort(sales.today ?? sales.todaySales)}</strong></span>
                        <span style={{ color: "#cbd5e1" }}>•</span>
                        <span>This Mo: <strong>{fmtShort(sales.thisMonth ?? sales.monthSales)}</strong></span>
                    </div>
                </div>

                {/* 2. Total GST Collected */}
                <div className="analytics-kpi-card">
                    <div className="kpi-stripe" style={{ backgroundColor: "#8b5cf6" }} />
                    <div className="kpi-title">GST Tax Liability</div>
                    <div className="kpi-main-val" style={{ color: "#6d28d9" }}>
                        {fmt(gst.totalGst ?? gst.totalTax)}
                    </div>
                    <div className="kpi-sub-row" style={{ gap: "6px" }}>
                        <span style={{ padding: "2px 6px", background: "#f5f3ff", color: "#6d28d9", borderRadius: "4px", fontWeight: 600, fontSize: "11px" }}>
                            CGST: {fmtShort(gst.cgst ?? gst.totalCgst)}
                        </span>
                        <span style={{ padding: "2px 6px", background: "#ede9fe", color: "#5b21b6", borderRadius: "4px", fontWeight: 600, fontSize: "11px" }}>
                            SGST: {fmtShort(gst.sgst ?? gst.totalSgst)}
                        </span>
                        <span style={{ padding: "2px 6px", background: "#fdf2f8", color: "#be185d", borderRadius: "4px", fontWeight: 600, fontSize: "11px" }}>
                            IGST: {fmtShort(gst.igst ?? gst.totalIgst)}
                        </span>
                    </div>
                </div>

                {/* 3. Total Invoices Breakdown */}
                <div className="analytics-kpi-card">
                    <div className="kpi-stripe" style={{ backgroundColor: "#10b981" }} />
                    <div className="kpi-title">Total Invoices</div>
                    <div className="kpi-main-val" style={{ color: "#065f46" }}>
                        {invoices.total ?? invoices.totalCount ?? 0}
                    </div>
                    <div className="kpi-sub-row">
                        <span style={{ color: "var(--success-text)", fontWeight: 600 }}>
                            {invoices.paid ?? invoices.paidCount ?? 0} Paid
                        </span>
                        <span style={{ color: "#cbd5e1" }}>•</span>
                        <span style={{ color: "#b45309", fontWeight: 600 }}>
                            {invoices.pending ?? invoices.pendingCount ?? 0} Pending
                        </span>
                        <span style={{ color: "#cbd5e1" }}>•</span>
                        <span style={{ color: "#94a3b8" }}>
                            {invoices.cancelled ?? invoices.cancelledCount ?? 0} Cancelled
                        </span>
                    </div>
                </div>

                {/* 4. Outstanding & Overdue Amount */}
                <div className="analytics-kpi-card">
                    <div className="kpi-stripe" style={{ backgroundColor: "#ef4444" }} />
                    <div className="kpi-title">Receivables & Overdue</div>
                    <div className="kpi-main-val" style={{ color: "#991b1b" }}>
                        {fmt(outstanding.unpaidAmount ?? outstanding.totalOutstanding)}
                    </div>
                    <div className="kpi-sub-row">
                        <span style={{ color: "#dc2626", fontWeight: 600 }}>
                            {fmtShort(outstanding.overdueAmount ?? outstanding.overdueOutstanding)} Overdue
                        </span>
                        <span style={{ color: "#cbd5e1" }}>•</span>
                        <span style={{ color: "var(--text-muted)" }}>
                            {outstanding.overdueCount ?? outstanding.overdueInvoicesCount ?? 0} overdue invoices
                        </span>
                    </div>
                </div>
            </div>

            {/* Charts Row: Sales Trend + GST Trend */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(340px, 1fr))", gap: "20px" }}>
                {/* 1. Sales Trend Area Chart */}
                <div className="chart-card">
                    <div className="chart-header">
                        <div>
                            <div className="chart-title">Sales Trend ({rangeLabels[range]})</div>
                            <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "2px" }}>
                                Daily and aggregate turnover with timeline progression
                            </div>
                        </div>
                        <div className="chart-legend">
                            <span><span className="legend-dot" style={{ backgroundColor: "#2563eb" }} />Daily Sales</span>
                        </div>
                    </div>

                    <div className="chart-container-rel">
                        {hoverSales && (
                            <div
                                className="chart-tooltip"
                                style={{
                                    left: `${(hoverSales.x / svgWidth) * 100}%`,
                                    top: `${(hoverSales.y / svgHeight) * 100}%`
                                }}
                            >
                                <div style={{ fontWeight: 700, marginBottom: "2px" }}>{formatTrendDate(hoverSales.date)}</div>
                                <div>Sales: <strong style={{ color: "#93c5fd" }}>{fmt(hoverSales.sales)}</strong></div>
                                <div style={{ fontSize: "11px", color: "#94a3b8" }}>Invoices: {hoverSales.invoiceCount}</div>
                            </div>
                        )}

                        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="chart-svg">
                            <defs>
                                <linearGradient id="salesGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                                    <stop offset="0%" stopColor="#2563eb" stopOpacity="0.28" />
                                    <stop offset="100%" stopColor="#2563eb" stopOpacity="0.0" />
                                </linearGradient>
                            </defs>

                            {/* Horizontal Gridlines */}
                            {[0, 0.25, 0.5, 0.75, 1].map((pct, idx) => {
                                const yPos = paddingTop + plotHeight * (1 - pct);
                                const gridVal = maxSales * pct;
                                return (
                                    <g key={idx}>
                                        <line
                                            x1={paddingLeft}
                                            y1={yPos}
                                            x2={svgWidth - paddingRight}
                                            y2={yPos}
                                            stroke="#f1f5f9"
                                            strokeDasharray={idx === 0 ? "none" : "3 3"}
                                        />
                                        <text
                                            x={paddingLeft - 8}
                                            y={yPos + 4}
                                            textAnchor="end"
                                            fontSize="10"
                                            fill="#94a3b8"
                                        >
                                            {fmtShort(gridVal)}
                                        </text>
                                    </g>
                                );
                            })}

                            {/* Area Fill */}
                            {salesAreaD && (
                                <path
                                    d={salesAreaD}
                                    fill="url(#salesGrad)"
                                    style={{ transition: "all 0.4s ease" }}
                                />
                            )}

                            {/* Smooth Line */}
                            {salesPathD && (
                                <path
                                    d={salesPathD}
                                    fill="none"
                                    stroke="#2563eb"
                                    strokeWidth="2.5"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    style={{ transition: "all 0.4s ease" }}
                                />
                            )}

                            {/* Interactive Data Points */}
                            {salesPoints.map((pt, i) => (
                                <circle
                                    key={i}
                                    cx={pt.x}
                                    cy={pt.y}
                                    r={hoverSales?.date === pt.date ? 6 : 4}
                                    fill="#ffffff"
                                    stroke="#2563eb"
                                    strokeWidth="2.5"
                                    className="chart-data-point"
                                    onMouseEnter={() => setHoverSales(pt)}
                                    onMouseLeave={() => setHoverSales(null)}
                                />
                            ))}

                            {/* X-axis Date Labels */}
                            {salesPoints.length > 0 &&
                                salesPoints
                                    .filter((_, idx) => {
                                        if (salesPoints.length <= 8) return true;
                                        const step = Math.ceil(salesPoints.length / 7);
                                        return idx % step === 0 || idx === salesPoints.length - 1;
                                    })
                                    .map((pt, idx) => (
                                        <text
                                            key={idx}
                                            x={pt.x}
                                            y={svgHeight - 10}
                                            textAnchor="middle"
                                            fontSize="10"
                                            fill="#64748b"
                                        >
                                            {formatTrendDate(pt.date)}
                                        </text>
                                    ))}
                        </svg>
                    </div>
                </div>

                {/* 2. GST Trend Breakdown Bar Chart */}
                <div className="chart-card">
                    <div className="chart-header">
                        <div>
                            <div className="chart-title">GST Collected Over Time</div>
                            <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "2px" }}>
                                Daily and period tax liability distributed by GST components
                            </div>
                        </div>
                        <div className="chart-legend">
                            <span><span className="legend-dot" style={{ backgroundColor: "#4f46e5" }} />CGST</span>
                            <span><span className="legend-dot" style={{ backgroundColor: "#9333ea" }} />SGST</span>
                            <span><span className="legend-dot" style={{ backgroundColor: "#ec4899" }} />IGST</span>
                        </div>
                    </div>

                    <div className="chart-container-rel">
                        {hoverGst && (
                            <div
                                className="chart-tooltip"
                                style={{
                                    left: `${(hoverGst.x / svgWidth) * 100}%`,
                                    top: `${(hoverGst.y / svgHeight) * 100}%`
                                }}
                            >
                                <div style={{ fontWeight: 700, marginBottom: "2px" }}>{formatTrendDate(hoverGst.item.date)}</div>
                                <div>Total Tax: <strong>{fmt(hoverGst.item.totalGst ?? hoverGst.item.totalTax)}</strong></div>
                                <div style={{ fontSize: "11px", color: "#c7d2fe" }}>CGST: {fmt(hoverGst.item.cgst)}</div>
                                <div style={{ fontSize: "11px", color: "#e9d5ff" }}>SGST: {fmt(hoverGst.item.sgst)}</div>
                                <div style={{ fontSize: "11px", color: "#fbcfe8" }}>IGST: {fmt(hoverGst.item.igst)}</div>
                            </div>
                        )}

                        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="chart-svg">
                            {/* Horizontal Gridlines */}
                            {[0, 0.25, 0.5, 0.75, 1].map((pct, idx) => {
                                const yPos = paddingTop + plotHeight * (1 - pct);
                                const gridVal = maxGst * pct;
                                return (
                                    <g key={idx}>
                                        <line
                                            x1={paddingLeft}
                                            y1={yPos}
                                            x2={svgWidth - paddingRight}
                                            y2={yPos}
                                            stroke="#f1f5f9"
                                            strokeDasharray={idx === 0 ? "none" : "3 3"}
                                        />
                                        <text
                                            x={paddingLeft - 8}
                                            y={yPos + 4}
                                            textAnchor="end"
                                            fontSize="10"
                                            fill="#94a3b8"
                                        >
                                            {fmtShort(gridVal)}
                                        </text>
                                    </g>
                                );
                            })}

                            {/* Stacked Bars */}
                            {gstTrend.map((d, i) => {
                                const barGroupWidth = plotWidth / (gstTrend.length || 1);
                                const barW = Math.max(Math.min(barGroupWidth * 0.55, 36), 6);
                                const xCenter = paddingLeft + (i + 0.5) * barGroupWidth;
                                const x = xCenter - barW / 2;

                                const cgstVal = Math.max(0, Number(d.cgst || 0));
                                const sgstVal = Math.max(0, Number(d.sgst || 0));
                                const igstVal = Math.max(0, Number(d.igst || 0));

                                const cgstH = Math.max(0, Math.min(plotHeight, (cgstVal / maxGst) * plotHeight));
                                const sgstH = Math.max(0, Math.min(plotHeight - cgstH, (sgstVal / maxGst) * plotHeight));
                                const igstH = Math.max(0, Math.min(plotHeight - cgstH - sgstH, (igstVal / maxGst) * plotHeight));

                                const baseY = paddingTop + plotHeight;
                                const cgstY = baseY - cgstH;
                                const sgstY = cgstY - sgstH;
                                const igstY = sgstY - igstH;

                                const itemTotal = Number(d.totalGst ?? d.totalTax ?? (cgstVal + sgstVal + igstVal));

                                return (
                                    <g
                                        key={i}
                                        className="chart-bar"
                                        onMouseEnter={() => setHoverGst({
                                            x: xCenter,
                                            y: Math.max(paddingTop, Math.min(igstY, baseY - 20)),
                                            item: { ...d, totalTax: itemTotal, totalGst: itemTotal, cgst: cgstVal, sgst: sgstVal, igst: igstVal }
                                        })}
                                        onMouseLeave={() => setHoverGst(null)}
                                    >
                                        {/* CGST */}
                                        {cgstH > 0 && (
                                            <rect
                                                x={x}
                                                y={cgstY}
                                                width={barW}
                                                height={cgstH}
                                                fill="#4f46e5"
                                                rx={sgstH === 0 && igstH === 0 ? "3" : "0"}
                                            />
                                        )}
                                        {/* SGST */}
                                        {sgstH > 0 && (
                                            <rect
                                                x={x}
                                                y={sgstY}
                                                width={barW}
                                                height={sgstH}
                                                fill="#9333ea"
                                                rx={igstH === 0 ? "3" : "0"}
                                            />
                                        )}
                                        {/* IGST */}
                                        {igstH > 0 && (
                                            <rect
                                                x={x}
                                                y={igstY}
                                                width={barW}
                                                height={igstH}
                                                fill="#ec4899"
                                                rx="3"
                                            />
                                        )}
                                    </g>
                                );
                            })}

                            {/* X-axis Labels */}
                            {gstTrend.length > 0 &&
                                gstTrend
                                    .filter((_, idx) => {
                                        if (gstTrend.length <= 8) return true;
                                        const step = Math.ceil(gstTrend.length / 7);
                                        return idx % step === 0 || idx === gstTrend.length - 1;
                                    })
                                    .map((d, idx) => {
                                        const barGroupWidth = plotWidth / (gstTrend.length || 1);
                                        const xCenter = paddingLeft + (idx + 0.5) * barGroupWidth;
                                        return (
                                            <text
                                                key={idx}
                                                x={xCenter}
                                                y={svgHeight - 10}
                                                textAnchor="middle"
                                                fontSize="10"
                                                fill="#64748b"
                                            >
                                                {formatTrendDate(d.date)}
                                            </text>
                                        );
                                    })}
                        </svg>
                    </div>
                </div>
            </div>

            {/* Leaderboards: Top Customers & Top Products */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "20px" }}>
                {/* 1. Top Customers */}
                <div className="card" style={{ padding: "22px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                        <div>
                            <h3 style={{ margin: 0, fontSize: "16px", fontWeight: "700" }}>Top Customers</h3>
                            <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Ranked by total invoice volume</span>
                        </div>
                        <button
                            type="button"
                            className="btn-link"
                            onClick={() => onNavigate("customers")}
                        >
                            View Directory →
                        </button>
                    </div>

                    {topCustomers.length === 0 ? (
                        <div style={{ padding: "32px 0", textAlign: "center", color: "var(--text-muted)", fontSize: "13px" }}>
                            No customer purchase data recorded for this period.
                        </div>
                    ) : (
                        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            {topCustomers.map((cust, idx) => {
                                const rankClass = idx === 0 ? "rank-1" : idx === 1 ? "rank-2" : idx === 2 ? "rank-3" : "rank-other";
                                const topVal = Number(topCustomers[0]?.totalPurchaseValue || 1);
                                const pct = Math.min(100, Math.round((Number(cust.totalPurchaseValue || 0) / topVal) * 100));

                                return (
                                    <div key={cust.customerId || idx}>
                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "10px" }}>
                                            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                                                <span className={`rank-badge ${rankClass}`}>{idx + 1}</span>
                                                <div>
                                                    <strong style={{ fontSize: "13.5px", color: "var(--text-primary)" }}>{cust.customerName}</strong>
                                                    {cust.gstin && (
                                                        <span style={{ display: "block", fontSize: "11px", color: "var(--text-muted)" }}>
                                                            GSTIN: {cust.gstin}
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                            <div style={{ textAlign: "right" }}>
                                                <div style={{ fontWeight: "700", color: "#0f172a", fontSize: "14px" }}>
                                                    {fmt(cust.totalPurchaseValue)}
                                                </div>
                                                <span style={{ fontSize: "11.5px", color: "var(--text-muted)" }}>
                                                    {cust.invoiceCount} invoice{cust.invoiceCount !== 1 ? "s" : ""}
                                                </span>
                                            </div>
                                        </div>
                                        <div className="progress-track">
                                            <div
                                                className="progress-fill"
                                                style={{
                                                    width: `${pct}%`,
                                                    backgroundColor: idx === 0 ? "#2563eb" : idx === 1 ? "#3b82f6" : "#60a5fa"
                                                }}
                                            />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* 2. Top Products */}
                <div className="card" style={{ padding: "22px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                        <div>
                            <h3 style={{ margin: 0, fontSize: "16px", fontWeight: "700" }}>Top Products & Services</h3>
                            <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Ranked by billed revenue</span>
                        </div>
                        <button
                            type="button"
                            className="btn-link"
                            onClick={() => onNavigate("products")}
                        >
                            View Catalog →
                        </button>
                    </div>

                    {topProducts.length === 0 ? (
                        <div style={{ padding: "32px 0", textAlign: "center", color: "var(--text-muted)", fontSize: "13px" }}>
                            No product sales recorded for this period.
                        </div>
                    ) : (
                        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                            {topProducts.map((prod, idx) => {
                                const rankClass = idx === 0 ? "rank-1" : idx === 1 ? "rank-2" : idx === 2 ? "rank-3" : "rank-other";
                                const topRev = Number(topProducts[0]?.totalRevenue || 1);
                                const pct = Math.min(100, Math.round((Number(prod.totalRevenue || 0) / topRev) * 100));

                                return (
                                    <div key={prod.productId || idx}>
                                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "10px" }}>
                                            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                                                <span className={`rank-badge ${rankClass}`}>{idx + 1}</span>
                                                <div>
                                                    <strong style={{ fontSize: "13.5px", color: "var(--text-primary)" }}>{prod.productName}</strong>
                                                    {prod.sku && (
                                                        <span style={{ display: "block", fontSize: "11px", color: "var(--text-muted)" }}>
                                                            SKU: {prod.sku}
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                            <div style={{ textAlign: "right" }}>
                                                <div style={{ fontWeight: "700", color: "#0f172a", fontSize: "14px" }}>
                                                    {fmt(prod.totalRevenue)}
                                                </div>
                                                <span style={{ fontSize: "11.5px", color: "var(--text-muted)" }}>
                                                    {prod.totalQuantity ?? prod.totalQuantitySold ?? 0} unit{(prod.totalQuantity ?? prod.totalQuantitySold) !== 1 ? "s" : ""} sold
                                                </span>
                                            </div>
                                        </div>
                                        <div className="progress-track">
                                            <div
                                                className="progress-fill"
                                                style={{
                                                    width: `${pct}%`,
                                                    backgroundColor: idx === 0 ? "#10b981" : idx === 1 ? "#34d399" : "#6ee7b7"
                                                }}
                                            />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>

            {/* Recent Invoices Table */}
            {metrics?.recentInvoices && metrics.recentInvoices.length > 0 && (
                <div className="table-card" style={{ marginTop: "4px" }}>
                    <div className="table-header-card">
                        <div>
                            <h3 style={{ margin: 0, fontSize: "16px", fontWeight: 700 }}>Recent Transactions</h3>
                            <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>Latest billing invoices generated for customers</span>
                        </div>
                        <button
                            type="button"
                            className="primary-button action-btn-sm"
                            onClick={() => onNavigate("invoices")}
                        >
                            View All Invoices
                        </button>
                    </div>
                    <div className="table-container">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Invoice #</th>
                                    <th>Customer</th>
                                    <th style={{ textAlign: "right" }}>Total Amount</th>
                                    <th style={{ textAlign: "center" }}>Status</th>
                                    <th style={{ textAlign: "right" }}>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {metrics.recentInvoices.map((inv) => (
                                    <tr key={inv.id} style={{ cursor: "pointer" }} onClick={() => onSelectInvoice(inv.id)}>
                                        <td style={{ fontWeight: "600", color: "var(--primary)" }}>
                                            {inv.invoiceNumber}
                                        </td>
                                        <td>
                                            <strong>{inv.customerName || "—"}</strong>
                                        </td>
                                        <td className="table-num" style={{ textAlign: "right" }}>
                                            {fmt(inv.grandTotal)}
                                        </td>
                                        <td style={{ textAlign: "center" }}>
                                            <span className={`status-badge status-${String(inv.status || "DRAFT").toLowerCase()}`}>
                                                {inv.status}
                                            </span>
                                        </td>
                                        <td style={{ textAlign: "right" }}>
                                            <button
                                                type="button"
                                                className="action-btn-sm"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    onSelectInvoice(inv.id);
                                                }}
                                            >
                                                View
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}
