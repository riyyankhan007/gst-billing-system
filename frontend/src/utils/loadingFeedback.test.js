import test from "node:test";
import assert from "node:assert/strict";
import {
    sanitizeErrorMessage,
    shouldDedupeToast,
    resolveEmptyStateConfig,
    isColdStartSlow
} from "./loadingFeedback.js";

test("sanitizeErrorMessage - removes Bearer tokens", () => {
    const raw = "HTTP 401 Unauthorized: Authorization Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz.123 failed";
    const sanitized = sanitizeErrorMessage(raw);
    assert.ok(!sanitized.includes("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));
    assert.ok(sanitized.includes("Bearer [redacted]") || sanitized.includes("[token redacted]"));
});

test("sanitizeErrorMessage - handles null and undefined gracefully", () => {
    assert.equal(sanitizeErrorMessage(null), "An unexpected error occurred. Please try again.");
    assert.equal(sanitizeErrorMessage(undefined), "An unexpected error occurred. Please try again.");
    assert.equal(sanitizeErrorMessage(""), "An unexpected error occurred. Please try again.");
});

test("sanitizeErrorMessage - redacts password queries and database errors", () => {
    const raw = "Request failed at /api/auth?password=supersecret123&user=admin - SQLITE_CORRUPT";
    const sanitized = sanitizeErrorMessage(raw);
    assert.ok(!sanitized.includes("supersecret123"));
    assert.ok(sanitized.includes("password=[redacted]"));
    assert.ok(sanitized.includes("Database error"));
});

test("shouldDedupeToast - drops identical toast within window", () => {
    const map = new Map();
    const key = "error:Failed to fetch";

    // First call at t = 1000 -> accepted
    const res1 = shouldDedupeToast(map, key, 1000, 1500);
    assert.equal(res1, false);

    // Second call at t = 1500 (500ms later) -> deduplicated (dropped)
    const res2 = shouldDedupeToast(map, key, 1500, 1500);
    assert.equal(res2, true);

    // Third call at t = 2600 (1600ms later) -> accepted
    const res3 = shouldDedupeToast(map, key, 2600, 1500);
    assert.equal(res3, false);
});

test("shouldDedupeToast - handles different toast keys independently", () => {
    const map = new Map();
    const key1 = "success:Saved";
    const key2 = "error:Failed";

    assert.equal(shouldDedupeToast(map, key1, 1000, 1500), false);
    assert.equal(shouldDedupeToast(map, key2, 1000, 1500), false);
});

test("resolveEmptyStateConfig - genuine empty state", () => {
    const config = resolveEmptyStateConfig({
        totalCount: 0,
        isFiltered: false,
        entityName: "invoice"
    });

    assert.equal(config.title, "No invoices yet");
    assert.equal(config.isFilterEmpty, false);
    assert.equal(config.actionLabel, "Add First Invoice");
});

test("resolveEmptyStateConfig - filtered empty state with query", () => {
    const config = resolveEmptyStateConfig({
        totalCount: 15,
        isFiltered: true,
        entityName: "customer",
        filterQuery: "Acme Corp"
    });

    assert.equal(config.title, "No customers match your criteria");
    assert.ok(config.message.includes('"Acme Corp"'));
    assert.equal(config.isFilterEmpty, true);
    assert.equal(config.actionLabel, "Clear Filters");
});

test("isColdStartSlow - returns true only when duration exceeds threshold", () => {
    assert.equal(isColdStartSlow(1500, 2500), false);
    assert.equal(isColdStartSlow(2499, 2500), false);
    assert.equal(isColdStartSlow(2500, 2500), true);
    assert.equal(isColdStartSlow(3500, 2500), true);
    assert.equal(isColdStartSlow(null, 2500), false);
});
