import { useState } from "react";
import { importHsnSacCsv } from "../services/api";
import Icon from "./Icon";

const CSV_SAMPLE = `code,type,description,classification_level,gst_rate,effective_from,source
84713010,HSN,"Personal computer (laptop, palmtop, notebook)",8_DIGIT,18.00,2017-07-01,CBIC_TARIFF_SCH_3
85171300,HSN,"Smartphones",8_DIGIT,18.00,2017-07-01,CBIC_TARIFF_SCH_3
998311,SAC,"Information technology (IT) consulting and support services",TARIFF,18.00,2017-07-01,CBIC_SERVICES_11_2017
998222,SAC,"Accounting, auditing and bookkeeping services",TARIFF,18.00,2017-07-01,CBIC_SERVICES_11_2017`;

export default function HsnSacAdminImport({ onClose, onImportSuccess }) {
    const [file, setFile] = useState(null);
    const [csvText, setCsvText] = useState("");
    const [source, setSource] = useState("CBIC_GST_TARIFF_UPDATE");
    const [version, setVersion] = useState("2024.1");
    const [mode, setMode] = useState("file"); // "file" or "text"
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [result, setResult] = useState(null);

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
            setError("");
        }
    };

    const handleImport = async (e) => {
        e.preventDefault();
        setError("");
        setResult(null);

        const dataToUpload = mode === "file" ? file : csvText.trim();
        if (!dataToUpload) {
            setError(mode === "file" ? "Please select a valid CSV file to upload." : "Please paste CSV data.");
            return;
        }

        try {
            setLoading(true);
            const res = await importHsnSacCsv(dataToUpload, source, version);
            setResult(res);
            if (onImportSuccess) {
                onImportSuccess(res);
            }
        } catch (err) {
            setError(err.message || "Failed to process catalogue import.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-backdrop" style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(15, 23, 42, 0.6)",
            backdropFilter: "blur(4px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1100,
            padding: "16px"
        }}>
            <div className="modal-content" style={{
                backgroundColor: "var(--bg-card)",
                borderRadius: "12px",
                width: "100%",
                maxWidth: "640px",
                maxHeight: "90vh",
                overflowY: "auto",
                boxShadow: "var(--shadow-modal)",
                padding: "24px",
                border: "1px solid var(--border-color)"
            }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                    <div>
                        <h3 style={{ margin: 0, fontSize: "18px", fontWeight: "700", color: "var(--text-primary)" }}>
                            Official HSN/SAC Catalogue Import
                        </h3>
                        <p style={{ margin: "4px 0 0", fontSize: "12px", color: "var(--text-muted)" }}>
                            Administrator-controlled bulk import with verification, idempotency, and audit tracking.
                        </p>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        style={{ background: "transparent", border: "none", cursor: "pointer", color: "var(--text-muted)" }}
                    >
                        <Icon type="close" size={18} />
                    </button>
                </div>

                {error && (
                    <div style={{
                        padding: "10px 14px",
                        marginBottom: "16px",
                        backgroundColor: "var(--danger-bg)",
                        color: "var(--danger-text)",
                        borderRadius: "6px",
                        fontSize: "13px"
                    }}>
                        {error}
                    </div>
                )}

                {result && (
                    <div style={{
                        padding: "14px",
                        marginBottom: "20px",
                        backgroundColor: result.rejectedRecords > 0 ? "var(--warning-bg)" : "var(--success-bg)",
                        border: "1px solid var(--border-color)",
                        borderRadius: "8px"
                    }}>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "8px" }}>
                            <strong style={{ fontSize: "14px", color: result.rejectedRecords > 0 ? "var(--warning-text)" : "var(--success-text)" }}>
                                Import Status: {result.status}
                            </strong>
                            <span style={{ fontSize: "11px", color: "var(--text-muted)", fontFamily: "monospace" }}>
                                ({result.batchId})
                            </span>
                        </div>

                        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "8px", fontSize: "12px", textAlign: "center" }}>
                            <div style={{ background: "#ffffff", padding: "8px", borderRadius: "6px", border: "1px solid var(--border-color)" }}>
                                <div style={{ fontWeight: "700", fontSize: "16px", color: "var(--text-primary)" }}>{result.totalRecords}</div>
                                <div style={{ color: "var(--text-muted)", fontSize: "11px" }}>Total Read</div>
                            </div>
                            <div style={{ background: "#ffffff", padding: "8px", borderRadius: "6px", border: "1px solid var(--border-color)" }}>
                                <div style={{ fontWeight: "700", fontSize: "16px", color: "var(--success)" }}>+{result.insertedRecords}</div>
                                <div style={{ color: "var(--text-muted)", fontSize: "11px" }}>Inserted</div>
                            </div>
                            <div style={{ background: "#ffffff", padding: "8px", borderRadius: "6px", border: "1px solid var(--border-color)" }}>
                                <div style={{ fontWeight: "700", fontSize: "16px", color: "var(--primary)" }}>{result.updatedRecords}</div>
                                <div style={{ color: "var(--text-muted)", fontSize: "11px" }}>Updated</div>
                            </div>
                            <div style={{ background: "#ffffff", padding: "8px", borderRadius: "6px", border: "1px solid var(--border-color)" }}>
                                <div style={{ fontWeight: "700", fontSize: "16px", color: result.rejectedRecords > 0 ? "var(--danger)" : "var(--text-muted)" }}>
                                    {result.rejectedRecords}
                                </div>
                                <div style={{ color: "var(--text-muted)", fontSize: "11px" }}>Rejected</div>
                            </div>
                        </div>

                        {result.errors && result.errors.length > 0 && (
                            <div style={{ marginTop: "12px", padding: "8px", background: "#ffffff", borderRadius: "6px", maxHeight: "120px", overflowY: "auto" }}>
                                <strong style={{ fontSize: "11px", color: "var(--danger-text)", display: "block", marginBottom: "4px" }}>
                                    Validation Issues:
                                </strong>
                                <ul style={{ margin: 0, paddingLeft: "16px", fontSize: "11px", color: "var(--danger-text)" }}>
                                    {result.errors.map((err, i) => (
                                        <li key={i}>{err}</li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                )}

                <form onSubmit={handleImport}>
                    <div style={{ display: "flex", gap: "8px", marginBottom: "16px" }}>
                        <button
                            type="button"
                            className={mode === "file" ? "primary-button" : "secondary-button"}
                            onClick={() => setMode("file")}
                            style={{ flex: 1, padding: "8px" }}
                        >
                            Upload CSV File
                        </button>
                        <button
                            type="button"
                            className={mode === "text" ? "primary-button" : "secondary-button"}
                            onClick={() => setMode("text")}
                            style={{ flex: 1, padding: "8px" }}
                        >
                            Paste Raw CSV Data
                        </button>
                    </div>

                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "16px" }}>
                        <div className="form-field">
                            <label style={{ fontSize: "12px", fontWeight: "600" }}>Source Authority</label>
                            <input
                                type="text"
                                className="form-control"
                                value={source}
                                onChange={e => setSource(e.target.value)}
                                placeholder="e.g. CBIC_GST_TARIFF_2024"
                                required
                            />
                        </div>
                        <div className="form-field">
                            <label style={{ fontSize: "12px", fontWeight: "600" }}>Tariff Version</label>
                            <input
                                type="text"
                                className="form-control"
                                value={version}
                                onChange={e => setVersion(e.target.value)}
                                placeholder="e.g. 2024.1"
                                required
                            />
                        </div>
                    </div>

                    {mode === "file" ? (
                        <div className="form-field" style={{ marginBottom: "16px" }}>
                            <label style={{ fontSize: "12px", fontWeight: "600" }}>CSV File (*.csv)</label>
                            <input
                                type="file"
                                accept=".csv,text/csv"
                                className="form-control"
                                onChange={handleFileChange}
                                required={mode === "file"}
                            />
                        </div>
                    ) : (
                        <div className="form-field" style={{ marginBottom: "16px" }}>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "4px" }}>
                                <label style={{ fontSize: "12px", fontWeight: "600" }}>CSV Records</label>
                                <button
                                    type="button"
                                    onClick={() => setCsvText(CSV_SAMPLE)}
                                    style={{ background: "none", border: "none", color: "var(--primary)", fontSize: "11px", cursor: "pointer", textDecoration: "underline" }}
                                >
                                    Insert Sample Data
                                </button>
                            </div>
                            <textarea
                                rows={6}
                                className="form-control"
                                value={csvText}
                                onChange={e => setCsvText(e.target.value)}
                                placeholder="code,type,description,classification_level,gst_rate,effective_from,source"
                                style={{ fontFamily: "monospace", fontSize: "11px" }}
                                required={mode === "text"}
                            />
                        </div>
                    )}

                    <div style={{ fontSize: "11px", color: "var(--text-muted)", marginBottom: "20px", background: "var(--bg-muted)", padding: "10px", borderRadius: "6px" }}>
                        <strong>Required CSV Columns:</strong>
                        <div style={{ fontFamily: "monospace", marginTop: "2px" }}>
                            code,type,description,classification_level,gst_rate,effective_from,source
                        </div>
                        <div style={{ marginTop: "4px" }}>
                            • <strong>type:</strong> Must be either <code>HSN</code> (goods) or <code>SAC</code> (services).<br />
                            • <strong>gst_rate:</strong> Numerical percentage (0, 5, 12, 18, 28).<br />
                            • <strong>idempotent:</strong> Rerunning existing codes updates descriptions/rates without creating duplicates.
                        </div>
                    </div>

                    <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={onClose}
                            disabled={loading}
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="primary-button"
                            disabled={loading}
                            style={{ display: "flex", alignItems: "center", gap: "6px" }}
                        >
                            {loading ? (
                                <>
                                    <div className="spinner" style={{ width: "14px", height: "14px", borderWidth: "2px" }} />
                                    <span>Processing Import...</span>
                                </>
                            ) : (
                                <span>Commit Catalogue Import</span>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
