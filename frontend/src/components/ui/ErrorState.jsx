import React from "react";
import Icon from "../Icon";
import { sanitizeErrorMessage } from "../../utils/loadingFeedback";

/**
 * Standard enterprise ErrorState component.
 * Displays understandable, actionable feedback for failed API requests with a Retry button.
 */
export default function ErrorState({
    title = "Unable to load data",
    message = "An error occurred while communicating with the server. Please check your network connection and try again.",
    onRetry,
    retryLabel = "Retry",
    secondaryLabel,
    onSecondaryAction,
    className = "",
    style = {}
}) {

    return (
        <div
            className={`error-state-card ${className}`}
            style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                textAlign: "center",
                padding: "40px 24px",
                background: "var(--bg-card, #ffffff)",
                borderRadius: "var(--radius-md, 10px)",
                border: "1px solid rgba(239, 68, 68, 0.25)",
                margin: "12px 0",
                ...style
            }}
            role="alert"
            aria-live="assertive"
        >
            <div
                style={{
                    width: "52px",
                    height: "52px",
                    borderRadius: "50%",
                    backgroundColor: "var(--danger-bg, #fef2f2)",
                    color: "var(--danger, #ef4444)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    marginBottom: "16px",
                    border: "1px solid rgba(239, 68, 68, 0.2)"
                }}
                aria-hidden="true"
            >
                <Icon type="alert" size={24} color="var(--danger, #ef4444)" />
            </div>

            <h3
                style={{
                    fontSize: "16px",
                    fontWeight: 600,
                    color: "var(--danger-text, #991b1b)",
                    marginBottom: "8px"
                }}
            >
                {title}
            </h3>

            <p
                style={{
                    fontSize: "13.5px",
                    color: "var(--text-secondary, #475569)",
                    maxWidth: "460px",
                    lineHeight: "1.5",
                    marginBottom: "20px"
                }}
            >
                {sanitizeErrorMessage(message)}
            </p>

            <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", justifyContent: "center" }}>
                {onRetry && (
                    <button
                        type="button"
                        className="primary-button"
                        onClick={onRetry}
                        style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
                    >
                        <Icon type="refresh" size={14} />
                        <span>{retryLabel}</span>
                    </button>
                )}

                {secondaryLabel && onSecondaryAction && (
                    <button
                        type="button"
                        className="btn btn-secondary action-btn-sm"
                        onClick={onSecondaryAction}
                    >
                        <span>{secondaryLabel}</span>
                    </button>
                )}
            </div>
        </div>
    );
}
