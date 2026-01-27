import { Plugin } from 'chart.js';

import { NgZone } from '@angular/core';

/** Animation state interface */
interface AnimationState {
    progress: number;
    startTime: number;
    animating: boolean;
}

/** Options for the line draw animation plugin */
export interface LineDrawAnimationOptions {
    /** Whether animation is enabled */
    enabled: boolean;
    /** Animation duration in milliseconds */
    duration: number;
}

/** Default animation duration */
export const LINE_DRAW_ANIMATION_DURATION = 1200;

/** Default sparkline animation duration (shorter for smaller charts) */
export const SPARKLINE_ANIMATION_DURATION = 1000;

/**
 * Creates a Chart.js plugin that animates line charts with a "drawing" effect.
 * The line is progressively revealed from left to right using canvas clipping.
 *
 * @param getOptions - Function that returns the current animation options
 * @param animationState - Mutable state object to track animation progress
 * @param ngZone - Optional NgZone to run animation outside Angular's change detection
 * @returns Chart.js plugin configuration
 *
 * @example
 * ```typescript
 * // In a component
 * readonly #ngZone = inject(NgZone);
 * #animationState = createAnimationState();
 *
 * readonly lineDrawPlugin = [
 *     createLineDrawAnimationPlugin(
 *         () => ({ enabled: this.$animated(), duration: 1000 }),
 *         this.#animationState,
 *         this.#ngZone
 *     )
 * ];
 * ```
 */
export function createLineDrawAnimationPlugin(
    getOptions: () => LineDrawAnimationOptions,
    animationState: AnimationState,
    ngZone?: NgZone
): Plugin {
    return {
        id: 'lineDrawAnimation',
        beforeDatasetsDraw: (chart) => {
            const options = getOptions();

            if (!options.enabled) return;

            const { ctx, chartArea } = chart;
            if (!chartArea) return;

            const now = performance.now();

            // Initialize animation on first draw
            if (!animationState.animating) {
                animationState.startTime = now;
                animationState.animating = true;
                animationState.progress = 0;
            }

            // Calculate progress (0 to 1)
            const elapsed = now - animationState.startTime;
            animationState.progress = Math.min(elapsed / options.duration, 1);

            // Easing function (easeOutCubic) for smooth deceleration
            const eased = 1 - Math.pow(1 - animationState.progress, 3);

            // Calculate clip width based on progress
            const width = chartArea.right - chartArea.left;
            const clipWidth = chartArea.left + width * eased;

            // Apply clip to reveal line progressively from left to right
            ctx.save();
            ctx.beginPath();
            ctx.rect(
                chartArea.left,
                chartArea.top - 10,
                clipWidth - chartArea.left,
                chartArea.bottom - chartArea.top + 20
            );
            ctx.clip();
        },
        afterDatasetsDraw: (chart) => {
            const options = getOptions();

            if (!options.enabled) return;

            chart.ctx.restore();

            // Continue animation loop if not complete
            if (animationState.progress < 1) {
                const animationLoop = () => {
                    requestAnimationFrame(() => {
                        chart.update('none');
                    });
                };

                // Run outside Angular's zone to avoid triggering change detection
                // on every animation frame (~60 times/second)
                if (ngZone) {
                    ngZone.runOutsideAngular(animationLoop);
                } else {
                    animationLoop();
                }
            }
        }
    };
}

/**
 * Creates a fresh animation state object.
 * Call this to get a new state for each component instance.
 *
 * @returns Fresh animation state
 */
export function createAnimationState(): AnimationState {
    return {
        progress: 0,
        startTime: 0,
        animating: false
    };
}

/**
 * Resets the animation state to replay the animation.
 * Useful when data changes or component transitions from loading to loaded.
 *
 * @param state - Animation state to reset
 */
export function resetAnimationState(state: AnimationState): void {
    state.progress = 0;
    state.startTime = 0;
    state.animating = false;
}
