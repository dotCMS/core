/**
 * Common types for analytics data access
 */

import { ComponentStatus } from '@dotcms/dotcms-models';

/**
 * Time range options for analytics filtering.
 * Using const assertion for time range management.
 */
const TimeRange = {
    LAST_7_DAYS: 'from 7 days ago to now',
    LAST_30_DAYS: 'from 30 days ago to now'
} as const;

/**
 * Time range options for analytics filtering.
 */
export type TimeRange = (typeof TimeRange)[keyof typeof TimeRange];

/**
 * Default time range for analytics queries.
 */
export const DEFAULT_TIME_RANGE: TimeRange = 'from 7 days ago to now';

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
