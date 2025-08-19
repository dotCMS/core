import { Params, ParamMap } from '@angular/router';

import { TIME_RANGE_OPTIONS } from '@dotcms/portlets/dot-analytics/data-access';

import { getValidTimeRangeUrl, isValidCustomDateRange } from './dot-analytics.utils';

import { TimeRangeInput, DateRange } from '../types';

// Type definitions for better type safety
type QueryParamsResult =
    | { type: 'params'; params: Params }
    | { type: 'timeRange'; timeRange: TimeRangeInput };

/**
 * Creates default query parameters with the standard time range
 */
const createDefaultQueryParams = (): QueryParamsResult => ({
    type: 'params',
    params: {
        time_range: TIME_RANGE_OPTIONS.last7days
    }
});

/**
 * Validates custom time range parameters and returns appropriate query params
 */
const validateCustomTimeRange = (queryParamMap: ParamMap): QueryParamsResult => {
    const fromDate = queryParamMap.get('from');
    const toDate = queryParamMap.get('to');

    // Early return if either date is missing
    if (!fromDate || !toDate) {
        return createDefaultQueryParams();
    }

    // Validate date range
    if (!isValidCustomDateRange(fromDate, toDate)) {
        return createDefaultQueryParams();
    }

    return {
        type: 'timeRange',
        timeRange: [fromDate, toDate] as DateRange
    };
};

/**
 * Extracts and validates query parameters from URL to determine if default parameters should be applied.
 *
 * This function analyzes the current URL parameters and returns:
 * - A new `Params` object with default values if current parameters are invalid or missing
 * - A `TimeRangeInput` if the current parameters are valid
 *
 * @param queryParamMap - Angular router's ParamMap containing current URL query parameters
 * @returns QueryParamsResult with either default params or valid time range
 */
export const getProperQueryParamsFromUrl = (queryParamMap: ParamMap): QueryParamsResult => {
    // Early return for missing time range parameter
    const urlTimeRange = queryParamMap.get('time_range');
    if (!urlTimeRange) {
        return createDefaultQueryParams();
    }

    // Validate time range format
    const timeRangeUrl = getValidTimeRangeUrl(urlTimeRange);
    if (!timeRangeUrl) {
        return createDefaultQueryParams();
    }

    // Handle custom time range separately
    if (timeRangeUrl === TIME_RANGE_OPTIONS.custom) {
        return validateCustomTimeRange(queryParamMap);
    }

    // Handle predefined time ranges
    return {
        type: 'timeRange',
        timeRange: timeRangeUrl
    };
};
