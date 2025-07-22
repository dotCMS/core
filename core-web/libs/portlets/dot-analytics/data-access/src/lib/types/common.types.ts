/**
 * Common types for analytics data access
 */

/**
 * Time range type for analytics queries
 */
export type TimeRange = 'from 7 days ago to now' | 'from 30 days ago to now' | 'this week';

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
