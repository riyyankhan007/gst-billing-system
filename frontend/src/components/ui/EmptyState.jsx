import React from "react";
import Icon from "../Icon";

/**
 * Standard enterprise EmptyState component.
 * Respects strict zero-emoji policy and enterprise UI consistency.
 */
export default function EmptyState({
    icon = "info",
    title = "No records found",
    description = "There is no data to display at this time.",
    actionLabel,
    onAction,
    actionIcon = "plus",
    actionDisabled = false,
    secondaryLabel,
    onSecondaryAction,
    isFilterResult = false,
    onClearFilters,
    className = "",
    style = {}
}) {
    return (
        <div
            className={`empty-state ${className}`}
            style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                textAlign: "center",
                padding: "48px 24px",
                background: "var(--bg-card, #ffffff)",
                borderRadius: "var(--radius-md, 10px)",
                border: "1px dashed var(--border-color, #e2e8f0)",
                margin: "12px 0",
                ...style
            }}
            role="status"
        >
            <div
                className="empty-state-icon"
                style={{
                    width: "56px",
                    height: "56px",
                    borderRadius: "50%",
                    backgroundColor: "var(--primary-light, #eff6ff)",
                    color: "var(--primary, #2563eb)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    marginBottom: "16px",
                    border: "1px solid rgba(37, 99, 235, 0.15)"
                }}
                aria-hidden="true"
            >
                <Icon type={isFilterResult ? "search" : icon} size={26} color="var(--primary, #2563eb)" />
            </div>

            <h3
                style={{
                    fontSize: "17px",
                    fontWeight: 600,
                    color: "var(--text-primary, #0f172a)",
                    marginBottom: "8px"
                }}
            >
                {title}
            </h3>

            <p
                style={{
                    fontSize: "13.5px",
                    color: "var(--text-muted, #64748b)",
                    maxWidth: "440px",
                    lineHeight: "1.55",
                    marginBottom: (actionLabel || isFilterResult) ? "20px" : "0"
                }}
            >
                {description}
            </p>

            <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", justifyContent: "center" }}>
                {isFilterResult && onClearFilters && (
                    <button
                        type="button"
                        className="btn btn-secondary action-btn-sm"
                        onClick={onClearFilters}
                        style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
                    >
                        <Icon type="close" size={13} />
                        <span>Clear Filters</span>
                    </button>
                )}

                {actionLabel && onAction && (
                    <button
                        type="button"
                        className="primary-button"
                        onClick={onAction}
                        disabled={actionDisabled}
                        style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}
                    >
                        {actionIcon && <Icon type={actionIcon} size={14} />}
                        <span>{actionLabel}</span>
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
