import { Params } from '@angular/router';

import {
    DASHBOARD_TABS,
    DashboardTab,
    TIME_RANGE_DAYS_MAP,
    TIME_RANGE_OPTIONS
} from '../constants';
import { TimeRange, TimeRangeInput } from '../types';

const VALID_TIME_RANGE_VALUES: readonly string[] = Object.values(TIME_RANGE_OPTIONS);

/**
 * Validates if a string is a valid DashboardTab
 *
 * @param tab - The string to validate
 * @returns True if the string is a valid DashboardTab, false otherwise
 */
export function isValidTab(tab: string): tab is DashboardTab {
    return Object.values(DASHBOARD_TABS).includes(tab as DashboardTab);
}

/**
 * Converts URL query params to TimeRangeInput.
 *
 * @param params - The query params from the route
 * @returns A TimeRangeInput (either a predefined range string or a custom date array)
 */
export function paramsToTimeRange(params: Params | null | undefined): TimeRangeInput {
    if (!params) {
        return TIME_RANGE_OPTIONS.last7days;
    }

    const timeRange: string = params['time_range'];

    // Only return custom date range if both from and to are provided
    if (timeRange === TIME_RANGE_OPTIONS.custom && params['from'] && params['to']) {
        return [params['from'], params['to']];
    }

    // If time_range is custom but missing from/to, fall back to default
    if (timeRange === TIME_RANGE_OPTIONS.custom) {
        return TIME_RANGE_OPTIONS.last7days;
    }

    if (timeRange && VALID_TIME_RANGE_VALUES.includes(timeRange)) {
        return timeRange as TimeRange;
    }

    return TIME_RANGE_OPTIONS.last7days;
}

/** Return type for getComparisonLabel — i18n key with optional args */
export interface ComparisonLabelInfo {
    key: string;
    args: string[];
}

/**
 * Returns an i18n key and args for the comparison label based on the current time range.
 *
 * @param timeRange - The current time range input
 * @returns An object with `key` (i18n key) and `args` (substitution values)
 */
export function getComparisonLabel(timeRange: TimeRangeInput): ComparisonLabelInfo {
    if (Array.isArray(timeRange)) {
        return { key: 'analytics.metrics.comparison.previous-range', args: [] };
    }

    const days = TIME_RANGE_DAYS_MAP[timeRange];

    return days !== undefined
        ? { key: 'analytics.metrics.comparison.previous-days', args: [String(days)] }
        : { key: 'analytics.metrics.comparison.previous-range', args: [] };
}
