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
    WHERE event_type = 'pageview'
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
    httpResponseCode: { sql: 'http_response_code', type: `string` }
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
    id: {
      sql: `CONCAT(${CUBE}.request_id, '-', ${CUBE}.event_type)`,
      type: `string`,
      primaryKey: true
    },
    conHost: { sql: 'conhost', type: `string` },
    conHostName: { sql: 'conhostname', type: `string` },
    contentTypeName: { sql: 'object_contenttypename', type: `string` },
    contentTypeId: { sql: 'object_contenttypeid', type: `string` },
    contentTypeVariable: { sql: 'object_contenttype', type: `string` },
    live: { sql: 'object_live', type: `boolean` },
    working: { sql: 'object_working', type: `boolean` },
    baseType: { sql: 'object_basetype', type: `string` },
    identifier: { sql: 'object_identifier', type: `string` },
    title: { sql: 'object_title', type: `string` },
    requestId: { sql: 'request_id', type: `string` },
    clusterId: { sql: 'cluster_id', type: `string` },
    customerId: { sql: 'customer_id', type: `string` },
    sessionId: { sql: 'sessionid', type: `string` },
    isSessionNew: { sql: 'sessionnew', type: `number` },
    createdAt: { sql: 'utc_time', type: `time`, },
    sourceIp: { sql: 'source_ip', type: `string` },
    language: { sql: 'language', type: `string` },
    languageId: { sql: 'languageid', type: `string` },
    userAgent: { sql: 'useragent', type: `string` },
    referer: { sql: 'referer', type: `string` },
    persona: { sql: 'persona', type: `string` },
    url: { sql: 'url', type: `string` },
    forwardTo: { sql: 'object_forwardto', type: `string` },
    action: { sql: 'object_action', type: `string` },
    eventType: { sql: 'event_type', type: `string` },
    eventSource: { sql: 'event_source', type: `string` },
    httpResponseCode: {
      sql: `${HttpResponses.httpResponseCode}`,
      type: `string`
    }
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
    fileRequest: {
      sql: `CASE WHEN ${CUBE}.object_basetype = 'FILEASSET' THEN 1 ELSE NULL END`,
      type: 'count',
      title: 'Count of FileAsset Request'
    },
    fileRequestAverage: {
      sql: `${fileRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'FileRequest Average'
    },
    pageRequest: {
      sql: `CASE WHEN ${CUBE}.object_basetype = 'HTMLPAGE' THEN 1 ELSE NULL END`,
      type: 'count',
      title: 'Count of Page request'
    },
    pageRequestAverage: {
      sql: `${request.pageRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'Page Request Average'
    },
    otherRequestAverage: {
      sql: `(${totalRequest} - (${fileRequest} + ${pageRequest})) / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'No FIle OR Page Average'
    }
  },
  joins: {
    HttpResponses: {
      sql: `${CUBE}.request_id = ${HttpResponses.requestId}`,
      relationship: `one_to_one`
    }
  }
});
