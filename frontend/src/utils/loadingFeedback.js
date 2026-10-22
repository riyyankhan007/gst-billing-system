/**
 * Utilities for loading states, error sanitization, and toast deduplication.
 */

/**
 * Sanitizes technical error messages to avoid leaking Bearer tokens,
 * raw JWTs, database errors, or sensitive payload credentials.
 *
 * @param {string|Error|null|undefined} error
 * @returns {string} Sanitized, user-friendly message.
 */
export function sanitizeErrorMessage(error) {
    if (!error) return "An unexpected error occurred. Please try again.";

    const raw = typeof error === "object" && error.message ? error.message : String(error);

    // Redact Bearer tokens
    let cleaned = raw.replace(/Bearer\s+[A-Za-z0-9._~+/-]+/gi, "Bearer [redacted]");

    // Redact JWT strings (3-part base64)
    cleaned = cleaned.replace(/eyJ[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+/gi, "[token redacted]");

    // Redact password parameters
    cleaned = cleaned.replace(/password=[^&\s]+/gi, "password=[redacted]");

    // Redact database table/column internal syntax
    cleaned = cleaned.replace(/SQLITE_[A-Z0-9_]+/gi, "Database error");
    cleaned = cleaned.replace(/PRAGMA\s+[^;]+/gi, "[system query redacted]");

    return cleaned.trim() || "An unexpected error occurred.";
}

/**
 * Checks whether an incoming toast notification is a duplicate within the deduplication window.
 *
 * @param {Map<string, number>} recentMap Map of `${type}:${message}` -> timestamp
 * @param {string} dedupeKey Unique key for message and type
 * @param {number} now Current timestamp in ms
 * @param {number} windowMs Window threshold in ms (defaults to 1500)
 * @returns {boolean} True if duplicate and should be dropped, false if accepted.
 */
export function shouldDedupeToast(recentMap, dedupeKey, now = Date.now(), windowMs = 1500) {
    if (!recentMap || !dedupeKey) return false;
    const lastSeen = recentMap.get(dedupeKey);
    if (lastSeen !== undefined && now - lastSeen < windowMs) {
        return true;
    }
    recentMap.set(dedupeKey, now);
    return false;
}

/**
 * Resolves appropriate titles, messages, and action labels for data-driven empty states.
 * Differentiates a genuinely empty dataset from empty search/filter results.
 *
 * @param {object} options
 * @param {number} options.totalCount Total un-filtered records count
 * @param {boolean} options.isFiltered Whether active search or dropdown filter is applied
 * @param {string} options.entityName Lowercase singular entity name (e.g. 'invoice', 'customer', 'supplier')
 * @param {string} [options.filterQuery] The current search term if present
 * @returns {object} { title: string, message: string, actionLabel: string, isFilterEmpty: boolean }
 */
export function resolveEmptyStateConfig({ totalCount = 0, isFiltered = false, entityName = "record", filterQuery = "" }) {
    if (isFiltered) {
        const queryText = filterQuery ? ` "${filterQuery}"` : "";
        return {
            title: `No ${entityName}s match your criteria`,
            message: `No matching ${entityName}s found${queryText}. Try adjusting your filters or search keywords.`,
            actionLabel: "Clear Filters",
            isFilterEmpty: true
        };
    }

    const plural = entityName.endsWith("y") ? entityName.slice(0, -1) + "ies" : `${entityName}s`;
    return {
        title: `No ${plural} yet`,
        message: `Create your first ${entityName} to get started.`,
        actionLabel: `Add First ${entityName.charAt(0).toUpperCase() + entityName.slice(1)}`,
        isFilterEmpty: false
    };
}

/**
 * Determines whether a server request has taken long enough to warrant a non-intrusive
 * cold-start wakeup indicator (e.g., > 2500ms).
 *
 * @param {number} elapsedMs Elapsed request duration in ms
 * @param {number} thresholdMs Threshold before triggering warning (default: 2500ms)
 * @returns {boolean}
 */
export function isColdStartSlow(elapsedMs, thresholdMs = 2500) {
    return typeof elapsedMs === "number" && elapsedMs >= thresholdMs;
}
