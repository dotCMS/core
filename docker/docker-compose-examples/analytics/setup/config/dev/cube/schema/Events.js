cube(`Events`, {
  sql: `
    WITH CountsAndLastURL AS (SELECT lookbackwindow, MAX(utc_time) AS maxDate
                              FROM events
                              GROUP BY lookbackwindow
    )
    SELECT
      E.experiment,
      E.lookbackwindow,
      E.variant,
      E.runningid,
      toDateTime(toStartOfDay(toTimeZone(toDateTime(utc_time), 'UTC'), 'UTC'), 'UTC') as day,
    min(CASE WHEN (isexperimentpage = true AND event_type = 'pageview') THEN utc_time END) as firstExperimentPageVisit,
    max(CASE WHEN (istargetpage = true AND event_type = 'pageview') THEN utc_time END) as lastTargetPageVisit,
    firstExperimentPageVisit is not null as isSession,
    COUNT(CASE WHEN event_type = 'pageview' THEN 1 ELSE 0 END) AS pageviews,
    SUM(CASE WHEN (E.isexperimentpage = true AND C.maxDate = E.utc_time AND event_type = 'pageview') THEN 1 ELSE 0 END) as experimentPageLastVisited
    FROM events E JOIN CountsAndLastURL C ON C.lookbackwindow = E.lookbackwindow
    WHERE ${FILTER_PARAMS.request.customerId.filter('customer_id')}
        AND ${FILTER_PARAMS.request.cluster_id ? FILTER_PARAMS.request.cluster_id.filter('cluster_id') : '(cluster_id IS NULL OR cluster_id = \'\')'}
        AND ${FILTER_PARAMS.request.experiment.filter('experiment')}
        AND event_type = 'pageview'
    GROUP BY experiment, runningid, lookbackwindow, variant, day
    having isSession = 1
    order by day
  `,
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
        sql: `pageviews > 1`
      }]
    },
    exitRateSuccesses: {
      type: `count`,
      sql: `lookbackwindow`,
      filters: [{
        sql: `experimentPageLastVisited == 0`
      }]
    },
    targetVisitedAfterConvertionRate: {
      description: 'Convertion Rate',
      format: `percent`,
      type: 'number',
      sql: `ROUND(${targetVisitedAfterSuccesses} * 100 / ${totalSessions}, 2)`
    },
    bounceRateConvertionRate: {
      description: 'Convertion Rate',
      format: `percent`,
      type: 'number',
      sql: `ROUND(${bounceRateSuccesses} * 100 / ${totalSessions}, 2)`
    },
    exitRateConvertionRate: {
      description: 'Convertion Rate',
      format: `percent`,
      type: 'number',
      sql: `ROUND(${exitRateSuccesses} * 100 / ${totalSessions}, 2)`
    }
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

cube('HttpResponses', {
  sql: `SELECT request_id, http_response_code
        FROM events 
        WHERE event_type = 'HTTP_RESPONSE'`,
  dimensions: {
    requestId: { sql: 'request_id', type: `string` },
    httpResponseCode: { sql: 'http_response_code', type: `number` }
  }
});

cube('request', {
  sql: `SELECT *
        FROM events 
        WHERE ${FILTER_PARAMS.request.customerId.filter('customer_id')}
        AND ${FILTER_PARAMS.request.cluster_id ? FILTER_PARAMS.request.cluster_id.filter('cluster_id') : '(cluster_id IS NULL OR cluster_id = \'\')'}`,
  /*preAggregations: {
    totalRequestStats: {
      type: 'rollup',
      measures: [
        request.totalRequest
      ],
      dimensions: [
        request.customerId,
        request.clusterId,
        request.identifier,
        request.title,
        request.baseType,
        request.url
      ],
      granularity: 'day',  // Aggregates data daily
      timeDimension: request.createdAt,
      partitionGranularity: 'month', // Partitions by month for better performance
      refreshKey: {
        every: '1 hour', // Refresh pre-aggregation every hour
        incremental: true // Only update new data
      },
      indexes: {
        dailyIndex: {
          columns: ['request__customer_id', 'request__cluster_id']
        }
      }
    },

    countStats: {
      type: 'rollup',
      measures: [
        request.count
      ],
      dimensions: [
        request.customerId,
        request.clusterId,
        request.identifier,
        request.title,
        request.baseType,
        request.url
      ],
      granularity: 'day',  // Aggregates data daily
      timeDimension: request.createdAt,
      partitionGranularity: 'month', // Partitions by month for better performance
      refreshKey: {
        every: '1 hour', // Refresh pre-aggregation every hour
        incremental: true // Only update new data
      },
      indexes: {
        dailyIndex: {
          columns: ['request__customer_id', 'request__cluster_id']
        }
      }
    }
  },*/
  dimensions: {
    eventType: { sql: 'event_type', type: `string` },
    userAgent: { sql: 'user_agent', type: `string` },
    referer: { sql: 'referer', type: `string` },
    url: { sql: 'url', type: `string` },
    encoding: { sql: 'doc_encoding', type: `string` },
    pageTitle: { sql: 'page_title', type: `string` },
    languageId: { sql: 'language_id', type: `string` },
    persona: { sql: 'persona', type: `string` },
    path: { sql: 'doc_path', type: `string` },
    domain: { sql: 'doc_host', type: `string` },
    protocol: { sql: 'doc_protocol', type: `string` },
    urlHash: { sql: 'doc_hash', type: `string` },
    queryParameters: { sql: 'doc_search', type: `string` },
    screenResolution: { sql: 'screen_resolution', type: `string` },
    browserLanguage: { sql: 'user_language', type: `string` },
    viewportHeight: { sql: 'viewport_height', type: `string` },
    viewPortWidth: { sql: 'viewport_width', type: `string` },
    utmCampaign: { sql: 'utm_campaign', type: `string` },
    utmMedium: { sql: 'utm_medium', type: `string` },
    utmSource: { sql: 'utm_source', type: `string` },
    utmTerm: { sql: 'utm_term', type: `string` },
    utmContent: { sql: 'utm_content', type: `string` },
    key: { sql: 'context_site_key', type: `string` },
    sessionID: { sql: 'sessionid', type: `string` },
    userId: { sql: 'context_user_id', type: `string` },
    requestId: { sql: 'request_id', type: `string` },
    clusterId: { sql: 'cluster_id', type: `string` },
    customerId: { sql: 'customer_id', type: `string` },
    createdAt: { sql: 'utc_time', type: `time`, },
  },
  measures: {
    count: {
      type: "count"
    },
    totalSessions: {
      sql: 'sessionid',
      type: 'countDistinct',
      title: 'Total Sessions'
    },
    totalRequest: {
      sql: 'request_id',
      type: 'countDistinct',
      title: 'Total Requests'
    }
  }
});
