import { Params } from '@angular/router';

import { DASHBOARD_TABS, DashboardTab, TIME_RANGE_OPTIONS } from '../constants';
import { TimeRangeInput } from '../types';

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
 * Converts URL query params to TimeRangeInput
 *
 * @param params - The query params from the route
 * @returns A TimeRangeInput (either a predefined range string or a custom date array)
 */
export function paramsToTimeRange(params: Params | null | undefined): TimeRangeInput {
    if (!params) {
        return TIME_RANGE_OPTIONS.last7days;
    }

    // Only return custom date range if both from and to are provided
    if (params['time_range'] === TIME_RANGE_OPTIONS.custom && params['from'] && params['to']) {
        return [params['from'], params['to']];
    }

    // If time_range is custom but missing from/to, fall back to default
    if (params['time_range'] === TIME_RANGE_OPTIONS.custom) {
        return TIME_RANGE_OPTIONS.last7days;
    }

    return params['time_range'] || TIME_RANGE_OPTIONS.last7days;
}
