/**
 * =====================================================================================
 * 2) SessionsByDeviceDaily Cube
 * =====================================================================================
 *
 * Source table:
 *   clickhouse_test_db.sessions_by_device_daily
 *
 * Grain:
 *   One row per (customer_id, cluster_id, context_site_id, day, device_category).
 *
 * Widget semantics (matches your screenshots):
 * - The big number (e.g., 32030) is ENGAGED sessions in that bucket.
 * - The percentage (e.g., 72%) is:
 *       engaged_sessions_in_bucket / total_sessions_in_bucket
 * - Average time is computed ONLY over engaged sessions:
 *       total_duration_engaged_seconds / engaged_sessions
 */
cube(`SessionsByDeviceDaily`, {
    sql: `
        SELECT
            customer_id,
            context_site_id,
            day,
            device_category,
            total_sessions,
            engaged_sessions,
            total_duration_engaged_seconds,
            updated_at
        FROM clickhouse_test_db.sessions_by_device_daily
    `,

    measures: {
        // Base measures (sums)
        totalSessions: {
            sql: `total_sessions`,
            type: `sum`,
            title: `Total Sessions (All)`,
            description: `All sessions spawned by this device category in the selected period.`
        },
        engagedSessions: {
            sql: `engaged_sessions`,
            type: `sum`,
            title: `Engaged Sessions`,
            description: `Engaged sessions spawned by this device category in the selected period.`
        },

        engagedRateWithinDevice: {
            // engaged / total within the device bucket (this is your 72%)
            sql: `sum(engaged_sessions) / nullIf(sum(total_sessions), 0)`,
            type: `number`,
            format: `percent`,
            title: `Engaged % (Within Device)`,
            description: `Engaged sessions / total sessions within the device category.`
        },

        avgEngagedSessionTimeSeconds: {
            // average duration over engaged sessions only
            sql: `sum(total_duration_engaged_seconds) / nullIf(sum(engaged_sessions), 0)`,
            type: `number`,
            title: `Avg Engaged Session Time (Seconds)`,
            description: `Average engaged session time within the device category.`
        },

        totalDurationEngagedSeconds: {
            sql: `total_duration_engaged_seconds`,
            type: `sum`,
            title: `Total Duration Engaged (Seconds)`,
            description: `Sum of engaged session durations for this device category.`
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

        deviceCategory: {
            sql: `device_category`,
            type: `string`,
            description: `Normalized bucket: Desktop / Mobile / Tablet / Other.`
        },
        updatedAt: {
            sql: `updated_at`,
            type: `time`,
            title: `Updated At`,
            description: `When this rollup row was last recomputed by the refreshable MV.`
        },
    },
});
