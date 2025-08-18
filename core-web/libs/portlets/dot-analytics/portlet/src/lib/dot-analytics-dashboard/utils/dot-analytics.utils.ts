/**
 * Centralized utilities for analytics dashboard
 */

import { isBefore, isDate, isSameDay } from 'date-fns';

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
    if (!isDate(fromDateObj) || !isDate(toDateObj)) {
        return false;
    }

    return isBefore(fromDateObj, toDateObj) || isSameDay(fromDateObj, toDateObj);
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
