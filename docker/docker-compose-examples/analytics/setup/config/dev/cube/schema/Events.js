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
  preAggregations: {
    /*targetVisitedAfterAggregation: {
      measures: [Events.totalSessions, Events.targetVisitedAfterSuccesses],
      dimensions: [Events.experiment, Events.runningId, Events.variant],
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
      measures: [Events.totalSessions, Events.exitRateSuccesses],
      dimensions: [Events.experiment, Events.variant],
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

cube('request', {
  sql: `SELECT request_id,
               MAX(sessionid) as sessionid,
               (MAX(sessionnew) == 1)::bool as isSessionNew,
               MIN(utc_time) as createdAt,
               MAX(source_ip) as source_ip,
               MAX(language) as language,
               MAX(languageid) as languageId,
               MAX(object_live) as live,
               MAX(object_working) as working,
               MAX(useragent) as user_agent,
               MAX(persona) as persona,
               MAX(referer) as referer,
               MAX(conhost) as conHost,
               MAX(conhostname) as conHostName,
              CASE
                WHEN MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_identifier END) IS NOT NULL 
                    THEN MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_identifier END)
                WHEN MAX(CASE WHEN event_type = 'URL_MAP' THEN object_identifier END) IS NOT NULL 
                    THEN MAX(CASE WHEN event_type = 'URL_MAP' THEN object_identifier END)
                WHEN COUNT(DISTINCT object_identifier) == 1 THEN MAX(object_identifier)
                ELSE MAX(object_identifier)
               END AS  identifier,
               MAX(object_title) as title,
               CASE
                  WHEN MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_basetype END) IS NOT NULL 
                      THEN MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_basetype END)
                  WHEN MAX(CASE WHEN event_type = 'URL_MAP' THEN object_basetype END) IS NOT NULL 
                      THEN MAX(CASE WHEN event_type = 'URL_MAP' THEN object_basetype END)
                  WHEN COUNT(DISTINCT object_basetype) == 1 THEN MAX(object_basetype) 
                  ELSE MAX(object_basetype)
                END AS  baseType,
               MAX(object_contenttype) as contentType,
               MAX(object_contenttypename) as contentTypeName,
               MAX(object_contenttypeid) as contentTypeId,
               MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_forwardto ELSE NULL END) as vanity_forward_to,
               MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_action ELSE NULL END) as vanity_action,
               CASE 
                    WHEN SUM(CASE WHEN event_type = 'URL_MAP' THEN 1 ELSE 0 END) > 0 THEN 'URL_MAP'
                    WHEN SUM(CASE WHEN event_type = 'VANITY_REQUEST' THEN 1 ELSE 0 END) > 0 THEN 'VANITY_URL'
                    ELSE 'NORMAL'
               END mappingType,
               MAX(url) AS url,
               MAX(cluster_id) AS cluster_id,
               MAX(customer_id) AS customer_id
        FROM events
        GROUP BY request_id`,
  dimensions: {
    mappingType: { sql: 'mappingType', type: `string` },
    conHost: { sql: 'conHost', type: `string` },
    conHostName: { sql: 'conHostName', type: `string` },
    contentTypeName: { sql: 'contentTypeName', type: `string` },
    contentTypeId: { sql: 'contentTypeId', type: `string` },
    contentTypeVariable: { sql: 'contentType', type: `string` },
    live: { sql: 'live', type: `boolean` },
    working: { sql: 'working', type: `boolean` },
    baseType: { sql: 'baseType', type: `string` },
    identifier: { sql: 'identifier', type: `string` },
    title: { sql: 'title', type: `string` },
    requestId: { sql: 'request_id', type: `string` },
    clusterId: { sql: 'cluster_id', type: `string` },
    customerId: { sql: 'customer_id', type: `string` },
    sessionId: { sql: 'sessionid', type: `string` },
    isSessionNew: { sql: 'isSessionNew', type: `boolean` },
    createdAt: { sql: 'createdAt', type: `time`, },
    sourceIp: { sql: 'source_ip', type: `string` },
    language: { sql: 'language', type: `string` },
    languageId: { sql: 'languageid', type: `string` },
    userAgent: { sql: 'user_agent', type: `string` },
    referer: { sql: 'referer', type: `string` },
    persona: { sql: 'persona', type: `string` },
    url: { sql: 'url', type: `string` },
    forwardTo: { sql: 'vanity_forward_to', type: `string` },
    action: { sql: 'vanity_action', type: `string` }
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
      sql: `CASE WHEN ${CUBE}.baseType = 'FILEASSET' THEN 1 ELSE NULL END`,
      type: 'count',
      title: 'Count of FileAsset Request'
    },
    fileRequestAverage: {
      sql: `${fileRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'FileRequest Average'
    },
    pageRequest: {
      sql: `CASE WHEN ${CUBE}.baseType = 'HTMLPAGE' THEN 1 ELSE NULL END`,
      type: 'count',
      title: 'Count of Page request'
    },
    pageRequestAverage: {
      sql: `${pageRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'Page Request Average'
    },
    otherRequestAverage: {
      sql: `(${totalRequest} - (${fileRequest} + ${pageRequest})) / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'No FIle OR Page Average'
    }

  }
});

cube('events', {
  sql: `select * from events`,
  dimensions: {
    conHost: { sql: 'conHost', type: `string` },
    conHostName: { sql: 'conHostName', type: `string` },
    contentTypeName: { sql: 'contentTypeName', type: `string` },
    contentTypeId: { sql: 'contentTypeId', type: `string` },
    contentTypeVariable: { sql: 'contentType', type: `string` },
    live: { sql: 'live', type: `boolean` },
    working: { sql: 'working', type: `boolean` },
    baseType: { sql: 'baseType', type: `string` },
    identifier: { sql: 'identifier', type: `string` },
    title: { sql: 'title', type: `string` },
    requestId: { sql: 'request_id', type: `string` },
    clusterId: { sql: 'cluster_id', type: `string` },
    customerId: { sql: 'customer_id', type: `string` },
    sessionId: { sql: 'sessionid', type: `string` },
    isSessionNew: { sql: 'isSessionNew', type: `boolean` },
    createdAt: { sql: 'createdAt', type: `time`, },
    sourceIp: { sql: 'source_ip', type: `string` },
    language: { sql: 'language', type: `string` },
    languageId: { sql: 'languageid', type: `string` },
    userAgent: { sql: 'user_agent', type: `string` },
    referer: { sql: 'referer', type: `string` },
    persona: { sql: 'persona', type: `string` },
    url: { sql: 'url', type: `string` },
    forwardTo: { sql: 'vanity_forward_to', type: `string` },
    action: { sql: 'vanity_action', type: `string` },
    eventsType: { sql: 'events_type', type: `string` }
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
      sql: `CASE WHEN ${CUBE}.baseType = 'FILEASSET' THEN 1 ELSE NULL END`,
      type: 'count',
      title: 'Count of FileAsset Request'
    },
    fileRequestAverage: {
      sql: `${fileRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'FileRequest Average'
    },
    pageRequest: {
      sql: `CASE WHEN ${CUBE}.baseType = 'HTMLPAGE' THEN 1 ELSE NULL END`,
      type: 'count',
      title: 'Count of Page request'
    },
    pageRequestAverage: {
      sql: `${pageRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'Page Request Average'
    },
    otherRequestAverage: {
      sql: `(${totalRequest} - (${fileRequest} + ${pageRequest})) / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'No FIle OR Page Average'
    }

  }
});