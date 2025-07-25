import { TimeRange, TimeRangeOptions } from '@dotcms/portlets/dot-analytics/data-access';

import { FilterOption } from '../types';

/** Available time period options for analytics data filtering */
export const TIME_PERIOD_OPTIONS: FilterOption[] = [
    { label: 'analytics.filters.time-period.today', value: 'today' },
    { label: 'analytics.filters.time-period.yesterday', value: 'yesterday' },
    { label: 'analytics.filters.time-period.last-7-days', value: 'from 7 days ago to now' },
    { label: 'analytics.filters.time-period.last-30-days', value: 'from 30 days ago to now' },
    { label: 'analytics.filters.time-period.custom', value: 'CUSTOM_TIME_RANGE' }
];

/** Default time period selection (1 week) */
export const DEFAULT_TIME_PERIOD: TimeRange = TimeRangeOptions.LAST_7_DAYS;

/** Analytics entity field keys */
export const ANALYTICS_KEYS = {
    TOTAL_REQUEST: 'request.totalRequest',
    TOTAL_SESSIONS: 'request.totalSessions',
    PAGE_TITLE: 'request.pageTitle'
} as const;

/** Table configuration constants */
export const TABLE_CONFIG = {
    SORT_MODE: 'multiple',
    DATA_KEY: 'path',
    // Virtual scroll configuration
    VIRTUAL_SCROLL_ITEM_SIZE: 46,
    SCROLL_HEIGHT: '23.125rem'
} as const;

/** Table column configuration for top pages analytics table */
export const TOP_PAGES_TABLE_COLUMNS = [
    {
        field: 'pageTitle',
        header: 'analytics.table.headers.title',
        type: 'text',
        alignment: 'left',
        sortable: true,
        width: '50%'
    },
    {
        field: 'path',
        header: 'analytics.table.headers.page-url',
        type: 'link',
        alignment: 'left',
        sortable: true,
        width: '35%'
    },
    {
        field: 'views',
        header: 'analytics.table.headers.pageviews',
        type: 'number',
        alignment: 'center',
        sortable: true,
        width: '15%'
    }
] as const;
