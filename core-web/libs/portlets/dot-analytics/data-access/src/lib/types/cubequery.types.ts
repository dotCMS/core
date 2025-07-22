/**
 * CubeJS query types and interfaces
 */

/**
 * CubeJS time dimension configuration
 */
export interface CubeJSTimeDimension {
    dimension: string;
    dateRange: string | [string, string];
    granularity?: 'day' | 'week' | 'month';
}

/**
 * CubeJS filter configuration
 */
export interface CubeJSFilter {
    member: string;
    operator: 'equals' | 'notEquals' | 'contains' | 'gt' | 'gte' | 'lt' | 'lte';
    values: string[];
}

/**
 * Complete CubeJS query interface
 */
export interface CubeJSQuery {
    dimensions?: string[];
    measures?: string[];
    order?: Record<string, 'asc' | 'desc'>;
    timeDimensions?: CubeJSTimeDimension[];
    filters?: CubeJSFilter[];
    limit?: number;
}

/**
 * Field types for the query builder
 */
export type DimensionField = 'path' | 'pageTitle' | 'userAgent' | 'createdAt' | 'eventType';

export type MeasureField = 'totalRequest' | 'totalSessions' | 'totalUser';

export type FilterField = 'eventType' | 'path' | 'userAgent';

export type OrderField = 'totalRequest' | 'totalSessions' | 'createdAt' | 'path' | 'pageTitle';
