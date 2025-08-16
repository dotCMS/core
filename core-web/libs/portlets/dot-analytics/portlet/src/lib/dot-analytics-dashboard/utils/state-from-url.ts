import { Params, ParamMap } from '@angular/router';

import { CUSTOM_TIME_RANGE, TIME_RANGE_INTERNAL_MAPPING } from './../constants';
import { fromUrlFriendly, isValidCustomDateRange } from './dot-analytics.utils';

/**
 * Creates default query parameters with the standard time range
 */
const createDefaultQueryParams = (): Params => {
    return {
        time_range: TIME_RANGE_INTERNAL_MAPPING['from 7 days ago to now']
    }
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
export const getProperQueryParamsFromUrl = (queryParamMap: ParamMap): Params | null => {
    const urlTimeRange = queryParamMap.get('time_range');

    if (!urlTimeRange) {
        return createDefaultQueryParams();
    }

    const internalTimeRange = fromUrlFriendly(urlTimeRange);
    if (!internalTimeRange) {
        return createDefaultQueryParams();
    }

    if (internalTimeRange === CUSTOM_TIME_RANGE) {
        return validateCustomTimeRange(queryParamMap);
    }

    return null;
};



/**
 * Validates custom time range parameters and returns appropriate query params
 */
const validateCustomTimeRange = (queryParamMap: ParamMap): Params | null => {
    const fromDate = queryParamMap.get('from');
    const toDate = queryParamMap.get('to');
    if (!fromDate || !toDate) {
        return createDefaultQueryParams();
    }

    if (!isValidCustomDateRange(fromDate, toDate)) {
        return createDefaultQueryParams();
    }

    return null;
};


