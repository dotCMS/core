/**
 * Utility functions for impression tracking
 * Pure functions without class dependencies for better testability and reusability
 */

import { ViewportMetrics } from '../../shared/models';

/**
 * Calculates the visibility ratio of an element in the viewport
 * @param element - The HTML element to check
 * @returns A number between 0 and 1 representing the visible percentage
 */
export function calculateElementVisibilityRatio(element: HTMLElement): number {
    const rect = element.getBoundingClientRect();
    const viewHeight = window.innerHeight || document.documentElement.clientHeight;
    const viewWidth = window.innerWidth || document.documentElement.clientWidth;

    const visibleHeight = Math.min(rect.bottom, viewHeight) - Math.max(rect.top, 0);
    const visibleWidth = Math.min(rect.right, viewWidth) - Math.max(rect.left, 0);

    if (visibleHeight <= 0 || visibleWidth <= 0) {
        return 0;
    }

    const visibleArea = visibleHeight * visibleWidth;
    const totalArea = rect.height * rect.width;

    return totalArea > 0 ? visibleArea / totalArea : 0;
}

/**
 * Calculates the offset percentage of an element from the top of the viewport
 * @param element - The HTML element to check
 * @returns Percentage value (can be negative if above viewport)
 */
export function calculateViewportOffset(element: HTMLElement): number {
    const rect = element.getBoundingClientRect();
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight;

    const offsetPercentage = (rect.top / viewportHeight) * 100;
    return Math.round(offsetPercentage * 100) / 100;
}

/**
 * Checks if an element meets a specific visibility threshold
 * @param element - The HTML element to check
 * @param threshold - The required visibility ratio (0.0 to 1.0)
 * @returns True if the element meets or exceeds the threshold
 */
export function isElementMeetingVisibilityThreshold(
    element: HTMLElement,
    threshold: number
): boolean {
    const visibilityRatio = calculateElementVisibilityRatio(element);
    return visibilityRatio >= threshold;
}

/**
 * Gets comprehensive viewport metrics for an element
 * @param element - The HTML element to analyze
 * @returns Object containing offset percentage and visibility ratio
 */
export function getViewportMetrics(element: HTMLElement): ViewportMetrics {
    const visibilityRatio = calculateElementVisibilityRatio(element);
    const offsetPercentage = calculateViewportOffset(element);

    return {
        offsetPercentage,
        visibilityRatio
    };
}
