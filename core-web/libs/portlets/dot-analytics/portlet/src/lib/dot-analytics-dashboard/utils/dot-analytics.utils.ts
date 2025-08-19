import { isBefore, isDate, isSameDay } from 'date-fns';

import { TimeRange, TIME_RANGE_OPTIONS } from '@dotcms/portlets/dot-analytics/data-access';

/**
 * Validates custom date range parameters
 * @param fromDate - Start date string (ISO format)
 * @param toDate - End date string (ISO format)
 * @returns true if the date range is valid
 */
export const isValidCustomDateRange = (fromDate: string, toDate: string): boolean => {
    const fromDateObj = new Date(`${fromDate}T00:00:00.000`);
    const toDateObj = new Date(`${toDate}T00:00:00.000`);

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
