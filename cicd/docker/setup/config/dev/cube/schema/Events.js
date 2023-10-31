cube(`Events`, {
  sql: `SELECT experiment,
               lookbackwindow,
               variant,
               runningid,
               toDateTime(toStartOfDay(toTimeZone(toDateTime(utc_time), 'UTC'), 'UTC'), 'UTC') as day,
               min(CASE WHEN (isexperimentpage = true AND event_type = 'pageview') THEN utc_time END) as firstExperimentPageVisit,
               max(CASE WHEN (istargetpage = true AND event_type = 'pageview') THEN utc_time END) as lastTargetPageVisit,
               firstExperimentPageVisit is not null as isSession,
               COUNT(CASE WHEN event_type = 'pageview' THEN 1 ELSE 0 END) AS pageviews,
               arrayElement(arraySlice(groupArray(isexperimentpage), -1), 1) = true AS experimentPageLastVisited
        FROM clickhouse_test_db.events
        WHERE event_type = 'pageview'
        GROUP BY experiment, runningid, lookbackwindow, variant, day
        having isSession = 1
        order by day`,
  preAggregations: {
    // Pre-Aggregations definitions go here
    // Learn more here: https://cube.dev/docs/caching/pre-aggregations/getting-started
    /*targetVisitedAfterAggregation: {
      measures: [
        Events.totalSessions,
        Events.targetVisitedAfterSuccesses
      ],
      dimensions: [
        Events.experiment,
        Events.runningId,
        Events.variant
      ],
      timeDimension: Events.day,
      granularity: `day`,
      indexes: {
        totalSessions_targetVisitedAfter_index: {
          columns: [experiment, variant]
        }
      }
    },
    bounceRateAggregation: {
      measures: [Events.totalSessions, Events.bounceRateSuccesses],
      dimensions: [Events.experiment, Events.variant],
      timeDimension: Events.day,
      granularity: `day`,
      indexes: {
        totalSessions_bounceRateSuccesses_index: {
          columns: [totalSessions, bounceRateSuccesses]
        }
      }
    },
    exitRateAggregation: {
      measures: [
        Events.totalSessions,
        Events.bounceRateSuccesses,
        Events.exitRateSuccesses
      ],
      dimensions: [
        Events.experiment,
        Events.variant
      ],
      timeDimension: Events.day,
      granularity: `day`,
      indexes: {
        totalSessions_exitRateSuccesses_index: {
          columns: [totalSessions, exitRateSuccesses]
        }
      }
    }*/
  },
  joins: {},
  measures: {
    count: {
      type: `count`
    },
    totalSessions: {
      type: `count_distinct`,
      sql: `lookbackwindow`
    },
    targetVisitedAfterSuccesses: {
      type: `count_distinct`,
      sql: `lookbackwindow`,
      filters: [{
        sql: `firstExperimentPageVisit < lastTargetPageVisit`
      }]
    },
    bounceRateSuccesses: {
      type: `count`,
      sql: `lookbackwindow`,
      filters: [{
        sql: `pageviews = 1`
      }]
    },
    exitRateSuccesses: {
      type: `count`,
      sql: `lookbackwindow`,
      filters: [{
        sql: `experimentPageLastVisited = 1`
      }]
    },
    targetVisitedAfterConvertionRate: {
      description: 'Convertion Rate',
      format: `percent`,
      type: 'number',
      sql: `${targetVisitedAfterSuccesses} * 100 / ${totalSessions}`
    },
    bounceRateConvertionRate: {
      description: 'Convertion Rate',
      format: `percent`,
      type: 'number',
      sql: `${bounceRateSuccesses} * 100 / ${totalSessions}`
    },
    exitRateConvertionRate: {
      description: 'Convertion Rate',
      format: `percent`,
      type: 'number',
      sql: `${exitRateSuccesses} * 100 / ${totalSessions}`
    },
  },
  dimensions: {
    experiment: {
      sql: `experiment`,
      type: `string`
    },
    runningId: {
      sql: `runningid`,
      type: `string`
    },
    lookBackWindow: {
      sql: `lookbackwindow`,
      type: `string`
    },
    variant: {
      sql: `variant`,
      type: `string`
    },
    isExperimentSession: {
      type: 'boolean',
      sql: 'isSession'
    },
    pageViewsTotal: {
      type: 'number',
      sql: 'pageviews'
    },
    lastUrlVisited: {
      type: 'string',
      sql: 'lastUrlVisited'
    },
    firstExperimentPageVisit: {
      sql: `firstExperimentPageVisit`,
      type: `time`
    },
    lastTargetPageVisit: {
      sql: `lastTargetPageVisit`,
      type: `time`
    },
    day: {
      sql: `day`,
      type: `time`
    }
  },
  dataSource: `default`
});