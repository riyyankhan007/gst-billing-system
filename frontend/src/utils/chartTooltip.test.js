import test from "node:test";
import assert from "node:assert/strict";
import {
    getChartTooltipPosition,
    calculateTaxPercentages,
    computeStackedBarGeometry
} from "./chartTooltip.js";

test("getChartTooltipPosition - left edge boundary alignment", () => {
    // 50px out of 720px is ~6.94% (< 22%)
    const pos = getChartTooltipPosition(50, 150, 720, 220);
    assert.equal(pos.hAlign, "left");
    assert.equal(pos.isNearTop, false);
    assert.equal(pos.className, "chart-tooltip pos-top align-left");
    assert.ok(pos.xPct < 22);
});

test("getChartTooltipPosition - right edge boundary alignment", () => {
    // 660px out of 720px is ~91.67% (> 78%)
    const pos = getChartTooltipPosition(660, 150, 720, 220);
    assert.equal(pos.hAlign, "right");
    assert.equal(pos.isNearTop, false);
    assert.equal(pos.className, "chart-tooltip pos-top align-right");
    assert.ok(pos.xPct > 78);
});

test("getChartTooltipPosition - center alignment", () => {
    // 360px out of 720px is 50%
    const pos = getChartTooltipPosition(360, 150, 720, 220);
    assert.equal(pos.hAlign, "center");
    assert.equal(pos.isNearTop, false);
    assert.equal(pos.className, "chart-tooltip pos-top align-center");
});

test("getChartTooltipPosition - tall bar near top flips vertically to pos-bottom", () => {
    // 25px out of 220px is ~11.36% (< 45%)
    const pos = getChartTooltipPosition(360, 25, 720, 220);
    assert.equal(pos.isNearTop, true);
    assert.equal(pos.className, "chart-tooltip pos-bottom align-center");
});

test("getChartTooltipPosition - clamps negative and overshooting coordinates", () => {
    const posNegative = getChartTooltipPosition(-50, -20, 720, 220);
    assert.equal(posNegative.xPct, 0);
    assert.equal(posNegative.yPct, 0);
    assert.equal(posNegative.className, "chart-tooltip pos-bottom align-left");

    const posOver = getChartTooltipPosition(1000, 500, 720, 220);
    assert.equal(posOver.xPct, 100);
    assert.equal(posOver.yPct, 100);
    assert.equal(posOver.className, "chart-tooltip pos-top align-right");
});

test("calculateTaxPercentages - accurate percentage distribution", () => {
    const res = calculateTaxPercentages(180, 180, 640, 1000);
    assert.equal(res.cgstPct, "18.0");
    assert.equal(res.sgstPct, "18.0");
    assert.equal(res.igstPct, "64.0");
});

test("calculateTaxPercentages - handles zero total gracefully", () => {
    const res = calculateTaxPercentages(0, 0, 0, 0);
    assert.equal(res.cgstPct, "0.0");
    assert.equal(res.sgstPct, "0.0");
    assert.equal(res.igstPct, "0.0");
});

test("computeStackedBarGeometry - tall stacked bar", () => {
    const geom = computeStackedBarGeometry({
        cgst: 5000,
        sgst: 5000,
        igst: 10000,
        maxGst: 20000,
        plotHeight: 165,
        paddingTop: 20,
        baseY: 185
    });

    assert.equal(geom.totalBarH, 165);
    assert.equal(geom.barTopY, 20); // Top of chart
    assert.equal(geom.cgstH, 41.25);
    assert.equal(geom.sgstH, 41.25);
    assert.equal(geom.igstH, 82.5);
});

test("computeStackedBarGeometry - zero liability fallback", () => {
    const geom = computeStackedBarGeometry({
        cgst: 0,
        sgst: 0,
        igst: 0,
        maxGst: 20000,
        plotHeight: 165,
        paddingTop: 20,
        baseY: 185
    });

    assert.equal(geom.totalBarH, 0);
    assert.equal(geom.barTopY, 175); // baseY - 10
});
