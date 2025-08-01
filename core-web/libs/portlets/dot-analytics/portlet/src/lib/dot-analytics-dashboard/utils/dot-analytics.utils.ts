/**
 * Centralized utilities for analytics dashboard
 */

import {
    DEFAULT_TIME_PERIOD,
    TIME_PERIOD_OPTIONS,
    TIME_RANGE_INTERNAL_MAPPING,
    TIME_RANGE_URL_MAPPING
} from '../constants';

// ============================================================================
// DATE VALIDATION UTILITIES
// ============================================================================

/**
 * Validate that a custom date range has valid dates and proper order
 */
export const isValidCustomDateRange = (fromDate: string, toDate: string): boolean => {
    if (!fromDate || !toDate) {
        return false;
    }

    const fromDateObj = new Date(fromDate);
    const toDateObj = new Date(toDate);

    // Check if dates are valid
    if (isNaN(fromDateObj.getTime()) || isNaN(toDateObj.getTime())) {
        return false;
    }

    // Check if from date is before to date
    if (fromDateObj >= toDateObj) {
        return false;
    }

    return true;
};

/**
 * Validate if the time range from URL is valid
 */
export const isValidTimeRange = (timeRange: string): boolean => {
    return TIME_PERIOD_OPTIONS.some((option) => option.value === timeRange);
};

// ============================================================================
// URL MAPPING UTILITIES
// ============================================================================

/**
 * Convert internal time range value to URL-friendly value
 */
export const toUrlFriendly = (internalValue: string): string => {
    return (
        TIME_RANGE_INTERNAL_MAPPING[internalValue as keyof typeof TIME_RANGE_INTERNAL_MAPPING] ||
        internalValue
    );
};

/**
 * Convert URL-friendly value to internal time range value
 */
export const fromUrlFriendly = (urlValue: string): string => {
    return TIME_RANGE_URL_MAPPING[urlValue as keyof typeof TIME_RANGE_URL_MAPPING] || urlValue;
};

// ============================================================================
// VALIDATION UTILITIES
// ============================================================================

/**
 * Check if a value is a valid date string or Date object
 */
export const isValidDate = (date: string | Date): boolean => {
    const dateObj = typeof date === 'string' ? new Date(date) : date;

    return dateObj instanceof Date && !isNaN(dateObj.getTime());
};

/**
 * Check if a date range has proper order (from < to)
 */
export const isValidDateOrder = (fromDate: string | Date, toDate: string | Date): boolean => {
    const from = typeof fromDate === 'string' ? new Date(fromDate) : fromDate;
    const to = typeof toDate === 'string' ? new Date(toDate) : toDate;

    return from < to;
};

// ============================================================================
// DATE FORMATTING UTILITIES
// ============================================================================

/**
 * Format date to ISO string for URL parameters
 */
export const formatDateForUrl = (date: Date): string => {
    return date.toISOString().split('T')[0]; // YYYY-MM-DD format
};

/**
 * Parse date from URL parameter string
 */
export const parseDateFromUrl = (dateString: string): Date | null => {
    if (!dateString) return null;

    const date = new Date(dateString);

    return isValidDate(date) ? date : null;
};

// ============================================================================
// CONSTANTS UTILITIES
// ============================================================================

/**
 * Get default time period from constants
 */
export const getDefaultTimePeriod = (): string => DEFAULT_TIME_PERIOD;

/**
 * Get available time period options
 */
export const getTimePeriodOptions = () => TIME_PERIOD_OPTIONS;
