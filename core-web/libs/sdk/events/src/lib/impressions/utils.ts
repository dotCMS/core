/**
 * Utility functions for impression tracking
 */

import { calculateElementVisibilityRatio } from '../contentlets/viewport';

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
