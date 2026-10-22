import React, { useEffect } from "react";
import Icon from "../Icon";

/**
 * Accessible enterprise ConfirmDialog for dangerous/destructive actions.
 * Replaces crude window.confirm() with a polished SaaS confirmation modal.
 */
export default function ConfirmDialog({
    isOpen,
    title = "Confirm Action",
    message = "Are you sure you want to proceed with this action?",
    confirmLabel = "Confirm",
    cancelLabel = "Cancel",
    confirmVariant = "danger", // "danger" | "primary"
    onConfirm,
    onCancel,
    loading = false
}) {
    // Escape key closes dialog
    useEffect(() => {
        if (!isOpen) return;
        const handleKeyDown = (e) => {
            if (e.key === "Escape" && !loading) {
                onCancel();
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [isOpen, loading, onCancel]);

    if (!isOpen) return null;

    const isDanger = confirmVariant === "danger";

    return (
        <div
            className="modal-backdrop"
            style={{
                position: "fixed",
                inset: 0,
                backgroundColor: "rgba(15, 23, 42, 0.55)",
                backdropFilter: "blur(4px)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                zIndex: 10000,
                padding: "16px"
            }}
            onClick={(e) => {
                if (e.target === e.currentTarget && !loading) onCancel();
            }}
            role="dialog"
            aria-modal="true"
            aria-labelledby="confirm-dialog-title"
        >
            <div
                className="confirm-dialog-card"
                style={{
                    backgroundColor: "var(--bg-card, #ffffff)",
                    borderRadius: "var(--radius-lg, 16px)",
                    boxShadow: "var(--shadow-modal, 0 20px 25px -5px rgba(0, 0, 0, 0.15))",
                    width: "100%",
                    maxWidth: "440px",
                    padding: "24px",
                    display: "flex",
                    flexDirection: "column",
                    gap: "16px",
                    animation: "toastSlideIn 0.2s ease-out"
                }}
            >
                <div style={{ display: "flex", alignItems: "flex-start", gap: "14px" }}>
                    <div
                        style={{
                            width: "42px",
                            height: "42px",
                            borderRadius: "50%",
                            backgroundColor: isDanger ? "var(--danger-bg, #fef2f2)" : "var(--primary-light, #eff6ff)",
                            color: isDanger ? "var(--danger, #ef4444)" : "var(--primary, #2563eb)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            flexShrink: 0
                        }}
                        aria-hidden="true"
                    >
                        <Icon type={isDanger ? "alert" : "info"} size={22} color="currentColor" />
                    </div>

                    <div style={{ flex: 1 }}>
                        <h3
                            id="confirm-dialog-title"
                            style={{
                                fontSize: "16px",
                                fontWeight: 600,
                                color: "var(--text-primary, #0f172a)",
                                margin: "0 0 6px 0"
                            }}
                        >
                            {title}
                        </h3>
                        <p
                            style={{
                                fontSize: "13.5px",
                                color: "var(--text-secondary, #475569)",
                                margin: 0,
                                lineHeight: "1.5"
                            }}
                        >
                            {message}
                        </p>
                    </div>
                </div>

                <div
                    style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: "10px",
                        marginTop: "8px"
                    }}
                >
                    <button
                        type="button"
                        className="btn btn-secondary action-btn-sm"
                        onClick={onCancel}
                        disabled={loading}
                        style={{ padding: "8px 16px" }}
                    >
                        {cancelLabel}
                    </button>

                    <button
                        type="button"
                        className={isDanger ? "btn btn-danger action-btn-sm" : "primary-button action-btn-sm"}
                        onClick={onConfirm}
                        disabled={loading}
                        style={{
                            padding: "8px 18px",
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "8px",
                            backgroundColor: isDanger ? "var(--danger, #ef4444)" : undefined,
                            color: "#ffffff"
                        }}
                    >
                        {loading && (
                            <span
                                className="inline-spinner"
                                style={{
                                    width: "14px",
                                    height: "14px",
                                    border: "2px solid rgba(255, 255, 255, 0.4)",
                                    borderTopColor: "#ffffff",
                                    borderRadius: "50%",
                                    animation: "spin 0.8s linear infinite",
                                    display: "inline-block"
                                }}
                            />
                        )}
                        <span>{loading ? "Processing..." : confirmLabel}</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
