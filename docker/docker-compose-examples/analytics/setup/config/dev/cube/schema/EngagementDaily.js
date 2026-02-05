/**
 * =====================================================================================
 * 1) EngagementDaily Cube
 * =====================================================================================
 *
 * Source table:
 *   clickhouse_test_db.engagement_daily
 *
 * Grain:
 *   One row per (customer_id, cluster_id, context_site_id, day).
 *
 * Primary use cases:
 * - KPI cards:
 *     - Engagement Rate
 *     - Avg Interactions (Engaged)
 *     - Avg Session Time
 *     - Total Sessions
 *     - Conversion Rate (Engaged conversions / Total sessions)
 * - Trend charts:
 *     - Engaged sessions per day
 *     - Total sessions per day
 *
 * Why this cube is fast:
 * - The table is already rolled up daily in ClickHouse.
 * - Queries over date ranges become sums over days, not scans of sessions/events.
 *
 * Correctness notes:
 * - Rates must be computed as ratio-of-sums over the selected period.
 *   Example:
 *     engagementRate = sum(engaged_sessions) / sum(total_sessions)
 *   Do NOT average daily engagement rates.
 */
cube(`EngagementDaily`, {
    sql: `
    SELECT
      customer_id,
      context_site_id,
      day,
      total_sessions,
      engaged_sessions,
      engaged_conversion_sessions,
      total_events_all,
      total_duration_all,
      total_events_engaged,
      total_duration_engaged,
      updated_at
    FROM clickhouse_test_db.engagement_daily
  `,

    measures: {
        // ---------------------------
        // Base counts (raw sums)
        //
        // Base totals (these are additive; safe to SUM over any date range)
        // ---------------------------
        totalSessions: {
            sql: `total_sessions`,
            type: `sum`,
            title: `Total Sessions`,
            description: `Total sessions in the selected period (sum of daily totals).`
        },

        engagedSessions: {
            sql: `engaged_sessions`,
            type: `sum`,
            title: `Engaged Sessions`,
            description: `Number of engaged sessions in the selected period.`
        },

        engagedConversionSessions: {
            sql: `engaged_conversion_sessions`,
            type: `sum`,
            title: `Engaged Sessions With Conversion`,
            description: `Engaged sessions that contain at least one conversion event.`
        },

        // ---------------------------
        // Base sums for averages
        // ---------------------------
        totalEventsAll: {
            sql: `total_events_all`,
            type: `sum`,
            title: `Total Events (All Sessions)`,
            description: `Sum of total events across ALL sessions (engaged + non-engaged).`
        },

        totalDurationAllSeconds: {
            sql: `total_duration_all`,
            type: `sum`,
            title: `Total Session Duration (Seconds, All Sessions)`,
            description: `Sum of duration_seconds across ALL sessions.`
        },

        totalEventsEngaged: {
            sql: `total_events_engaged`,
            type: `sum`,
            title: `Total Events (Engaged Sessions)`,
            description: `Sum of total events across ENGAGED sessions only.`
        },

        totalDurationEngagedSeconds: {
            sql: `total_duration_engaged`,
            type: `sum`,
            title: `Total Session Duration (Seconds, Engaged Sessions)`,
            description: `Sum of duration_seconds across ENGAGED sessions only.`
        },

        // ---------------------------
        // KPI Ratios (ratio of sums)
        // ---------------------------
        // === Derived KPI measures (computed as ratio of sums; correct for any time range) ===
        engagementRate: {
            // engaged sessions / total sessions
            sql: `sum(engaged_sessions) / nullIf(sum(total_sessions), 0)`,
            type: `number`,
            format: `percent`,
            title: `Engagement Rate`,
            description: `Engaged Sessions / Total Sessions (ratio of sums over the selected period).`,
        },

        conversionRate: {
            // engaged sessions with >=1 conversion / total sessions (your definition)
            sql: `sum(engaged_conversion_sessions) / nullIf(sum(total_sessions), 0)`,
            type: `number`,
            format: `percent`,
            title: `Conversion Rate`,
            description: `Engaged sessions with >=1 conversion / Total sessions.`
        },

        avgInteractionsPerEngagedSession: {
            // average events per engaged session
            sql: `sum(total_events_engaged) / nullIf(sum(engaged_sessions), 0)`,
            type: `number`,
            title: `Avg Interactions per Engaged Session`,
            description: `Total events in engaged sessions / engaged sessions.`
        },

        avgSessionTimeSeconds: {
            // average session duration (seconds) over all sessions
            sql: `sum(total_duration_all) / nullIf(sum(total_sessions), 0)`,
            type: `number`,
            title: `Avg Engaged Session Time (Seconds)`,
            description: `Average time per session over ALL sessions: total_duration_all / total_sessions.`
        },

        avgEngagedSessionTimeSeconds: {
            // average duration among engaged sessions only (often useful)
            sql: `sum(total_duration_engaged) / nullIf(sum(engaged_sessions), 0)`,
            type: `number`,
            title: `Avg Engaged Session Time (Seconds)`,
            description: `Average time per ENGAGED session: total_duration_engaged / engaged_sessions.`
        },
    },

    dimensions: {
        customerId: {
            sql: `customer_id`,
            type: `string`,
            title: `Customer Id`,
            description: `Tenant identifier. Always filter by this in production.`
        },

        clusterId: {
            sql: `cluster_id`,
            type: `string`,
            title: `Cluster Id`,
            description: `Environment/cluster identifier (prod/stage/etc.). Filter when needed.`,
        },

        contextSiteId: {
            sql: `context_site_id`,
            type: `string`,
            title: `Site Id`,
            description: `dotCMS Site identifier (context_site_id)`
        },

        day: {
            sql: `day`,
            type: `time`,
            title: `Day`,
            description: `Day grain used for filtering and trends. Use granularity: day.`
        },

        updatedAt: {
            sql: `updated_at`,
            type: `time`,
            title: `Updated At`,
            description: `When this rollup row was last recomputed by the refreshable MV.`
        },
    },
});
