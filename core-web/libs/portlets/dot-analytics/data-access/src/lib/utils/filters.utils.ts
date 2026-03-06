import { Params } from '@angular/router';

import { DASHBOARD_TABS, DashboardTab, TIME_RANGE_OPTIONS } from '../constants';
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

/** Time range values that are no longer supported as URL params */
const EXCLUDED_TIME_RANGE_PARAMS: string[] = [
    TIME_RANGE_OPTIONS.today,
    TIME_RANGE_OPTIONS.yesterday
];

/**
 * Converts URL query params to TimeRangeInput.
 * `today` and `yesterday` are excluded and fall back to `last7days`.
 *
 * @param params - The query params from the route
 * @returns A TimeRangeInput (either a predefined range string or a custom date array)
 */
export function paramsToTimeRange(params: Params | null | undefined): TimeRangeInput {
    if (!params) {
        return TIME_RANGE_OPTIONS.last7days;
    }

    const timeRange: string = params['time_range'];

    // Excluded values fall back to the default
    if (EXCLUDED_TIME_RANGE_PARAMS.includes(timeRange)) {
        return TIME_RANGE_OPTIONS.last7days;
    }

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
