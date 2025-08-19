import { isBefore, isDate, isSameDay } from 'date-fns';

import { TimeRange, TIME_RANGE_OPTIONS } from '@dotcms/portlets/dot-analytics/data-access';

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

export const getValidTimeRangeUrl = (urlValue: string): TimeRange | null => {
    return Object.keys(TIME_RANGE_OPTIONS).includes(urlValue) ? (urlValue as TimeRange) : null;
};
