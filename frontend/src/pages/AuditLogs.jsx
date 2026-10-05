import React, { useEffect, useState } from "react";
import { getAuditLogs } from "../services/api";

export default function AuditLogs() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");

    useEffect(() => {
        loadLogs();
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

    const filtered = logs.filter(l => {
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

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {/* Header */}
            <div className="page-heading">
                <div>
                    <span className="eyebrow">SECURITY & COMPLIANCE</span>
                    <h1>System Audit Logs</h1>
                    <p>Tamper-evident traceability trail of user actions, invoice issuances, and tenant operations</p>
                </div>
                <button type="button" className="action-btn-sm" onClick={loadLogs}>
                    Refresh Logs
                </button>
            </div>

            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.12)", color: "var(--danger)", borderRadius: "var(--radius-sm)", border: "1px solid #fecaca", fontWeight: 500 }}>
                    {error}
                </div>
            )}

            {/* Audit Logs Table Card */}
            <div className="table-card">
                <div className="table-header-card">
                    <div className="search-input-wrapper" style={{ minWidth: "280px", maxWidth: "460px", flex: 1 }}>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="11" cy="11" r="8" />
                            <line x1="21" y1="21" x2="16.65" y2="16.65" />
                        </svg>
                        <input
                            type="text"
                            className="search-input"
                            placeholder="Search logs by user, action, details, correlation ID..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                    </div>
                    <span style={{ fontSize: "13px", color: "var(--text-muted)", fontWeight: 500 }}>
                        Showing {filtered.length} log events
                    </span>
                </div>

                <div className="table-container">
                    {loading ? (
                        <div style={{ padding: "40px", textAlign: "center" }}>
                            <div className="spinner" style={{ margin: "0 auto 12px" }} />
                            <p>Loading audit trail...</p>
                        </div>
                    ) : filtered.length === 0 ? (
                        <div style={{ padding: "40px", textAlign: "center", color: "var(--text-muted)" }}>
                            <p>No audit log events found matching filters.</p>
                        </div>
                    ) : (
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>User</th>
                                    <th>Action</th>
                                    <th>Entity</th>
                                    <th>Entity ID</th>
                                    <th>Details</th>
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
                                        <td><strong>{l.userEmail || "System"}</strong></td>
                                        <td>{getActionBadge(l.action)}</td>
                                        <td>
                                            <span className="badge" style={{ backgroundColor: "#f1f5f9", color: "#475569" }}>
                                                {l.entityType || "—"}
                                            </span>
                                        </td>
                                        <td style={{ letterSpacing: "0.2px", color: "var(--text-secondary)" }}>
                                            {l.entityId || "—"}
                                        </td>
                                        <td style={{ fontSize: "12.5px", color: "var(--text-secondary)", maxWidth: "280px" }}>
                                            {l.details || "—"}
                                        </td>
                                        <td style={{ letterSpacing: "0.2px", fontSize: "12px", color: "var(--text-muted)" }}>
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
