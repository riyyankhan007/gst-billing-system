import { describe, it } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const cssPath = path.resolve(__dirname, "../styles/app.css");

describe("Mobile Responsiveness Audit & CSS Integrity", () => {
    const cssContent = fs.readFileSync(cssPath, "utf-8");

    it("ensures mobile topbar supports safe-area-inset-top", () => {
        assert.ok(
            cssContent.includes("calc(56px + env(safe-area-inset-top, 0px))"),
            "mobile-topbar must account for safe-area-inset-top on modern mobile devices"
        );
        assert.ok(
            cssContent.includes("padding-top: env(safe-area-inset-top, 0px)"),
            "mobile-topbar must apply top padding for safe-area-inset"
        );
    });

    it("ensures hamburger button has minimum 44x44px touch target", () => {
        const hamburgerRegex = /\.hamburger-btn\s*\{[^}]*min-width:\s*44px;[^}]*min-height:\s*44px;/s;
        assert.ok(
            hamburgerRegex.test(cssContent),
            "hamburger-btn must satisfy 44x44px touch target guidelines"
        );
    });

    it("ensures sidebar drawer close button has minimum 44x44px touch target", () => {
        const closeBtnRegex = /\.sidebar-close-btn\s*\{[^}]*min-width:\s*44px;[^}]*min-height:\s*44px;/s;
        assert.ok(
            closeBtnRegex.test(cssContent),
            "sidebar-close-btn must satisfy 44x44px touch target guidelines"
        );
    });

    it("ensures business switcher dropdown does not blowout mobile viewports (<= 640px)", () => {
        assert.ok(
            cssContent.includes(".business-switcher-menu") &&
            cssContent.includes("position: fixed !important") &&
            cssContent.includes("max-width: calc(100vw - 16px) !important"),
            "business-switcher-menu must have position: fixed and max-width bounded to viewport on mobile"
        );
    });

    it("ensures grid column spans (col-span-2/3/4) collapse to span 1 on small screens (<= 640px)", () => {
        assert.ok(
            cssContent.includes(".col-span-2,") &&
            cssContent.includes("grid-column: span 1 !important"),
            "col-span-2, col-span-3, col-span-4 must collapse to span 1 on mobile to prevent CSS grid expansion"
        );
    });

    it("ensures table-container and table-responsive provide horizontal scrolling with touch momentum", () => {
        assert.ok(
            cssContent.includes(".table-container") &&
            cssContent.includes("overflow-x: auto") &&
            cssContent.includes("-webkit-overflow-scrolling: touch"),
            "table-container must have overflow-x: auto and smooth momentum scrolling"
        );
    });

    it("ensures modal backdrop does not use buggy 100vw that causes desktop horizontal overflow", () => {
        const backdropSection = cssContent.slice(cssContent.indexOf(".modal-backdrop"));
        const backdropRule = backdropSection.slice(0, backdropSection.indexOf("}"));
        assert.ok(
            !backdropRule.includes("width: 100vw"),
            "modal-backdrop must use width: 100% or omit width instead of 100vw"
        );
        assert.ok(
            backdropRule.includes("height: 100dvh"),
            "modal-backdrop should leverage dynamic viewport height (100dvh) for mobile browser address bars"
        );
    });

    it("ensures no blanket overflow-x: hidden is applied to html or body to mask layout bugs", () => {
        const bodyRegex = /(?:html|body)\s*\{[^}]*overflow-x:\s*hidden;/i;
        assert.ok(
            !bodyRegex.test(cssContent),
            "Blanket overflow-x: hidden on html or body is prohibited as it masks layout bugs and breaks position sticky"
        );
    });

    it("ensures key breakpoints (360px, 480px, 540px, 640px, 768px, 900px) are covered", () => {
        assert.ok(cssContent.includes("@media (max-width: 900px)"), "900px breakpoint required");
        assert.ok(cssContent.includes("@media (max-width: 768px)"), "768px breakpoint required");
        assert.ok(cssContent.includes("@media (max-width: 640px)"), "640px breakpoint required");
        assert.ok(cssContent.includes("@media (max-width: 540px)"), "540px breakpoint required");
        assert.ok(cssContent.includes("@media (max-width: 480px)"), "480px breakpoint required");
        assert.ok(cssContent.includes("@media (max-width: 360px)"), "360px breakpoint required");
    });
});
