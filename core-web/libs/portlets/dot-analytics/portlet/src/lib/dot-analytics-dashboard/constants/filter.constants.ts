import { FilterOption } from '../types';

/** Available time period options for analytics data filtering */
export const TIME_PERIOD_OPTIONS: FilterOption[] = [
    { label: 'analytics.filters.time-period.1week', value: '1week' },
    { label: 'analytics.filters.time-period.2weeks', value: '2weeks' },
    { label: 'analytics.filters.time-period.1month', value: '1month' },
    { label: 'analytics.filters.time-period.3months', value: '3months' }
];

/** Default time period selection (1 week) */
export const DEFAULT_TIME_PERIOD = '1week';
