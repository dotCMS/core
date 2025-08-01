import { TimeRange, TimeRangeOptions } from '@dotcms/portlets/dot-analytics/data-access';

import { FilterOption } from '../types';

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

/** Available time period options for analytics data filtering */
export const TIME_PERIOD_OPTIONS: FilterOption[] = [
    { label: 'analytics.filters.time-period.today', value: 'today' },
    { label: 'analytics.filters.time-period.yesterday', value: 'yesterday' },
    { label: 'analytics.filters.time-period.last-7-days', value: 'from 7 days ago to now' },
    { label: 'analytics.filters.time-period.last-30-days', value: 'from 30 days ago to now' },
    { label: 'analytics.filters.time-period.custom', value: CUSTOM_TIME_RANGE }
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
