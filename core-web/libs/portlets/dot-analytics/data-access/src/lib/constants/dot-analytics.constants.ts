import { TimeRange, TimeRangeOptions } from '../types';

export interface FilterOption {
    /** Display text for the option */
    label: string;
    /** Internal value for the option */
    value: TimeRange;
}

export const CUSTOM_TIME_RANGE = 'CUSTOM_TIME_RANGE';

/** Mapping between URL-friendly values and internal CubeJS values */
export const TIME_RANGE_URL_MAPPING = {
    // URL-friendly → Internal CubeJS value
    today: 'today',
    yesterday: 'yesterday',
    last7days: 'from 7 days ago to now',
    last30days: 'from 30 days ago to now',
    custom: CUSTOM_TIME_RANGE
} as const;

/** Reverse mapping for Internal → URL-friendly */
export const TIME_RANGE_INTERNAL_MAPPING = {
    // Internal CubeJS value → URL-friendly
    today: 'today',
    yesterday: 'yesterday',
    'from 7 days ago to now': 'last7days',
    'from 30 days ago to now': 'last30days',
    [CUSTOM_TIME_RANGE]: 'custom'
} as const;

/** Default time period selection (1 week) */
export const DEFAULT_TIME_PERIOD = TimeRangeOptions.LAST_7_DAYS;

/** Available time period options for analytics data filtering */
export const TIME_PERIOD_OPTIONS: FilterOption[] = [
    { label: 'analytics.filters.time-period.today', value: 'today' },
    { label: 'analytics.filters.time-period.yesterday', value: 'yesterday' },
    { label: 'analytics.filters.time-period.last-7-days', value: 'from 7 days ago to now' },
    { label: 'analytics.filters.time-period.last-30-days', value: 'from 30 days ago to now' },
    { label: 'analytics.filters.time-period.custom', value: CUSTOM_TIME_RANGE }
];
