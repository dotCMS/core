/**
 * =====================================================================================
 * 4) SessionsByLanguageDaily Cube
 * =====================================================================================
 *
 * Source table:
 *   clickhouse_test_db.sessions_by_language_daily
 *
 * Grain:
 *   One row per (customer_id, cluster_id, context_site_id, day, language_id).
 *
 * Widget semantics:
 * - Engaged sessions count per language
 * - Engaged% within language = engaged_sessions / total_sessions
 * - Avg engaged time per language = total_duration_engaged_seconds / engaged_sessions
 *
 * Note:
 * - language_id is stored as String.
 * - UI resolves the language name via REST Endpoint/API
 * */
 cube(`SessionsByLanguageDaily`, {
    sql: `
        SELECT
            customer_id,
            context_site_id,
            day,
            language_id,
            total_sessions,
            engaged_sessions,
            total_duration_engaged_seconds,
            updated_at
        FROM clickhouse_test_db.sessions_by_language_daily
    `,

    measures: {
        totalSessions: {
            sql: `total_sessions`,
            type: `sum`,
            title: `Total Sessions (All)`,
        },

        engagedSessions: {
            sql: `engaged_sessions`,
            type: `sum`,
            title: `Engaged Sessions`,
        },

        totalDurationEngagedSeconds: {
            sql: `total_duration_engaged_seconds`,
            type: `sum`,
            title: `Total Duration (Engaged, Seconds)`,
        },

        engagedRateWithinLanguage: {
            sql: `sum(engaged_sessions) / nullIf(sum(total_sessions), 0)`,
            type: `number`,
            format: `percent`,
            title: `Engaged % (Within Language)`,
        },

        avgEngagedSessionTimeSeconds: {
            sql: `sum(total_duration_engaged_seconds) / nullIf(sum(engaged_sessions), 0)`,
            type: `number`,
            title: `Avg Engaged Session Time (Seconds)`,
        }
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

        languageId: {
            sql: `language_id`,
            type: `string`,
            title: `Language Id`,
            description: `dotCMS language id (String). Display name resolved externally.`,
        },

        updatedAt: {
            sql: `updated_at`,
            type: `time`,
            title: `Updated At`,
            description: `When this rollup row was last recomputed by the refreshable MV.`
        },
    },
});
