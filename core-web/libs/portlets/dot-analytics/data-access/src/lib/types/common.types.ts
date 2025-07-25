/**
 * Common types for analytics data access
 */

import { ComponentStatus } from '@dotcms/dotcms-models';

/**
 * Options for predefined time ranges in analytics
 * These correspond to common analytics reporting periods
 */
export const TimeRangeOptions = {
    TODAY: 'today',
    YESTERDAY: 'yesterday',
    LAST_7_DAYS: 'from 7 days ago to now',
    LAST_30_DAYS: 'from 30 days ago to now',
    CUSTOM_TIME_RANGE: 'CUSTOM_TIME_RANGE'
} as const;

/**
 * Default time range for analytics queries (7 days)
 */
export const DEFAULT_TIME_RANGE: TimeRange = TimeRangeOptions.LAST_7_DAYS;

/**
 * Union type representing all possible time range values
 * Includes predefined ranges and custom time expressions
 */
export type TimeRange = (typeof TimeRangeOptions)[keyof typeof TimeRangeOptions];

/** Date range for custom time period selection as ISO date strings */
export type DateRange = [string, string];

/** Union type for time range inputs - supports both predefined ranges and custom date arrays */
export type TimeRangeInput = TimeRange | DateRange;

/**
 * API Response type with generic entity
 */
export interface AnalyticsApiResponse<T = unknown> {
    entity: T[];
    errors: unknown[];
    i18nMessagesMap: Record<string, unknown>;
    messages: unknown[];
    pagination: unknown | null;
    permissions: unknown[];
}

/**
 * Individual request state interface
 * Generic interface for managing the state of API requests
 */
export interface RequestState<T = unknown> {
    status: ComponentStatus;
    data: T | null;
    error: string | null;
}

/**
 * Metric data structure for dashboard components
 * Used to display analytics metrics with consistent format
 */
export interface MetricData {
    name: string;
    value: number;
    subtitle: string;
    icon: string;
    status: ComponentStatus;
    error: string | null;
}

/**
 * Base initial request state
 * Default state for RequestState interface
 */
export const INITIAL_REQUEST_STATE: RequestState = {
    status: ComponentStatus.INIT,
    data: null,
    error: null
} as const;
