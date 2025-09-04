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
    siteId: { sql: 'context_site_id', type: `string` },
    sessionID: { sql: 'sessionid', type: `string` },
    userId: { sql: 'context_user_id', type: `string` },
    requestId: { sql: 'request_id', type: `string` },
    clusterId: { sql: 'cluster_id', type: `string` },
    customerId: { sql: 'customer_id', type: `string` },
    createdAt: { sql: 'utc_time', type: `time`, },

    //custom attributes
    custom_1: { sql: 'custom_1', type: 'string' },
    custom_2: { sql: 'custom_2', type: 'string' },
    custom_3: { sql: 'custom_3', type: 'string' },
    custom_4: { sql: 'custom_4', type: 'string' },
    custom_5: { sql: 'custom_5', type: 'string' },
    custom_6: { sql: 'custom_6', type: 'string' },
    custom_7: { sql: 'custom_7', type: 'string' },
    custom_8: { sql: 'custom_8', type: 'string' },
    custom_9: { sql: 'custom_9', type: 'string' },
    custom_10: { sql: 'custom_10', type: 'string' },
    custom_11: { sql: 'custom_11', type: 'string' },
    custom_12: { sql: 'custom_12', type: 'string' },
    custom_13: { sql: 'custom_13', type: 'string' },
    custom_14: { sql: 'custom_14', type: 'string' },
    custom_15: { sql: 'custom_15', type: 'string' },
    custom_16: { sql: 'custom_16', type: 'string' },
    custom_17: { sql: 'custom_17', type: 'string' },
    custom_18: { sql: 'custom_18', type: 'string' },
    custom_19: { sql: 'custom_19', type: 'string' },
    custom_20: { sql: 'custom_20', type: 'string' },
    custom_21: { sql: 'custom_21', type: 'string' },
    custom_22: { sql: 'custom_22', type: 'string' },
    custom_23: { sql: 'custom_23', type: 'string' },
    custom_24: { sql: 'custom_24', type: 'string' },
    custom_25: { sql: 'custom_25', type: 'string' },
    custom_26: { sql: 'custom_26', type: 'string' },
    custom_27: { sql: 'custom_27', type: 'string' },
    custom_28: { sql: 'custom_28', type: 'string' },
    custom_29: { sql: 'custom_29', type: 'string' },
    custom_30: { sql: 'custom_30', type: 'string' },
    custom_31: { sql: 'custom_31', type: 'string' },
    custom_32: { sql: 'custom_32', type: 'string' },
    custom_33: { sql: 'custom_33', type: 'string' },
    custom_34: { sql: 'custom_34', type: 'string' },
    custom_35: { sql: 'custom_35', type: 'string' },
    custom_36: { sql: 'custom_36', type: 'string' },
    custom_37: { sql: 'custom_37', type: 'string' },
    custom_38: { sql: 'custom_38', type: 'string' },
    custom_39: { sql: 'custom_39', type: 'string' },
    custom_40: { sql: 'custom_40', type: 'string' },
    custom_41: { sql: 'custom_41', type: 'string' },
    custom_42: { sql: 'custom_42', type: 'string' },
    custom_43: { sql: 'custom_43', type: 'string' },
    custom_44: { sql: 'custom_44', type: 'string' },
    custom_45: { sql: 'custom_45', type: 'string' },
    custom_46: { sql: 'custom_46', type: 'string' },
    custom_47: { sql: 'custom_47', type: 'string' },
    custom_48: { sql: 'custom_48', type: 'string' },
    custom_49: { sql: 'custom_49', type: 'string' },
    custom_50: { sql: 'custom_50', type: 'string' }
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
    },
    totalUsers: {
      sql: 'context_user_id',
      type: 'countDistinct',
      title: 'Total Users'
    }
  }
});
