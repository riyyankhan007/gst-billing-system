import React, { useEffect, useState } from "react";
import Icon from "../components/Icon";
import { getAnalytics, getDashboardMetrics } from "../services/api";
import { getChartTooltipPosition, calculateTaxPercentages } from "../utils/chartTooltip";
import { Skeleton, SkeletonKpi, SkeletonTable } from "../components/ui/Skeleton";
import ErrorState from "../components/ui/ErrorState";

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

    useEffect(() => {
        const handleBizSwitch = () => {
            loadAnalyticsData();
        };
        window.addEventListener("business-switched", handleBizSwitch);
        return () => window.removeEventListener("business-switched", handleBizSwitch);
    }, [range, customStart, customEnd]);

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
            <div style={{ display: "flex", flexDirection: "column", gap: "24px" }} aria-busy="true">
                <div className="analytics-header">
                    <div>
                        <Skeleton width="130px" height="12px" style={{ marginBottom: "8px" }} />
                        <Skeleton width="260px" height="28px" style={{ marginBottom: "8px" }} />
                        <Skeleton width="420px" height="14px" />
                    </div>
                    <div style={{ display: "flex", gap: "8px" }}>
                        <Skeleton width="280px" height="36px" borderRadius="var(--radius-sm, 6px)" />
                    </div>
                </div>

                <div className="analytics-kpi-grid">
                    <SkeletonKpi stripeColor="#2563eb" />
                    <SkeletonKpi stripeColor="#8b5cf6" />
                    <SkeletonKpi stripeColor="#10b981" />
                    <SkeletonKpi stripeColor="#ef4444" />
                </div>

                <div className="dashboard-charts-grid">
                    <div className="chart-card" style={{ padding: "20px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "16px" }}>
                            <Skeleton width="180px" height="18px" />
                            <Skeleton width="90px" height="14px" />
                        </div>
                        <Skeleton width="100%" height="220px" borderRadius="8px" />
                    </div>
                    <div className="chart-card" style={{ padding: "20px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "16px" }}>
                            <Skeleton width="180px" height="18px" />
                            <Skeleton width="90px" height="14px" />
                        </div>
                        <Skeleton width="100%" height="220px" borderRadius="8px" />
                    </div>
                </div>

                <div className="dashboard-leaderboards-grid">
                    <div className="table-card" style={{ padding: "18px 20px" }}>
                        <Skeleton width="180px" height="18px" style={{ marginBottom: "14px" }} />
                        <SkeletonTable columns={3} rows={4} showHeader={false} />
                    </div>
                    <div className="table-card" style={{ padding: "18px 20px" }}>
                        <Skeleton width="180px" height="18px" style={{ marginBottom: "14px" }} />
                        <SkeletonTable columns={3} rows={4} showHeader={false} />
                    </div>
                    <div className="table-card" style={{ padding: "18px 20px" }}>
                        <Skeleton width="180px" height="18px" style={{ marginBottom: "14px" }} />
                        <SkeletonTable columns={3} rows={4} showHeader={false} />
                    </div>
                </div>
            </div>
        );
    }

    if (error && !data) {
        return (
            <ErrorState
                title="Failed to load analytics dashboard"
                message={error}
                onRetry={loadAnalyticsData}
            />
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
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
                        <h1 style={{ margin: "2px 0 6px", fontSize: "24px", fontWeight: "800", color: "var(--text-primary)", letterSpacing: "-0.5px" }}>
                            Business Analytics
                        </h1>
                        {loading && data && (
                            <span className="refresh-indicator">
                                <span
                                    className="inline-spinner"
                                    style={{
                                        width: "10px",
                                        height: "10px",
                                        border: "2px solid #2563eb",
                                        borderTopColor: "transparent"
                                    }}
                                />
                                Updating...
                            </span>
                        )}
                    </div>
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
            <div className="dashboard-charts-grid">
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
                        {hoverSales && (() => {
                            const { xPct, yPct, className } = getChartTooltipPosition(
                                hoverSales.x,
                                hoverSales.y,
                                svgWidth,
                                svgHeight
                            );

                            return (
                                <div
                                    className={className}
                                    style={{
                                        left: `${xPct}%`,
                                        top: `${yPct}%`
                                    }}
                                    role="tooltip"
                                    aria-hidden="false"
                                >
                                    <div style={{ fontWeight: 700, marginBottom: "4px", fontSize: "12px", borderBottom: "1px solid rgba(255, 255, 255, 0.14)", paddingBottom: "4px" }}>
                                        {formatTrendDate(hoverSales.date)}
                                    </div>
                                    <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", fontSize: "11.5px" }}>
                                        <span style={{ color: "#94a3b8" }}>Turnover:</span>
                                        <strong style={{ color: "#93c5fd" }}>{fmt(hoverSales.sales)}</strong>
                                    </div>
                                    <div style={{ display: "flex", justifyContent: "space-between", gap: "12px", fontSize: "11px", color: "#cbd5e1", marginTop: "2px" }}>
                                        <span>Invoices:</span>
                                        <strong style={{ color: "#ffffff" }}>{hoverSales.invoiceCount ?? 0}</strong>
                                    </div>
                                </div>
                            );
                        })()}

                        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="chart-svg" onMouseLeave={() => setHoverSales(null)}>
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

                            {/* Interactive Data Points with larger touch target & keyboard accessibility */}
                            {salesPoints.map((pt, i) => (
                                <g key={i}>
                                    <circle
                                        cx={pt.x}
                                        cy={pt.y}
                                        r={16}
                                        fill="transparent"
                                        style={{ cursor: "pointer", outline: "none" }}
                                        tabIndex={0}
                                        role="button"
                                        aria-label={`Sales for ${pt.date}: ${fmt(pt.sales)}, ${pt.invoiceCount || 0} invoices`}
                                        onMouseEnter={() => setHoverSales(pt)}
                                        onFocus={() => setHoverSales(pt)}
                                        onBlur={() => setHoverSales(null)}
                                        onClick={() => setHoverSales(prev => prev?.date === pt.date ? null : pt)}
                                    />
                                    <circle
                                        cx={pt.x}
                                        cy={pt.y}
                                        r={hoverSales?.date === pt.date ? 6 : 4}
                                        fill="#ffffff"
                                        stroke="#2563eb"
                                        strokeWidth="2.5"
                                        className="chart-data-point"
                                        style={{ pointerEvents: "none" }}
                                    />
                                </g>
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
                        {hoverGst && (() => {
                            const { xPct, yPct, className } = getChartTooltipPosition(
                                hoverGst.x,
                                hoverGst.y,
                                svgWidth,
                                svgHeight
                            );

                            const itemTotal = Number(hoverGst.item.totalGst ?? hoverGst.item.totalTax ?? 0);
                            const cgstVal = Number(hoverGst.item.cgst || 0);
                            const sgstVal = Number(hoverGst.item.sgst || 0);
                            const igstVal = Number(hoverGst.item.igst || 0);
                            const { cgstPct, sgstPct, igstPct } = calculateTaxPercentages(cgstVal, sgstVal, igstVal, itemTotal);

                            return (
                                <div
                                    className={className}
                                    style={{
                                        left: `${xPct}%`,
                                        top: `${yPct}%`
                                    }}
                                    role="tooltip"
                                    aria-hidden="false"
                                >
                                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "14px", marginBottom: "6px", borderBottom: "1px solid rgba(255, 255, 255, 0.14)", paddingBottom: "5px" }}>
                                        <span style={{ fontWeight: 700, fontSize: "12px", color: "#f8fafc" }}>
                                            {formatTrendDate(hoverGst.item.date)}
                                        </span>
                                        <span style={{ fontSize: "11px", color: "#94a3b8" }}>
                                            Total: <strong style={{ color: "#ffffff", fontSize: "12px" }}>{fmt(itemTotal)}</strong>
                                        </span>
                                    </div>

                                    <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                                        {/* CGST */}
                                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px", fontSize: "11.5px" }}>
                                            <span style={{ display: "flex", alignItems: "center", gap: "6px", color: "#c7d2fe" }}>
                                                <span style={{ width: "8px", height: "8px", borderRadius: "2px", backgroundColor: "#4f46e5", display: "inline-block", flexShrink: 0 }} />
                                                <span>CGST ({cgstPct}%)</span>
                                            </span>
                                            <span style={{ fontWeight: 600, color: "#ffffff", fontFamily: "var(--font-mono, monospace)", fontSize: "11px" }}>
                                                {fmt(cgstVal)}
                                            </span>
                                        </div>

                                        {/* SGST */}
                                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px", fontSize: "11.5px" }}>
                                            <span style={{ display: "flex", alignItems: "center", gap: "6px", color: "#e9d5ff" }}>
                                                <span style={{ width: "8px", height: "8px", borderRadius: "2px", backgroundColor: "#9333ea", display: "inline-block", flexShrink: 0 }} />
                                                <span>SGST ({sgstPct}%)</span>
                                            </span>
                                            <span style={{ fontWeight: 600, color: "#ffffff", fontFamily: "var(--font-mono, monospace)", fontSize: "11px" }}>
                                                {fmt(sgstVal)}
                                            </span>
                                        </div>

                                        {/* IGST */}
                                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px", fontSize: "11.5px" }}>
                                            <span style={{ display: "flex", alignItems: "center", gap: "6px", color: "#fbcfe8" }}>
                                                <span style={{ width: "8px", height: "8px", borderRadius: "2px", backgroundColor: "#ec4899", display: "inline-block", flexShrink: 0 }} />
                                                <span>IGST ({igstPct}%)</span>
                                            </span>
                                            <span style={{ fontWeight: 600, color: "#ffffff", fontFamily: "var(--font-mono, monospace)", fontSize: "11px" }}>
                                                {fmt(igstVal)}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            );
                        })()}

                        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="chart-svg" onMouseLeave={() => setHoverGst(null)}>
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
                                const totalBarH = cgstH + sgstH + igstH;
                                const barTopY = totalBarH > 0 ? igstY : baseY - 10;
                                const isHovered = hoverGst?.item?.date === d.date;

                                return (
                                    <g key={i} className="chart-bar-group">
                                        {/* Column hover highlight */}
                                        {isHovered && (
                                            <rect
                                                x={paddingLeft + i * barGroupWidth + 1}
                                                y={paddingTop}
                                                width={Math.max(1, barGroupWidth - 2)}
                                                height={plotHeight}
                                                fill="#f1f5f9"
                                                opacity="0.75"
                                                rx="4"
                                                style={{ pointerEvents: "none" }}
                                            />
                                        )}

                                        {/* Baseline indicator for 0 liability */}
                                        {totalBarH === 0 && (
                                            <rect
                                                x={x}
                                                y={baseY - 2}
                                                width={barW}
                                                height={2}
                                                fill="#cbd5e1"
                                                rx="1"
                                                style={{ pointerEvents: "none" }}
                                            />
                                        )}

                                        {/* CGST */}
                                        {cgstH > 0 && (
                                            <rect
                                                x={x}
                                                y={cgstY}
                                                width={barW}
                                                height={cgstH}
                                                fill="#4f46e5"
                                                rx={sgstH === 0 && igstH === 0 ? "3" : "0"}
                                                style={{ pointerEvents: "none" }}
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
                                                style={{ pointerEvents: "none" }}
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
                                                style={{ pointerEvents: "none" }}
                                            />
                                        )}

                                        {/* Accessible full-column interactive hit area */}
                                        <rect
                                            x={paddingLeft + i * barGroupWidth}
                                            y={paddingTop}
                                            width={barGroupWidth}
                                            height={plotHeight}
                                            fill="transparent"
                                            style={{ cursor: "pointer", outline: "none" }}
                                            tabIndex={0}
                                            role="button"
                                            aria-label={`GST for ${d.date}: Total ${fmt(itemTotal)}, CGST ${fmt(cgstVal)}, SGST ${fmt(sgstVal)}, IGST ${fmt(igstVal)}`}
                                            onMouseEnter={() => setHoverGst({
                                                x: xCenter,
                                                y: barTopY,
                                                item: { ...d, totalTax: itemTotal, totalGst: itemTotal, cgst: cgstVal, sgst: sgstVal, igst: igstVal }
                                            })}
                                            onFocus={() => setHoverGst({
                                                x: xCenter,
                                                y: barTopY,
                                                item: { ...d, totalTax: itemTotal, totalGst: itemTotal, cgst: cgstVal, sgst: sgstVal, igst: igstVal }
                                            })}
                                            onBlur={() => setHoverGst(null)}
                                            onClick={() => setHoverGst(prev => prev?.item?.date === d.date ? null : {
                                                x: xCenter,
                                                y: barTopY,
                                                item: { ...d, totalTax: itemTotal, totalGst: itemTotal, cgst: cgstVal, sgst: sgstVal, igst: igstVal }
                                            })}
                                        />
                                    </g>
                                );
                            })}

                            {/* X-axis Labels */}
                            {gstTrend.length > 0 &&
                                gstTrend
                                    .map((d, origIdx) => ({ d, origIdx }))
                                    .filter(({ origIdx }) => {
                                        if (gstTrend.length <= 8) return true;
                                        const step = Math.ceil(gstTrend.length / 7);
                                        return origIdx % step === 0 || origIdx === gstTrend.length - 1;
                                    })
                                    .map(({ d, origIdx }) => {
                                        const barGroupWidth = plotWidth / (gstTrend.length || 1);
                                        const xCenter = paddingLeft + (origIdx + 0.5) * barGroupWidth;
                                        return (
                                            <text
                                                key={origIdx}
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
            <div className="dashboard-leaderboards-grid">
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
                            style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}
                            onClick={() => onNavigate("customers")}
                        >
                            View Directory <Icon type="arrowRight" size={13} />
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
                            style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}
                            onClick={() => onNavigate("products")}
                        >
                            View Catalog <Icon type="arrowRight" size={13} />
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
