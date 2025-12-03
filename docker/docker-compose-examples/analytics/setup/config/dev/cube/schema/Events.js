/**
 * This model is used to calculate session-level conversions. In other words, it handles metrics
 * that require information from the entire session, such as bounce rate or exit rate.
 * To compute these, all pageviews generated within a single session must be analyzed.
 *
 * The current name of this model is misleading — it should ideally be renamed to Session.
 *
 * At the moment, the model is only used for A/B Testing Analytics.
 * However, in the future, it should also support general Analytics. To achieve this,
 * we will first need to decouple it from experiment-specific data, such as the isExperimentPage attribute.
 *
 * SECURITY: This cube implements defense-in-depth security filtering:
 * - Layer 1 (Base SQL): FILTER_PARAMS provide customer_id/cluster_id filtering at SQL level
 * - Layer 2 (Runtime): cube.js queryRewrite adds additional runtime security filters
 *
 * PERFORMANCE: Optimized for ClickHouse with:
 * - GROUP BY order matches partition structure: customer_id, cluster_id, day, ...
 * - Base SQL filtering enables partition elimination
 * - Explicit column selection for better performance
 */
cube(`Events`, {
  sql: `
    WITH CountsAndLastURL AS (SELECT lookbackwindow, MAX(utc_time) AS maxDate
                              FROM events
      WHERE ${FILTER_PARAMS.Events.customerId.filter('customer_id')}
        AND (
          ${FILTER_PARAMS.Events.clusterId ? FILTER_PARAMS.Events.clusterId.filter('cluster_id') : '1=1'}
          OR (${FILTER_PARAMS.Events.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})
        )
                              GROUP BY lookbackwindow
    )
    SELECT
      E.experiment,
      E.lookbackwindow,
      E.variant,
      E.runningid,
      E.customer_id,
      E.cluster_id,
      toDateTime(toStartOfDay(toTimeZone(toDateTime(utc_time), 'UTC'), 'UTC'), 'UTC') as day,
    min(CASE WHEN (isexperimentpage = true AND event_type = 'pageview') THEN utc_time END) as firstExperimentPageVisit,
    max(CASE WHEN (istargetpage = true AND event_type = 'pageview') THEN utc_time END) as lastTargetPageVisit,
    firstExperimentPageVisit is not null as isSession,
    COUNT(CASE WHEN event_type = 'pageview' THEN 1 ELSE 0 END) AS pageviews,
    SUM(CASE WHEN (E.isexperimentpage = true AND C.maxDate = E.utc_time AND event_type = 'pageview') THEN 1 ELSE 0 END) as experimentPageLastVisited
    FROM events E JOIN CountsAndLastURL C ON C.lookbackwindow = E.lookbackwindow
    WHERE ${FILTER_PARAMS.Events.customerId.filter('E.customer_id')}
        AND (
          ${FILTER_PARAMS.Events.clusterId ? FILTER_PARAMS.Events.clusterId.filter('E.cluster_id') : '1=1'}
          OR (${FILTER_PARAMS.Events.clusterId ? 'FALSE' : '(E.cluster_id IS NULL OR E.cluster_id = \'\')'})
        )
        AND ${FILTER_PARAMS.Events.experiment.filter('E.experiment')}
        AND E.event_type = 'pageview'
    GROUP BY E.customer_id, E.cluster_id, day, E.experiment, E.runningid, E.lookbackwindow, E.variant
    having isSession = 1
    order by day
  `,
  joins: {},
  measures: {
    count: {
      type: `count`,
      title: 'Total Events',
      description: 'Total number of experiment events'
    },
    totalSessions: {
      type: `count_distinct`,
      sql: `lookbackwindow`,
      title: 'Total Sessions',
      description: 'Unique count of user sessions in experiments'
    },
    targetVisitedAfterSuccesses: {
      type: `count_distinct`,
      sql: `lookbackwindow`,
      title: 'Target Page Conversions',
      description: 'Sessions that visited target page after experiment page',
      filters: [{
        sql: `firstExperimentPageVisit < lastTargetPageVisit`
      }]
    },
    bounceRateSuccesses: {
      type: `count`,
      sql: `lookbackwindow`,
      title: 'Non-Bounce Sessions',
      description: 'Sessions with more than one page view (inverse of bounce)',
      filters: [{
        sql: `pageviews > 1`
      }]
    },
    exitRateSuccesses: {
      type: `count`,
      sql: `lookbackwindow`,
      title: 'Non-Exit Sessions',
      description: 'Sessions that did not exit from experiment page',
      filters: [{
        sql: `experimentPageLastVisited == 0`
      }]
    },
    targetVisitedAfterConversionRate: {
      title: 'Target Page Conversion Rate',
      description: 'Percentage of sessions that visited target page after experiment page',
      format: `percent`,
      type: 'number',
      sql: `ROUND(${targetVisitedAfterSuccesses} * 100 / ${totalSessions}, 2)`
    },
    bounceRateConversionRate: {
      title: 'Non-Bounce Rate',
      description: 'Percentage of sessions with more than one page view (1 - bounce rate)',
      format: `percent`,
      type: 'number',
      sql: `ROUND(${bounceRateSuccesses} * 100 / ${totalSessions}, 2)`
    },
    exitRateConversionRate: {
      title: 'Non-Exit Rate',
      description: 'Percentage of sessions that did not exit from experiment page (1 - exit rate)',
      format: `percent`,
      type: 'number',
      sql: `ROUND(${exitRateSuccesses} * 100 / ${totalSessions}, 2)`
    }
  },
  dimensions: {
    experiment: {
      sql: `experiment`,
      type: `string`,
      title: 'Experiment ID',
      description: 'Unique identifier for the A/B test experiment'
    },
    runningId: {
      sql: `runningid`,
      type: `string`,
      title: 'Running ID',
      description: 'Current running instance identifier for the experiment'
    },
    lookBackWindow: {
      sql: `lookbackwindow`,
      type: `string`,
      title: 'Look-back Window',
      description: 'Session identifier used for tracking user behavior over time'
    },
    variant: {
      sql: `variant`,
      type: `string`,
      title: 'Experiment Variant',
      description: 'A/B test variant (e.g., A, B, control, treatment)'
    },
    isExperimentSession: {
      type: 'boolean',
      sql: 'isSession',
      title: 'Is Experiment Session',
      description: 'Whether this session participated in the experiment'
    },
    pageViewsTotal: {
      type: 'number',
      sql: 'pageviews',
      title: 'Total Page Views',
      description: 'Total number of page views in the session'
    },
    lastUrlVisited: {
      type: 'string',
      sql: 'lastUrlVisited',
      title: 'Last URL Visited',
      description: 'The last URL visited in the session'
    },
    firstExperimentPageVisit: {
      sql: `firstExperimentPageVisit`,
      type: `time`,
      title: 'First Experiment Page Visit',
      description: 'Timestamp of first visit to experiment page'
    },
    lastTargetPageVisit: {
      sql: `lastTargetPageVisit`,
      type: `time`,
      title: 'Last Target Page Visit',
      description: 'Timestamp of last visit to target/conversion page'
    },
    day: {
      sql: `day`,
      type: `time`,
      title: 'Date',
      description: 'Date dimension for time-based analysis (UTC)'
    },
    // Security dimensions for multi-tenant filtering
    customerId: {
      sql: `customer_id`,
      type: `string`,
      title: 'Customer ID',
      description: 'Multi-tenant customer identifier for security filtering'
    },
    clusterId: {
      sql: `cluster_id`,
      type: `string`,
      title: 'Cluster ID',
      description: 'Multi-tenant cluster identifier for security filtering'
    }
  },
  dataSource: `default`
});