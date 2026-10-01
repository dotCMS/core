import { isBefore, isValid, parse } from 'date-fns';

import { TIME_RANGE_OPTIONS, TimeRange } from '@dotcms/portlets/dot-analytics/data-access';

/**
 * Distributes integer percentages using the Largest Remainder Method,
 * guaranteeing the sum equals exactly 100.
 *
 * @param values - Raw numeric values to convert to percentages
 * @returns Array of integer percentages summing to 100
 */
export function distributePercentages(values: number[]): number[] {
    const total = values.reduce((sum, v) => sum + v, 0);
    if (total <= 0) {
        return values.map(() => 0);
    }

    const rawPercentages = values.map((v) => (v / total) * 100);
    const floored = rawPercentages.map(Math.floor);
    const remainders = rawPercentages.map((val, i) => val - floored[i]);
    let remaining = 100 - floored.reduce((sum, v) => sum + v, 0);

    const indices = remainders.map((r, i) => ({ r, i })).sort((a, b) => b.r - a.r);

    for (const { i } of indices) {
        if (remaining <= 0) {
            break;
        }
        floored[i]++;
        remaining--;
    }

    return floored;
}

/**
 * Validates custom date range parameters.
 * A single calendar day is valid; the end date must not precede the start date.
 * @param fromDate - Start date string (yyyy-MM-dd)
 * @param toDate - End date string (yyyy-MM-dd)
 * @returns true if both dates are valid and in non-decreasing order
 */
export const isValidCustomDateRange = (fromDate: string, toDate: string): boolean => {
    const fromDateObj = parse(fromDate, 'yyyy-MM-dd', new Date());
    const toDateObj = parse(toDate, 'yyyy-MM-dd', new Date());

    if (!isValid(fromDateObj) || !isValid(toDateObj)) {
        return false;
    }

    return !isBefore(toDateObj, fromDateObj);
};

/**
 * Validates and returns a valid time range from a URL parameter value.
 * @param urlValue - URL parameter value for time range
 * @returns Valid TimeRange or null if invalid
 */
export const getValidTimeRangeUrl = (urlValue: string): TimeRange | null => {
    if (!urlValue || typeof urlValue !== 'string') {
        return null;
    }

    return Object.values(TIME_RANGE_OPTIONS).includes(urlValue as TimeRange)
        ? (urlValue as TimeRange)
        : null;
};

/**
 * Converts a color (hex, rgb, or rgba) to rgba format with specified alpha.
 * Useful for creating semi-transparent versions of colors for charts and gradients.
 *
 * @param color - Color string in hex (#RRGGBB), rgb(r, g, b), or rgba(r, g, b, a) format
 * @param alpha - Alpha value between 0 (transparent) and 1 (opaque)
 * @returns Color in rgba format
 *
 * @example
 * hexToRgba('#1243e3', 0.5) // => 'rgba(18, 67, 227, 0.5)'
 * hexToRgba('rgb(255, 0, 0)', 0.3) // => 'rgba(255, 0, 0, 0.3)'
 */
export const hexToRgba = (color: string, alpha: number): string => {
    // Handle rgb format
    if (color.startsWith('rgb(')) {
        const match = color.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
        if (match) {
            return `rgba(${match[1]}, ${match[2]}, ${match[3]}, ${alpha})`;
        }
    }

    // Handle rgba format (replace existing alpha)
    if (color.startsWith('rgba(')) {
        const match = color.match(/rgba\((\d+),\s*(\d+),\s*(\d+),\s*[\d.]+\)/);
        if (match) {
            return `rgba(${match[1]}, ${match[2]}, ${match[3]}, ${alpha})`;
        }
    }

    // Handle hex format
    if (color.startsWith('#')) {
        const hex = color.replace('#', '');
        const r = parseInt(hex.substring(0, 2), 16);
        const g = parseInt(hex.substring(2, 4), 16);
        const b = parseInt(hex.substring(4, 6), 16);

        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }

    // Default fallback (primary blue)
    return `rgba(18, 67, 227, ${alpha})`;
};
