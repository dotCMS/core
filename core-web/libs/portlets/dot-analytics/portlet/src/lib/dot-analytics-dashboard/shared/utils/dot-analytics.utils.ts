import { isBefore, isDate, isSameDay, parse } from 'date-fns';

import { TIME_RANGE_OPTIONS, TimeRange } from '@dotcms/portlets/dot-analytics/data-access';

/**
 * Validates custom date range parameters
 * @param fromDate - Start date string (ISO format)
 * @param toDate - End date string (ISO format)
 * @returns true if the date range is valid
 */
export const isValidCustomDateRange = (fromDate: string, toDate: string): boolean => {
    const fromDateObj = parse(fromDate, 'yyyy-MM-dd', new Date());
    const toDateObj = parse(toDate, 'yyyy-MM-dd', new Date());

    // Ensure dates are valid Date objects
    if (!isDate(fromDateObj) || !isDate(toDateObj)) {
        return false;
    }

    // Check if from date is before or equal to to date
    return isBefore(fromDateObj, toDateObj) || isSameDay(fromDateObj, toDateObj);
};

/**
 * Validates and returns a valid time range from URL value
 * @param urlValue - URL parameter value for time range
 * @returns Valid TimeRange or null if invalid
 */
export const getValidTimeRangeUrl = (urlValue: string): TimeRange | null => {
    if (!urlValue || typeof urlValue !== 'string') {
        return null;
    }

    return Object.keys(TIME_RANGE_OPTIONS).includes(urlValue) ? (urlValue as TimeRange) : null;
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
