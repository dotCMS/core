/**
 * Common types for analytics data access
 */

/**
 * Time range options for analytics filtering.
 * Using const assertion for time range management.
 */
const TimeRange = {
    LAST_7_DAYS: 'from 7 days ago to now',
    LAST_30_DAYS: 'from 30 days ago to now',
    THIS_WEEK: 'this week'
} as const;

export type TimeRange = (typeof TimeRange)[keyof typeof TimeRange];

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
