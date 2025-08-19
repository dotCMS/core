import { Params, ParamMap } from '@angular/router';

import { TIME_RANGE_OPTIONS } from '@dotcms/portlets/dot-analytics/data-access';

import { getValidTimeRangeUrl, isValidCustomDateRange } from './dot-analytics.utils';

import { TimeRangeInput, DateRange } from '../types';

/**
 * Creates default query parameters with the standard time range
 */
const createDefaultQueryParams = (): { type: 'params'; params: Params } => {
    return {
        type: 'params',
        params: {
            time_range: TIME_RANGE_OPTIONS.last7days
        }
    };
};

/**
 * Extracts and validates query parameters from URL to determine if default parameters should be applied.
 *
 * This function analyzes the current URL parameters and returns:
 * - `null` if the current parameters are valid and complete
 * - A new `URLSearchParams` object with default values if current parameters are invalid or missing
 *
 * @param queryParamMap - Angular router's ParamMap containing current URL query parameters
 * @returns `null` if current params are valid, or `URLSearchParams` with default values
 */
export const getProperQueryParamsFromUrl = (
    queryParamMap: ParamMap
): { type: 'params'; params: Params } | { type: 'timeRange'; timeRange: TimeRangeInput } => {
    const urlTimeRange = queryParamMap.get('time_range');

    if (!urlTimeRange) {
        return createDefaultQueryParams();
    }

    const timeRangeUrl = getValidTimeRangeUrl(urlTimeRange);
    if (!timeRangeUrl) {
        return createDefaultQueryParams();
    }

    if (timeRangeUrl === TIME_RANGE_OPTIONS.custom) {
        return validateCustomTimeRange(queryParamMap);
    }

    return {
        type: 'timeRange',
        timeRange: timeRangeUrl
    };
};

/**
 * Validates custom time range parameters and returns appropriate query params
 */
const validateCustomTimeRange = (
    queryParamMap: ParamMap
): { type: 'params'; params: Params } | { type: 'timeRange'; timeRange: DateRange } => {
    const fromDate = queryParamMap.get('from');
    const toDate = queryParamMap.get('to');
    if (!fromDate || !toDate) {
        return createDefaultQueryParams();
    }

    if (!isValidCustomDateRange(fromDate, toDate)) {
        return createDefaultQueryParams();
    }

    return {
        type: 'timeRange',
        timeRange: [fromDate, toDate]
    };
};
