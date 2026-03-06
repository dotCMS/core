import { TimeRange, TIME_RANGE_OPTIONS } from '@dotcms/portlets/dot-analytics/data-access';

import { FilterOption } from '../types';

/** Available time period options for analytics data filtering */
export const TIME_PERIOD_OPTIONS: FilterOption[] = [
    { label: 'analytics.filters.time-period.last-7-days', value: TIME_RANGE_OPTIONS.last7days },
    { label: 'analytics.filters.time-period.last-30-days', value: TIME_RANGE_OPTIONS.last30days },
    { label: 'analytics.filters.time-period.custom', value: TIME_RANGE_OPTIONS.custom }
];

/** Default time period selection (1 week) */
export const DEFAULT_TIME_PERIOD: TimeRange = TIME_RANGE_OPTIONS.last7days;

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
    ROWS_PER_PAGE: 10
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
        alignment: 'right',
        sortable: true,
        width: '15%'
    }
] as const;
