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
            (l.details && l.details.toLowerCase().includes(q))
        );
    });

    return (
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="page-heading">
                <div>
                    <span className="eyebrow">SECURITY & COMPLIANCE</span>
                    <h1>System Audit Logs</h1>
                    <p>Traceability trail of user actions, invoice issuances, and financial changes</p>
                </div>
                <button className="secondary-button" onClick={loadLogs}>
                    Refresh Logs
                </button>
            </div>

            {error && (
                <div style={{ padding: "12px 16px", background: "rgba(239, 68, 68, 0.15)", color: "var(--danger)", borderRadius: "8px" }}>
                    {error}
                </div>
            )}

            <div className="card" style={{ padding: "14px 20px" }}>
                <input
                    type="text"
                    className="search-input"
                    placeholder="Search logs by user, action, or details..."
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    style={{ minWidth: "320px", maxWidth: "420px" }}
                />
            </div>

            <div className="card" style={{ padding: 0, overflow: "hidden" }}>
                {loading ? (
                    <div style={{ padding: "40px", textAlign: "center" }}>
                        <div className="spinner" style={{ margin: "0 auto 12px" }} />
                        <p>Loading audit trail...</p>
                    </div>
                ) : filtered.length === 0 ? (
                    <div style={{ padding: "40px", textAlign: "center", color: "var(--muted)" }}>
                        <p>No audit log events recorded yet.</p>
                    </div>
                ) : (
                    <table className="table" style={{ width: "100%", margin: 0 }}>
                        <thead>
                            <tr>
                                <th>Timestamp</th>
                                <th>User</th>
                                <th>Action</th>
                                <th>Entity</th>
                                <th>ID</th>
                                <th>Details</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filtered.map(l => (
                                <tr key={l.id}>
                                    <td style={{ fontSize: "12px", whiteSpace: "nowrap" }}>
                                        {l.createdAt ? new Date(l.createdAt).toLocaleString() : "-"}
                                    </td>
                                    <td style={{ fontWeight: "600", fontSize: "13px" }}>{l.userEmail || "System"}</td>
                                    <td>
                                        <span className="badge" style={{ background: "rgba(99, 102, 241, 0.15)", color: "#4f46e5" }}>
                                            {l.action}
                                        </span>
                                    </td>
                                    <td style={{ fontWeight: "600", fontSize: "12px" }}>{l.entityType}</td>
                                    <td style={{ fontFamily: "monospace", fontSize: "12px" }}>{l.entityId || "-"}</td>
                                    <td style={{ fontSize: "13px", color: "var(--text-secondary)" }}>{l.details || "-"}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}
