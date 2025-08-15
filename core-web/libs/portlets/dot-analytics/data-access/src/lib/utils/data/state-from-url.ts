
import { isAfter, isDate } from 'date-fns';

import { ParamMap } from '@angular/router';

import { CUSTOM_TIME_RANGE, TIME_PERIOD_OPTIONS, TIME_RANGE_URL_MAPPING } from './../../constants';

import { TimeRange, TimeRangeInput, TimeRangeOptions } from '../../types';

/**
 * Convert URL-friendly value to internal time range value
 */
export const fromUrlFriendly = (urlValue: string): TimeRange | null => {
    return TIME_RANGE_URL_MAPPING[urlValue as keyof typeof TIME_RANGE_URL_MAPPING] || null;
};

export const getTimeRangeFromUrl = (queryParamMap: ParamMap): TimeRangeInput => {
    const urlTimeRange = queryParamMap.get('time_range');

      // If no time range specified, keep default state
      if (!urlTimeRange) {
        return TimeRangeOptions.LAST_7_DAYS;
      }

      const internalTimeRange = fromUrlFriendly(urlTimeRange);
      if (!internalTimeRange) {
        return TimeRangeOptions.LAST_7_DAYS;
      }

      // Handle custom time range with date validation
      if (internalTimeRange === CUSTOM_TIME_RANGE) {
        return handleCustomTimeRange(queryParamMap);
      }

      // Handle predefined time ranges
      if (isValidTimeRange(internalTimeRange)) {
        return internalTimeRange;
      }

      return TimeRangeOptions.LAST_7_DAYS;
};

/**
 * Validate if the time range from URL is valid
 */
export const isValidTimeRange = (timeRange: string): boolean => {
    return TIME_PERIOD_OPTIONS.some((option) => option.value === timeRange);
};

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

    // Check if from date is before to date
    if (isAfter(fromDateObj, toDateObj)) {
        return false;
    }

    return true;
};

export const handleCustomTimeRange =(queryParams: ParamMap): TimeRangeInput => {
    const fromDate = queryParams.get('from');
    const toDate = queryParams.get('to');

    if (!fromDate || !toDate) {
        return TimeRangeOptions.LAST_7_DAYS;
    }

    if (!isValidCustomDateRange(fromDate, toDate)) {
        return TimeRangeOptions.LAST_7_DAYS;
    }

    return [fromDate, toDate];
  };
