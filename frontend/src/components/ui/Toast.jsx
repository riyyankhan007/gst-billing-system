import React from "react";
import Icon from "../Icon";

export default function ToastContainer({ toasts = [], onDismiss }) {
    if (!toasts || toasts.length === 0) return null;

    return (
        <div
            className="toast-container"
            style={{
                position: "fixed",
                top: "20px",
                right: "20px",
                zIndex: 9999,
                display: "flex",
                flexDirection: "column",
                gap: "10px",
                maxWidth: "380px",
                width: "calc(100vw - 40px)",
                pointerEvents: "none"
            }}
            aria-live="polite"
            role="region"
            aria-label="Notifications"
        >
            {toasts.map((t) => (
                <ToastItem key={t.id} toast={t} onDismiss={() => onDismiss(t.id)} />
            ))}
        </div>
    );
}

function ToastItem({ toast, onDismiss }) {
    const { message, type = "info" } = toast;

    const stylesByType = {
        success: {
            bg: "var(--success-bg, #ecfdf5)",
            border: "1px solid rgba(16, 185, 129, 0.3)",
            color: "var(--success-text, #065f46)",
            icon: "checkCircle",
            iconColor: "var(--success, #10b981)"
        },
        error: {
            bg: "var(--danger-bg, #fef2f2)",
            border: "1px solid rgba(239, 68, 68, 0.3)",
            color: "var(--danger-text, #991b1b)",
            icon: "xCircle",
            iconColor: "var(--danger, #ef4444)"
        },
        warning: {
            bg: "var(--warning-bg, #fffbeb)",
            border: "1px solid rgba(245, 158, 11, 0.3)",
            color: "var(--warning-text, #92400e)",
            icon: "alert",
            iconColor: "var(--warning, #f59e0b)"
        },
        info: {
            bg: "var(--primary-light, #eff6ff)",
            border: "1px solid rgba(37, 99, 235, 0.3)",
            color: "var(--primary-text, #1d4ed8)",
            icon: "info",
            iconColor: "var(--primary, #2563eb)"
        }
    };

    const cfg = stylesByType[type] || stylesByType.info;

    return (
        <div
            className={`toast-item toast-${type}`}
            style={{
                pointerEvents: "auto",
                backgroundColor: cfg.bg,
                border: cfg.border,
                color: cfg.color,
                borderRadius: "var(--radius-md, 10px)",
                padding: "12px 14px",
                boxShadow: "var(--shadow-md, 0 4px 6px -1px rgba(0, 0, 0, 0.08))",
                display: "flex",
                alignItems: "flex-start",
                gap: "10px",
                animation: "toastSlideIn 0.25s ease-out",
                fontSize: "13.5px",
                lineHeight: "1.4"
            }}
            role="status"
        >
            <div style={{ flexShrink: 0, marginTop: "2px" }} aria-hidden="true">
                <Icon type={cfg.icon} size={18} color={cfg.iconColor} />
            </div>

            <div style={{ flex: 1, wordBreak: "break-word" }}>
                {message}
            </div>

            <button
                type="button"
                onClick={onDismiss}
                style={{
                    background: "none",
                    border: "none",
                    padding: "2px",
                    color: "currentColor",
                    opacity: 0.6,
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    flexShrink: 0
                }}
                aria-label="Dismiss notification"
            >
                <Icon type="close" size={13} />
            </button>
        </div>
    );
}
