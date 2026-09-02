import { Plugin } from 'chart.js';

const CROSSHAIR_COLOR = 'rgba(52, 211, 153, 0.4)';
const CROSSHAIR_WIDTH = 1;
const POINT_RADIUS = 4;
const POINT_BORDER = 2;
const POINT_BORDER_COLOR = 'white';

/**
 * Chart.js plugin that draws a vertical crosshair line and hover points
 * at the active index. Hover points are drawn by the plugin (not by Chart.js)
 * to avoid chart-area clipping at the edges.
 */
export function createSparklineCrosshairPlugin(): Plugin {
    return {
        id: 'sparklineCrosshair',
        afterDraw(chart) {
            const activeElements = chart.getActiveElements();
            if (!activeElements?.length) return;

            const { ctx, chartArea } = chart;
            const x = activeElements[0].element.x;
            if (x == null || !chartArea) return;

            ctx.save();

            // Vertical crosshair line
            ctx.beginPath();
            ctx.strokeStyle = CROSSHAIR_COLOR;
            ctx.lineWidth = CROSSHAIR_WIDTH;
            ctx.setLineDash([]);
            ctx.moveTo(x, chartArea.top);
            ctx.lineTo(x, chartArea.bottom);
            ctx.stroke();

            // Hover points (drawn without clip so edge points are fully visible)
            ctx.setLineDash([]);
            for (const active of activeElements) {
                const { x: px, y: py } = active.element;
                const ds = chart.data.datasets[active.datasetIndex];
                const rawColor = ds.borderColor;
                const color = typeof rawColor === 'string' ? rawColor : '#000';

                ctx.beginPath();
                ctx.arc(px, py, POINT_RADIUS, 0, Math.PI * 2);
                ctx.fillStyle = color;
                ctx.fill();
                ctx.strokeStyle = POINT_BORDER_COLOR;
                ctx.lineWidth = POINT_BORDER;
                ctx.stroke();
            }

            ctx.restore();
        }
    };
}
