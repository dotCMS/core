/**
 * Centralized tooltip configuration for analytics charts.
 * Provides consistent styling and behavior across all chart components.
 */

/** Options for customizing tooltip behavior */
export interface TooltipOptions {
    /** Whether tooltip is enabled */
    enabled?: boolean;
    /** Whether to show color indicators */
    displayColors?: boolean;
    /** Custom label callback */
    labelCallback?: (context: { parsed: { y: number }; label?: string }) => string;
    /** Custom title callback */
    titleCallback?: (context: { label?: string }[]) => string;
}

/** Default tooltip styling - dark theme with smooth animation */
export const TOOLTIP_STYLE = {
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    titleColor: '#ffffff',
    bodyColor: '#ffffff',
    borderColor: 'rgba(255, 255, 255, 0.1)',
    borderWidth: 1,
    padding: 10,
    cornerRadius: 4,
    titleFont: { size: 12, weight: 'normal' as const },
    bodyFont: { size: 13, weight: 'bold' as const },
    // Smooth tooltip animation
    animation: {
        duration: 150,
        easing: 'easeOutQuart' as const
    }
} as const;

/** Default animation configuration for smooth interactions */
export const CHART_TRANSITIONS = {
    active: {
        animation: {
            duration: 200,
            easing: 'easeOutCubic' as const
        }
    }
} as const;

/** Animation config that disables initial animation but keeps interactions smooth */
export const CHART_ANIMATION = {
    duration: 0
} as const;

/**
 * Creates a tooltip configuration object for Chart.js.
 * Uses consistent dark styling across all analytics charts.
 *
 * @param options - Optional customization options
 * @returns Chart.js tooltip configuration
 *
 * @example
 * ```typescript
 * // Basic usage
 * tooltip: createTooltipConfig({ enabled: true })
 *
 * // With custom label
 * tooltip: createTooltipConfig({
 *     enabled: true,
 *     labelCallback: (ctx) => `Value: ${ctx.parsed.y}%`
 * })
 * ```
 */
export function createTooltipConfig(options: TooltipOptions = {}) {
    const { enabled = true, displayColors = false, labelCallback, titleCallback } = options;

    return {
        enabled,
        displayColors,
        ...TOOLTIP_STYLE,
        callbacks: {
            ...(labelCallback && {
                label: labelCallback
            }),
            ...(titleCallback && {
                title: titleCallback
            })
        }
    };
}

/**
 * Creates the full interaction configuration for charts.
 * Includes animation settings and transitions for smooth hover effects.
 *
 * @returns Object with animation and transitions config
 *
 * @example
 * ```typescript
 * const options = {
 *     ...createInteractionConfig(),
 *     plugins: { ... }
 * };
 * ```
 */
export function createInteractionConfig() {
    return {
        animation: CHART_ANIMATION,
        transitions: CHART_TRANSITIONS
    };
}
