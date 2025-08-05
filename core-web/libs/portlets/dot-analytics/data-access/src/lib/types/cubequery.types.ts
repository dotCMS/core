/**
 * CubeJS query types and interfaces
 */

/**
 * Sort direction options for ordering queries.
 * Using const assertion for sort direction management.
 */
const SortDirection = {
    ASC: 'asc',
    DESC: 'desc'
} as const;

export type SortDirection = (typeof SortDirection)[keyof typeof SortDirection];

/**
 * Granularity options for time-based queries.
 * Using const assertion for granularity management.
 */
const Granularity = {
    HOUR: 'hour',
    DAY: 'day',
    WEEK: 'week',
    MONTH: 'month'
} as const;

export type Granularity = (typeof Granularity)[keyof typeof Granularity];

/**
 * CubeJS time dimension configuration
 */
export interface CubeJSTimeDimension {
    dimension: string;
    dateRange: string | [string, string];
    granularity?: Granularity;
}

/**
 * CubeJS filter configuration
 */
export interface CubeJSFilter {
    member: string;
    operator: FilterOperator;
    values: string[];
}

/**
 * Dimension fields for CubeJS queries.
 * Using const assertion for field name management.
 * These are the field names used in CubeJS queries, not API responses.
 */
const DimensionField = {
    PATH: 'path',
    PAGE_TITLE: 'pageTitle',
    USER_AGENT: 'userAgent',
    CREATED_AT: 'createdAt',
    EVENT_TYPE: 'eventType'
} as const;

export type DimensionField = (typeof DimensionField)[keyof typeof DimensionField];

/**
 * Measure fields for CubeJS aggregations.
 * Using const assertion for measure management.
 */
const MeasureField = {
    TOTAL_REQUEST: 'totalRequest',
    TOTAL_SESSIONS: 'totalSessions',
    TOTAL_USERS: 'totalUsers'
} as const;

export type MeasureField = (typeof MeasureField)[keyof typeof MeasureField];

/**
 * Filter fields for CubeJS filtering.
 * Using const assertion for filter field management.
 */
const FilterField = {
    EVENT_TYPE: 'eventType',
    PATH: 'path',
    USER_AGENT: 'userAgent',
    SITE_ID: 'siteId'
} as const;

export type FilterField = (typeof FilterField)[keyof typeof FilterField];

/**
 * Order fields for CubeJS sorting.
 * Using const assertion for order field management.
 */
const OrderField = {
    TOTAL_REQUEST: 'totalRequest',
    TOTAL_SESSIONS: 'totalSessions',
    CREATED_AT: 'createdAt',
    PATH: 'path',
    PAGE_TITLE: 'pageTitle'
} as const;

export type OrderField = (typeof OrderField)[keyof typeof OrderField];

/**
 * Filter operators for CubeJS filtering.
 * Using const assertion for filter operator management.
 */
const FilterOperator = {
    EQUALS: 'equals',
    NOT_EQUALS: 'notEquals',
    CONTAINS: 'contains',
    GT: 'gt',
    GTE: 'gte',
    LT: 'lt',
    LTE: 'lte'
} as const;

export type FilterOperator = (typeof FilterOperator)[keyof typeof FilterOperator];

/**
 * Complete CubeJS query interface
 */
export interface CubeJSQuery {
    dimensions?: string[];
    measures?: string[];
    order?: Record<string, SortDirection>;
    timeDimensions?: CubeJSTimeDimension[];
    filters?: CubeJSFilter[];
    limit?: number;
}
