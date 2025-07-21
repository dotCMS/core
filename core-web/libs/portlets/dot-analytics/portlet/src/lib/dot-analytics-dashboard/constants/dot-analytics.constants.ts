import { TimeRange } from '@dotcms/portlets/dot-analytics/data-access';

import { FilterOption } from '../types';

/** Available time period options for analytics data filtering */
export const TIME_PERIOD_OPTIONS: FilterOption[] = [
    { label: 'analytics.filters.time-period.1week', value: 'from 7 days ago to now' },
    { label: 'analytics.filters.time-period.1month', value: 'from 30 days ago to now' },
    { label: 'analytics.filters.time-period.this-week', value: 'this week' }
];

/** Default time period selection (1 week) */
export const DEFAULT_TIME_PERIOD: TimeRange = 'from 7 days ago to now';

/** Analytics entity field keys */
export const ANALYTICS_KEYS = {
    TOTAL_REQUEST: 'request.totalRequest',
    TOTAL_SESSIONS: 'request.totalSessions',
    PAGE_TITLE: 'request.pageTitle'
} as const;

/** Table configuration constants */
export const TABLE_CONFIG = {
    DEFAULT_ROWS: 10,
    ROWS_PER_PAGE_OPTIONS: [10, 25, 50],
    SORT_MODE: 'multiple',
    DATA_KEY: 'path'
} as const;

/** Table column configuration for top pages analytics table */
export const TOP_PAGES_TABLE_COLUMNS = [
    {
        field: 'pageTitle',
        header: 'analytics.table.headers.title',
        type: 'text',
        alignment: 'left',
        sortable: true
    },
    {
        field: 'path',
        header: 'analytics.table.headers.page-url',
        type: 'link',
        alignment: 'left',
        sortable: true
    },
    {
        field: 'views',
        header: 'analytics.table.headers.pageviews',
        type: 'number',
        alignment: 'center',
        sortable: true
    }
] as const;
