/**
 * Utility functions for computing boundary-aware chart tooltip positioning,
 * percentages, and alignments to prevent clipping across all viewports.
 */

/**
 * Calculates horizontal and vertical positioning classes for chart tooltips
 * based on coordinate boundaries.
 *
 * @param {number} x - Current X coordinate in SVG coordinate space
 * @param {number} y - Current Y coordinate in SVG coordinate space
 * @param {number} svgWidth - Total SVG coordinate width
 * @param {number} svgHeight - Total SVG coordinate height
 * @param {number} [topThreshold=45] - Percentage threshold under which tooltip flips below
 * @returns {{ xPct: number, yPct: number, isNearTop: boolean, hAlign: 'left'|'center'|'right', className: string }}
 */
export function getChartTooltipPosition(x, y, svgWidth, svgHeight, topThreshold = 45) {
    const safeSvgWidth = svgWidth > 0 ? svgWidth : 1;
    const safeSvgHeight = svgHeight > 0 ? svgHeight : 1;

    const xPct = Math.max(0, Math.min(100, (Number(x || 0) / safeSvgWidth) * 100));
    const yPct = Math.max(0, Math.min(100, (Number(y || 0) / safeSvgHeight) * 100));

    const isNearTop = yPct < topThreshold;
    let hAlign = "center";
    if (xPct < 22) {
        hAlign = "left";
    } else if (xPct > 78) {
        hAlign = "right";
    }

    const posClass = isNearTop ? "pos-bottom" : "pos-top";
    const alignClass = `align-${hAlign}`;

    return {
        xPct,
        yPct,
        isNearTop,
        hAlign,
        className: `chart-tooltip ${posClass} ${alignClass}`
    };
}

/**
 * Calculates tax component percentages safely without division by zero.
 *
 * @param {number} cgst
 * @param {number} sgst
 * @param {number} igst
 * @param {number} total
 * @returns {{ cgstPct: string, sgstPct: string, igstPct: string }}
 */
export function calculateTaxPercentages(cgst, sgst, igst, total) {
    const numTotal = Number(total || 0);
    if (numTotal <= 0) {
        return {
            cgstPct: "0.0",
            sgstPct: "0.0",
            igstPct: "0.0"
        };
    }

    const cVal = Math.max(0, Number(cgst || 0));
    const sVal = Math.max(0, Number(sgst || 0));
    const iVal = Math.max(0, Number(igst || 0));

    return {
        cgstPct: ((cVal / numTotal) * 100).toFixed(1),
        sgstPct: ((sVal / numTotal) * 100).toFixed(1),
        igstPct: ((iVal / numTotal) * 100).toFixed(1)
    };
}

/**
 * Computes stacked bar segment heights and top anchor Y position.
 */
export function computeStackedBarGeometry({ cgst, sgst, igst, maxGst, plotHeight, paddingTop, baseY }) {
    const cgstVal = Math.max(0, Number(cgst || 0));
    const sgstVal = Math.max(0, Number(sgst || 0));
    const igstVal = Math.max(0, Number(igst || 0));
    const safeMax = Math.max(maxGst || 1, 1);

    const cgstH = Math.max(0, Math.min(plotHeight, (cgstVal / safeMax) * plotHeight));
    const sgstH = Math.max(0, Math.min(plotHeight - cgstH, (sgstVal / safeMax) * plotHeight));
    const igstH = Math.max(0, Math.min(plotHeight - cgstH - sgstH, (igstVal / safeMax) * plotHeight));

    const cgstY = baseY - cgstH;
    const sgstY = cgstY - sgstH;
    const igstY = sgstY - igstH;

    const totalBarH = cgstH + sgstH + igstH;
    const barTopY = totalBarH > 0 ? igstY : baseY - 10;

    return {
        cgstVal,
        sgstVal,
        igstVal,
        cgstH,
        sgstH,
        igstH,
        cgstY,
        sgstY,
        igstY,
        totalBarH,
        barTopY
    };
}
