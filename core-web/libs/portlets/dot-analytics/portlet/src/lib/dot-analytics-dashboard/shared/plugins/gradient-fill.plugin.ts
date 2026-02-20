import { Plugin } from 'chart.js';

/** Options for the gradient fill plugin */
export interface GradientFillOptions {
    /** Whether gradient fill is enabled */
    enabled: boolean;
    /** Color for the gradient (hex, rgb, or rgba) */
    color: string;
    /** Opacity at the top of the gradient (0-1) */
    topOpacity?: number;
    /** Opacity at the middle of the gradient (0-1) */
    middleOpacity?: number;
    /** Opacity at the bottom of the gradient (0-1) */
    bottomOpacity?: number;
}

/** Default gradient opacities */
const DEFAULT_TOP_OPACITY = 0.4;
const DEFAULT_MIDDLE_OPACITY = 0.25;
const DEFAULT_BOTTOM_OPACITY = 0;

/**
 * Creates a Chart.js plugin that applies a vertical gradient fill to line charts.
 * The gradient fades from more opaque at the top to transparent at the bottom.
 *
 * @param getOptions - Function that returns the current gradient options
 * @param hexToRgba - Function to convert color to rgba with alpha
 * @returns Chart.js plugin configuration
 *
 * @example
 * ```typescript
 * readonly gradientPlugin = createGradientFillPlugin(
 *     () => ({
 *         enabled: this.$filled(),
 *         color: this.$color()
 *     }),
 *     (color, alpha) => this.#hexToRgba(color, alpha)
 * );
 * ```
 */
export function createGradientFillPlugin(
    getOptions: () => GradientFillOptions,
    hexToRgba: (color: string, alpha: number) => string
): Plugin {
    return {
        id: 'gradientFill',
        afterLayout: (chart) => {
            const options = getOptions();

            if (!options.enabled) return;

            const { ctx, chartArea, data } = chart;
            if (!chartArea) return;

            const dataset = data?.datasets?.[0];
            if (!dataset) return;

            // Only create gradient if not already a CanvasGradient
            if (dataset.backgroundColor instanceof CanvasGradient) return;

            const {
                color,
                topOpacity = DEFAULT_TOP_OPACITY,
                middleOpacity = DEFAULT_MIDDLE_OPACITY,
                bottomOpacity = DEFAULT_BOTTOM_OPACITY
            } = options;

            const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);

            gradient.addColorStop(0, hexToRgba(color, topOpacity));
            gradient.addColorStop(0.5, hexToRgba(color, middleOpacity));
            gradient.addColorStop(1, hexToRgba(color, bottomOpacity));

            dataset.backgroundColor = gradient;
        }
    };
}
