/**
 * Common types for analytics data access
 */

import { ComponentStatus } from '@dotcms/dotcms-models';

import {
    TIME_RANGE_CUBEJS_MAPPING,
    TIME_RANGE_OPTIONS
} from '../constants/dot-analytics.constants';

/**
 * Union type representing all possible time range values
 * Includes predefined ranges and custom time expressions
 */
export type TimeRange = (typeof TIME_RANGE_OPTIONS)[keyof typeof TIME_RANGE_OPTIONS];

/** Date range for custom time period selection as ISO date strings */
export type DateRange = [string, string];

/**
 * Union type representing all possible time range values for Cube.js
 * Includes predefined ranges and custom time expressions
 */
export type TimeRangeCubeJS =
    | (typeof TIME_RANGE_CUBEJS_MAPPING)[keyof typeof TIME_RANGE_CUBEJS_MAPPING]
    | DateRange;

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
    value: number | string;
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
