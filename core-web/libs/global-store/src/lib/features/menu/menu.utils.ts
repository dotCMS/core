/**
 * Pure utility functions for menu navigation logic.
 * These functions provide helper utilities for menu URL processing.
 */

/**
 * Extracts the relevant ID from a URL path.
 * For legacy URLs (starting with /c/), returns the last segment.
 * For other URLs, returns the first segment.
 *
 * @param url - The URL path to extract the ID from
 * @returns The extracted ID, or empty string if not found
 *
 * @example
 * getTheUrlId('/c/content-types') // returns 'content-types'
 * getTheUrlId('/sites') // returns 'sites'
 */
export function getTheUrlId(url: string): string {
    const urlSegments: string[] = url.split('/').filter(Boolean);

    if (urlSegments[0] === 'c') {
        return urlSegments.pop() || '';
    }

    return urlSegments[0] || '';
}
