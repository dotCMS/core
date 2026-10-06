/**
 * Types for the domain-driven analytics query API (dotCMS/core#36628): the unified tabular
 * envelope returned by `/api/v1/analytics/events`, `/api/v1/analytics/sessions`, and
 * `/api/v1/analytics/content`. Distinct from the per-endpoint DTOs in `analytics-api.types.ts`,
 * which are the shapes `DotAnalyticsService` still exposes publicly — each migrated method maps
 * this envelope back into those DTOs so store features don't need to change.
 */

/** The only two column kinds the envelope ever uses. */
export type AnalyticsColumnType = 'DIMENSION' | 'METRIC';

/** Describes one field present in every `rows` entry, in display order (dimensions before metrics). */
export interface AnalyticsColumnDescriptor {
    name: string;
    type: AnalyticsColumnType;
}

/** Present only when `page`/`pageSize` were sent on the request. */
export interface AnalyticsPagination {
    page: number;
    pageSize: number;
    totalPages: number;
    totalItems: number;
}

/** Echoed, resolved request params. The tenant is never included — it comes from the auth token. */
export interface AnalyticsResponseParams {
    project: string;
    siteId?: string;
    eventType?: string;
    identifier?: string;
    title?: string;
    conversionName?: string;
    from?: string;
    to?: string;
    metrics?: string[];
    dimensions?: string[];
    orderBy?: string;
    orderDir?: string;
}

/**
 * The unified envelope returned by all three domain-driven query resources.
 * `rows` is always an array (never null); a scalar query (no `dimensions` requested) returns a
 * single-row array and omits `totals`. Rate/average metrics in `totals` are ratio-of-sums over
 * the whole window, not an average of per-row values.
 */
export interface AnalyticsQueryResponse<
    TRow extends Record<string, string | number> = Record<string, string | number>
> {
    params: AnalyticsResponseParams;
    columns: AnalyticsColumnDescriptor[];
    rows: TRow[];
    totals?: Record<string, number>;
    pagination?: AnalyticsPagination;
}
