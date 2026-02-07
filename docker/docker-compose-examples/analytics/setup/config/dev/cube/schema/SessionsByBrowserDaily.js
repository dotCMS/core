/**
 * =====================================================================================
 * 3) SessionsByBrowserDaily Cube
 * =====================================================================================
 *
 * Source table:
 *   clickhouse_test_db.sessions_by_browser_daily
 *
 * Grain:
 *   One row per (customer_id, cluster_id, context_site_id, day, browser_family).
 *
 * Widget semantics:
 * - Engaged sessions count per browser
 * - Engaged% within browser = engaged_sessions / total_sessions
 * - Avg engaged time per browser = total_duration_engaged_seconds / engaged_sessions
 */
cube(`SessionsByBrowserDaily`, {
    sql: `
        SELECT
            customer_id,
            context_site_id,
            day,
            browser_family,
            total_sessions,
            engaged_sessions,
            total_duration_engaged_seconds,
            updated_at
        FROM clickhouse_test_db.sessions_by_browser_daily
    `,

    measures: {
        totalSessions: {
            sql: `total_sessions`,
            type: `sum`,
            title: `Total Sessions (All)`,
            description: `All sessions spawned by this browser family in the selected period.`,
        },

        engagedSessions: {
            sql: `engaged_sessions`,
            type: `sum`,
            title: `Engaged Sessions`,
            description: `Engaged sessions spawned by this browser family in the selected period.`,
        },

        totalDurationEngagedSeconds: {
            sql: `total_duration_engaged_seconds`,
            type: `sum`,
            title: `Total Duration (Engaged, Seconds)`,
        },

        engagedRateWithinBrowser: {
            sql: `sum(engaged_sessions) / nullIf(sum(total_sessions), 0)`,
            type: `number`,
            format: `percent`,
            title: `Engaged % (Within Browser)`,
            description: `Engaged sessions / total sessions within the browser bucket.`,
        },

        avgEngagedSessionTimeSeconds: {
            sql: `sum(total_duration_engaged_seconds) / nullIf(sum(engaged_sessions), 0)`,
            type: `number`,
            title: `Avg Engaged Session Time (Seconds)`
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

        browserFamily: {
            sql: `browser_family`,
            type: `string`,
            title: `Browser Family`,
            description: `Normalized bucket: Chrome / Safari / Firefox / Edge / Other.`,
        },

        updatedAt: {
            sql: `updated_at`,
            type: `time`,
            title: `Updated At`,
            description: `When this rollup row was last recomputed by the refreshable MV.`
        },
    },
});
