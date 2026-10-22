import React, { useEffect, useState } from "react";
import Icon from "../components/Icon";
import { getAuditLogs } from "../services/api";
import { SkeletonTable } from "../components/ui/Skeleton";
import EmptyState from "../components/ui/EmptyState";
import ErrorState from "../components/ui/ErrorState";

export default function AuditLogs() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");
    const [actionFilter, setActionFilter] = useState("ALL");
    const [entityFilter, setEntityFilter] = useState("ALL");

    useEffect(() => {
        loadLogs();
    }, []);

    useEffect(() => {
        const handleBizSwitch = () => {
            loadLogs();
        };
        window.addEventListener("business-switched", handleBizSwitch);
        return () => window.removeEventListener("business-switched", handleBizSwitch);
    }, []);

    async function loadLogs() {
        try {
            setLoading(true);
            setError("");
            const data = await getAuditLogs();
            setLogs(data);
        } catch (err) {
            setError(err.message || "Failed to load audit logs");
        } finally {
            setLoading(false);
        }
    }

    const isFiltered = search !== "" || actionFilter !== "ALL" || entityFilter !== "ALL";

    const filtered = logs.filter(l => {
        if (actionFilter !== "ALL") {
            const act = (l.action || "").toUpperCase();
            if (!act.includes(actionFilter)) return false;
        }
        if (entityFilter !== "ALL") {
            const ent = (l.entityType || "").toUpperCase();
            if (!ent.includes(entityFilter)) return false;
        }
        if (!search) return true;
        const q = search.toLowerCase();
        return (
            (l.action && l.action.toLowerCase().includes(q)) ||
            (l.userEmail && l.userEmail.toLowerCase().includes(q)) ||
            (l.entityType && l.entityType.toLowerCase().includes(q)) ||
            (l.details && l.details.toLowerCase().includes(q)) ||
            (l.correlationId && l.correlationId.toLowerCase().includes(q)) ||
            (l.ipAddress && l.ipAddress.toLowerCase().includes(q))
        );
    });

    function getActionBadge(action) {
        if (!action) return <span className="status-badge status-draft">{action}</span>;
        const act = String(action).toUpperCase();
        if (act.includes("DELETE") || act.includes("CANCEL") || act.includes("LOCKED")) {
            return <span className="status-badge status-cancelled">{action}</span>;
        }
        if (act.includes("CREATE") || act.includes("GENERATE") || act.includes("ISSUE")) {
            return <span className="status-badge status-paid">{action}</span>;
        }
        if (act.includes("UPDATE") || act.includes("ROLE")) {
            return <span className="status-badge status-partially_paid">{action}</span>;
        }
        return <span className="status-badge status-issued">{action}</span>;
    }

    function renderLogDetails(details) {
        if (!details) return "—";
        if (details.includes("IRN generated:")) {
            const parts = details.split("AckNo:");
            const irnPart = parts[0].replace("E-Invoice IRN generated:", "").trim();
            const ackPart = parts[1] ? parts[1].trim() : null;
            return (
                <div style={{ display: "flex", flexDirection: "column", gap: "3px" }}>
                    <div style={{ fontWeight: 600, color: "var(--text-primary)", fontSize: "12px", display: "flex", alignItems: "center", gap: "6px" }}>
                        <Icon type="zap" size={13} color="var(--primary)" /> E-Invoice IRN Generated
                    </div>
                    <div
                        style={{
                            fontFamily: "monospace",
                            fontSize: "11px",
                            background: "#f1f5f9",
                            padding: "3px 6px",
                            borderRadius: "4px",
                            wordBreak: "break-all",
                            color: "#334155",
                            border: "1px solid #e2e8f0"
                        }}
                        title={irnPart}
                    >
                        {irnPart.length > 28 ? `${irnPart.substring(0, 14)}...${irnPart.substring(irnPart.length - 14)}` : irnPart}
                    </div>
                    {ackPart && (
                        <div style={{ fontSize: "11px", color: "var(--text-muted)" }}>
                            AckNo: <span style={{ fontWeight: 600, color: "var(--text-secondary)" }}>{ackPart}</span>
                        </div>
                    )}
                </div>
            );
        }
        return <span style={{ wordBreak: "break-word", overflowWrap: "anywhere" }}>{details}</span>;
    }

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {/* Header */}
            <div className="page-heading">
                <div>
                    <span className="eyebrow">SECURITY & COMPLIANCE</span>
                    <h1>System Audit Logs</h1>
                    <p>Tamper-evident traceability trail of user actions, invoice issuances, and tenant operations</p>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                    {loading && logs.length > 0 && (
                        <span className="refresh-indicator">
                            <span className="inline-spinner" style={{ width: "12px", height: "12px" }} />
                            <span>Updating...</span>
                        </span>
                    )}
                    <button
                        type="button"
                        className={`action-btn-sm ${loading ? "btn-loading" : ""}`}
                        style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
                        onClick={loadLogs}
                        disabled={loading}
                    >
                        {loading ? <span className="inline-spinner" /> : <Icon type="refresh" size={13} />}
                        <span>Refresh Logs</span>
                    </button>
                </div>
            </div>

            {error && (
                <ErrorState
                    message={error}
                    onRetry={loadLogs}
                />
            )}

            {/* Audit Logs Table Card */}
            <div className="table-card">
                <div className="table-header-card">
                    <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", flex: 1, alignItems: "center" }}>
                        <div className="search-input-wrapper" style={{ maxWidth: "400px", flex: 1 }}>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="11" cy="11" r="8" />
                                <line x1="21" y1="21" x2="16.65" y2="16.65" />
                            </svg>
                            <input
                                type="text"
                                className="search-input"
                                placeholder="Search logs by user, details, correlation..."
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
                            value={actionFilter}
                            onChange={e => setActionFilter(e.target.value)}
                            style={{ width: "160px", height: "38px" }}
                        >
                            <option value="ALL">All Actions</option>
                            <option value="CREATE">Create / Issue</option>
                            <option value="UPDATE">Update</option>
                            <option value="DELETE">Delete / Cancel</option>
                            <option value="LOGIN">Auth / Login</option>
                        </select>
                        <select
                            className="form-control"
                            value={entityFilter}
                            onChange={e => setEntityFilter(e.target.value)}
                            style={{ width: "160px", height: "38px" }}
                        >
                            <option value="ALL">All Entities</option>
                            <option value="INVOICE">Invoices</option>
                            <option value="CUSTOMER">Customers</option>
                            <option value="PRODUCT">Products</option>
                            <option value="PAYMENT">Payments</option>
                            <option value="BUSINESS">Business</option>
                            <option value="USER">Users</option>
                        </select>
                    </div>
                    <span style={{ fontSize: "13px", color: "var(--text-muted)", fontWeight: 500 }}>
                        Showing {filtered.length} log events
                    </span>
                </div>

                <div className="table-container">
                    {loading && logs.length === 0 ? (
                        <SkeletonTable rows={8} columns={8} />
                    ) : filtered.length === 0 ? (
                        <EmptyState
                            icon="shield"
                            title={isFiltered ? "No audit log events match filters" : "No audit logs recorded"}
                            message={isFiltered ? "No audit events matched your search query or filter criteria." : "Audit events will automatically appear here as team members perform business actions."}
                            actionLabel={isFiltered ? "Clear Filters" : undefined}
                            actionIcon={isFiltered ? "close" : undefined}
                            onAction={isFiltered ? () => { setSearch(""); setActionFilter("ALL"); setEntityFilter("ALL"); } : undefined}
                        />
                    ) : (
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>User</th>
                                    <th>Action</th>
                                    <th>Entity</th>
                                    <th>Entity ID</th>
                                    <th style={{ minWidth: "220px", maxWidth: "340px" }}>Details</th>
                                    <th>IP Address</th>
                                    <th>Correlation ID</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.map(l => (
                                    <tr key={l.id}>
                                        <td style={{ fontSize: "12px", color: "var(--text-secondary)", whiteSpace: "nowrap" }}>
                                            {l.createdAt ? new Date(l.createdAt).toLocaleString("en-IN", { dateStyle: "short", timeStyle: "medium" }) : "—"}
                                        </td>
                                        <td style={{ whiteSpace: "nowrap" }}><strong>{l.userEmail || "System"}</strong></td>
                                        <td>{getActionBadge(l.action)}</td>
                                        <td>
                                            <span className="badge" style={{ backgroundColor: "#f1f5f9", color: "#475569" }}>
                                                {l.entityType || "—"}
                                            </span>
                                        </td>
                                        <td style={{ letterSpacing: "0.2px", color: "var(--text-secondary)" }}>
                                            {l.entityId || "—"}
                                        </td>
                                        <td style={{ fontSize: "12.5px", color: "var(--text-secondary)", minWidth: "220px", maxWidth: "340px", wordBreak: "break-word", overflowWrap: "anywhere" }}>
                                            {renderLogDetails(l.details)}
                                        </td>
                                        <td style={{ letterSpacing: "0.2px", fontSize: "12px", color: "var(--text-muted)", whiteSpace: "nowrap" }}>
                                            {l.ipAddress || "—"}
                                        </td>
                                        <td>
                                            {l.correlationId ? (
                                                <span
                                                    style={{ letterSpacing: "0.2px", fontSize: "11.5px", color: "var(--primary)", cursor: "pointer", fontWeight: 500 }}
                                                    title="Click to filter by this correlation ID"
                                                    onClick={() => setSearch(l.correlationId)}
                                                >
                                                    {l.correlationId.length > 16 ? l.correlationId.substring(0, 16) + "..." : l.correlationId}
                                                </span>
                                            ) : "—"}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
}
