import { describe, it } from "node:test";
import assert from "node:assert/strict";

// Helper functions duplicating/validating inventory audit logic used in App.jsx and Inventory.jsx
function parseInventoryRoute(hashString) {
    const raw = (hashString || "").replace(/^#\/?/, "");
    if (!raw) return { page: "dashboard", inventoryTab: "stock" };

    const [pathPart, queryPart] = raw.split("?");
    const parts = pathPart.split("/").filter(Boolean);
    const primary = parts[0] || "dashboard";
    const subId = parts[1] || null;

    const queryParams = new URLSearchParams(queryPart || "");
    const qId = queryParams.get("id");

    if (primary === "inventory" || primary === "stock-audit" || primary === "stock-audit-trail" || primary === "audit-trail") {
        const tabParam = queryParams.get("tab") || subId;
        const normalizedTab = (tabParam === "audit" || tabParam === "movements" || tabParam === "trail")
            ? "movements"
            : (tabParam === "warehouses" || tabParam === "facilities" ? "warehouses" : (tabParam === "stock" ? "stock" : null));
        return {
            page: "inventory",
            inventoryTab: normalizedTab || (primary === "stock-audit" || primary === "stock-audit-trail" ? "movements" : "stock"),
            invoiceId: null,
            customerId: null
        };
    }

    return {
        page: primary,
        invoiceId: qId || null,
        customerId: qId || null,
        inventoryTab: "stock"
    };
}

function filterAuditMovements(movements, { search = "", movementFilter = "ALL", warehouseFilter = "ALL" } = {}) {
    if (!Array.isArray(movements)) return [];
    const q = search.toLowerCase().trim();

    return movements.filter(m => {
        const prodName = m.product?.name?.toLowerCase() || "";
        const prodSku = m.product?.sku?.toLowerCase() || "";
        const refNum = m.referenceNumber?.toLowerCase() || "";
        const notes = m.notes?.toLowerCase() || "";
        const whName = m.warehouse?.name?.toLowerCase() || "";
        const whCode = m.warehouse?.code?.toLowerCase() || "";
        const targetWh = m.targetWarehouse?.name?.toLowerCase() || "";
        const targetWhCode = m.targetWarehouse?.code?.toLowerCase() || "";

        const matchesSearch = !q ||
            prodName.includes(q) ||
            prodSku.includes(q) ||
            refNum.includes(q) ||
            notes.includes(q) ||
            whName.includes(q) ||
            whCode.includes(q) ||
            targetWh.includes(q) ||
            targetWhCode.includes(q);

        if (!matchesSearch) return false;
        if (movementFilter !== "ALL" && m.movementType !== movementFilter) return false;

        if (warehouseFilter !== "ALL") {
            const matchesWh = String(m.warehouse?.id) === String(warehouseFilter) ||
                              String(m.targetWarehouse?.id) === String(warehouseFilter);
            if (!matchesWh) return false;
        }

        return true;
    });
}

function formatAuditQuantity(qty) {
    const num = Number(qty || 0);
    return num > 0 ? `+${num}` : `${num}`;
}

describe("Stock Audit Trail Route Parsing & Navigation", () => {
    it("parses '#/inventory' to page 'inventory' with default tab 'stock'", () => {
        const route = parseInventoryRoute("#/inventory");
        assert.equal(route.page, "inventory");
        assert.equal(route.inventoryTab, "stock");
    });

    it("parses '#/inventory?tab=movements' to tab 'movements'", () => {
        const route = parseInventoryRoute("#/inventory?tab=movements");
        assert.equal(route.page, "inventory");
        assert.equal(route.inventoryTab, "movements");
    });

    it("parses direct audit route '#/stock-audit-trail' to tab 'movements'", () => {
        const route = parseInventoryRoute("#/stock-audit-trail");
        assert.equal(route.page, "inventory");
        assert.equal(route.inventoryTab, "movements");
    });

    it("parses path-based subroute '#/inventory/movements' to tab 'movements'", () => {
        const route = parseInventoryRoute("#/inventory/movements");
        assert.equal(route.page, "inventory");
        assert.equal(route.inventoryTab, "movements");
    });

    it("parses '#/inventory?tab=warehouses' to tab 'warehouses'", () => {
        const route = parseInventoryRoute("#/inventory?tab=warehouses");
        assert.equal(route.page, "inventory");
        assert.equal(route.inventoryTab, "warehouses");
    });
});

describe("Stock Audit Trail Movement Filtering & Safe Formatting", () => {
    const sampleMovements = [
        {
            id: 1,
            movementType: "OPENING_STOCK",
            quantity: "100.000",
            stockBefore: "0.000",
            stockAfter: "100.000",
            referenceNumber: "OPENING-2026",
            notes: "Initial balance",
            product: { id: 10, name: "Solar Inverter 5kW", sku: "INV-5000" },
            warehouse: { id: 1, name: "Main Depot", code: "MAIN" }
        },
        {
            id: 2,
            movementType: "INVOICE",
            quantity: "-5.000",
            stockBefore: "100.000",
            stockAfter: "95.000",
            referenceNumber: "INV-2026-0042",
            notes: "Deducted for invoice #INV-2026-0042",
            product: { id: 10, name: "Solar Inverter 5kW", sku: "INV-5000" },
            warehouse: { id: 1, name: "Main Depot", code: "MAIN" }
        },
        {
            id: 3,
            movementType: "TRANSFER_OUT",
            quantity: "-10.000",
            stockBefore: "95.000",
            stockAfter: "85.000",
            referenceNumber: "TRF-9901",
            notes: "Branch transfer",
            product: { id: 10, name: "Solar Inverter 5kW", sku: "INV-5000" },
            warehouse: { id: 1, name: "Main Depot", code: "MAIN" },
            targetWarehouse: { id: 2, name: "Pune Sub-Depot", code: "PUN-01" }
        },
        {
            id: 4,
            movementType: "PURCHASE",
            quantity: "50.000",
            stockBefore: "0.000",
            stockAfter: "50.000",
            referenceNumber: "SUPP-8821",
            notes: "Inward purchase",
            product: { id: 20, name: "Copper Cable 4mm", sku: "CBL-4MM" },
            warehouse: { id: 2, name: "Pune Sub-Depot", code: "PUN-01" }
        }
    ];

    it("returns all movements when no filters applied", () => {
        const filtered = filterAuditMovements(sampleMovements);
        assert.equal(filtered.length, 4);
    });

    it("filters movements by search query matching SKU", () => {
        const filtered = filterAuditMovements(sampleMovements, { search: "CBL-4MM" });
        assert.equal(filtered.length, 1);
        assert.equal(filtered[0].id, 4);
    });

    it("filters movements by movement type 'INVOICE'", () => {
        const filtered = filterAuditMovements(sampleMovements, { movementFilter: "INVOICE" });
        assert.equal(filtered.length, 1);
        assert.equal(filtered[0].referenceNumber, "INV-2026-0042");
    });

    it("filters movements by warehouse ID", () => {
        const filtered = filterAuditMovements(sampleMovements, { warehouseFilter: "2" });
        // id 3 has targetWarehouse 2, id 4 has warehouse 2
        assert.equal(filtered.length, 2);
    });

    it("handles null/undefined movements array safely", () => {
        assert.deepEqual(filterAuditMovements(null), []);
        assert.deepEqual(filterAuditMovements(undefined), []);
        assert.deepEqual(filterAuditMovements([]), []);
    });

    it("handles records with null product or warehouse gracefully without throwing", () => {
        const corruptedMovements = [
            { id: 99, movementType: "MANUAL_ADJUSTMENT", quantity: "2", product: null, warehouse: null }
        ];
        const result = filterAuditMovements(corruptedMovements, { search: "test" });
        assert.equal(result.length, 0);
    });

    it("formats positive quantity with '+' prefix and negative with '-'", () => {
        assert.equal(formatAuditQuantity("50"), "+50");
        assert.equal(formatAuditQuantity("-10"), "-10");
        assert.equal(formatAuditQuantity(0), "0");
        assert.equal(formatAuditQuantity(null), "0");
    });
});
