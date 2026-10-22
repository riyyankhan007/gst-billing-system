import React from "react";

/**
 * Base atomic Skeleton component.
 * Renders an accessible, shimmer-animated placeholder matching design tokens.
 */
export function Skeleton({
    width,
    height,
    borderRadius,
    variant = "rounded", // "text" | "circular" | "rectangular" | "rounded"
    className = "",
    style = {},
    ...props
}) {
    const inlineStyle = {
        width: width ?? (variant === "text" ? "100%" : undefined),
        height: height ?? (variant === "text" ? "1em" : undefined),
        borderRadius: borderRadius ?? (
            variant === "circular" ? "50%" :
            variant === "text" ? "4px" :
            variant === "rectangular" ? "0px" :
            "var(--radius-sm, 6px)"
        ),
        ...style
    };

    return (
        <div
            className={`skeleton skeleton-${variant} ${className}`}
            style={inlineStyle}
            aria-hidden="true"
            {...props}
        />
    );
}

/**
 * SkeletonText renders multiple lines of simulated text with realistic width variation.
 */
export function SkeletonText({
    lines = 3,
    lineHeight = "14px",
    gap = "8px",
    lastLineWidth = "60%",
    className = "",
    style = {}
}) {
    return (
        <div
            className={`skeleton-text-group ${className}`}
            style={{ display: "flex", flexDirection: "column", gap, ...style }}
            aria-hidden="true"
        >
            {Array.from({ length: lines }).map((_, i) => {
                const isLast = i === lines - 1;
                const width = isLast ? lastLineWidth : (i % 2 === 1 ? "88%" : "100%");
                return (
                    <Skeleton
                        key={i}
                        variant="text"
                        height={lineHeight}
                        width={width}
                    />
                );
            })}
        </div>
    );
}

/**
 * SkeletonCard renders a structured card container with header and content lines.
 */
export function SkeletonCard({
    hasHeader = true,
    lines = 3,
    height,
    className = "",
    style = {},
    children
}) {
    return (
        <div
            className={`card skeleton-card ${className}`}
            style={{ padding: "20px", display: "flex", flexDirection: "column", gap: "14px", height, ...style }}
            aria-busy="true"
            aria-live="polite"
        >
            {children || (
                <>
                    {hasHeader && (
                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "4px" }}>
                            <Skeleton width="45%" height="18px" />
                            <Skeleton width="20%" height="14px" />
                        </div>
                    )}
                    <SkeletonText lines={lines} />
                </>
            )}
        </div>
    );
}

/**
 * SkeletonKpi renders an authentic financial KPI card skeleton.
 * Matches Dashboard and Reports KPI metrics (accent stripe, small label, large amount, sub-row).
 */
export function SkeletonKpi({
    stripeColor = "#cbd5e1",
    className = "",
    style = {}
}) {
    return (
        <div
            className={`analytics-kpi-card skeleton-kpi-card ${className}`}
            style={{ position: "relative", padding: "18px 20px", ...style }}
            aria-busy="true"
        >
            <div
                className="kpi-stripe"
                style={{ backgroundColor: stripeColor, opacity: 0.5 }}
                aria-hidden="true"
            />
            <Skeleton width="40%" height="12px" style={{ marginBottom: "12px" }} />
            <Skeleton width="65%" height="28px" style={{ marginBottom: "10px", borderRadius: "6px" }} />
            <Skeleton width="50%" height="11px" />
        </div>
    );
}

/**
 * SkeletonTable renders a table skeleton with a header row and data rows.
 */
export function SkeletonTable({
    columns = 6,
    rows = 5,
    showHeader = true,
    columnWidths = [],
    className = "",
    style = {}
}) {
    return (
        <div
            className={`skeleton-table-wrapper ${className}`}
            style={{ width: "100%", overflowX: "auto", ...style }}
            aria-busy="true"
            aria-live="polite"
        >
            <table className="table invoice-table" style={{ width: "100%", margin: 0 }}>
                {showHeader && (
                    <thead>
                        <tr>
                            {Array.from({ length: columns }).map((_, c) => (
                                <th key={c} style={{ width: columnWidths[c] || undefined }}>
                                    <Skeleton width={columnWidths[c] ? "80%" : "60%"} height="13px" />
                                </th>
                            ))}
                        </tr>
                    </thead>
                )}
                <tbody>
                    {Array.from({ length: rows }).map((_, r) => (
                        <tr key={r}>
                            {Array.from({ length: columns }).map((_, c) => {
                                // Natural width jitter for realistic tabular preview
                                const widthPercent = 45 + ((r * 17 + c * 31) % 45);
                                return (
                                    <td key={c} style={{ padding: "14px 16px" }}>
                                        <Skeleton
                                            width={`${widthPercent}%`}
                                            height="14px"
                                        />
                                    </td>
                                );
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

/**
 * PageSkeleton renders a top-level page skeleton with header, action buttons,
 * and customizable children (defaults to KPI cards + table).
 */
export function PageSkeleton({
    titleWidth = "220px",
    subtitleWidth = "380px",
    kpiCount = 4,
    tableColumns = 6,
    tableRows = 5,
    children
}) {
    return (
        <div className="page-skeleton-container" style={{ display: "flex", flexDirection: "column", gap: "24px" }} aria-busy="true">
            {/* Header Skeleton */}
            <div className="page-heading" style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "16px" }}>
                <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                    <Skeleton width="110px" height="12px" />
                    <Skeleton width={titleWidth} height="28px" />
                    <Skeleton width={subtitleWidth} height="14px" />
                </div>
                <div style={{ display: "flex", gap: "10px" }}>
                    <Skeleton width="120px" height="38px" borderRadius="var(--radius-sm, 6px)" />
                </div>
            </div>

            {children || (
                <>
                    {kpiCount > 0 && (
                        <div className="analytics-kpi-grid">
                            {Array.from({ length: kpiCount }).map((_, i) => (
                                <SkeletonKpi key={i} />
                            ))}
                        </div>
                    )}

                    <div className="table-card">
                        <div className="table-header-card" style={{ display: "flex", justifyContent: "space-between", padding: "16px 20px" }}>
                            <Skeleton width="280px" height="38px" borderRadius="6px" />
                            <Skeleton width="100px" height="20px" />
                        </div>
                        <SkeletonTable columns={tableColumns} rows={tableRows} />
                    </div>
                </>
            )}
        </div>
    );
}

export default Skeleton;
