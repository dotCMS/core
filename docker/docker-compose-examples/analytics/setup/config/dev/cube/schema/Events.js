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
               MAX(useragent) as user_agent,
               MAX(persona) as persona,
               MAX(rendermode) as rendermode,
               MAX(referer) as referer,
               MAX(host) as host,
               MAX(CASE WHEN event_type = 'PAGE_REQUEST' THEN object_id ELSE NULL END) as page_id,
               MAX(CASE WHEN event_type = 'PAGE_REQUEST' THEN object_title ELSE NULL END) as page_title,
               MAX(CASE WHEN event_type = 'FILE_REQUEST' THEN object_id ELSE NULL END) as file_id,
               MAX(CASE WHEN event_type = 'FILE_REQUEST' THEN object_title ELSE NULL END) as file_title,
               MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_id ELSE NULL END) as vanity_id,
               MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_forward_to ELSE NULL END) as vanity_forward_to,
               MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN object_response ELSE NULL END) as vanity_response,
               (SUM(CASE WHEN event_type = 'VANITY_REQUEST' THEN 1 ELSE 0 END) > 0)::bool as was_vanity_url_hit,
               MAX(CASE WHEN event_type = 'VANITY_REQUEST' THEN comefromvanityurl ELSE NULL END) as come_from_vanity_url,
               (SUM(CASE WHEN event_type = 'URL_MAP' THEN 1 ELSE 0 END) > 0)::bool as url_map_match,
               MAX(CASE WHEN event_type = 'URL_MAP' THEN object_id ELSE NULL END) as url_map_content_detail_id,
               MAX(CASE WHEN event_type = 'URL_MAP' THEN object_title ELSE NULL END) as url_map_content_detail_title,
               MAX(CASE WHEN event_type = 'URL_MAP' THEN object_content_type_id ELSE NULL END) as url_map_content_type_id,
               MAX(CASE WHEN event_type = 'URL_MAP' THEN object_content_type_name ELSE NULL END) as url_map_content_type_name,
               MAX(CASE WHEN event_type = 'URL_MAP' THEN object_content_type_var_name ELSE NULL END) as url_map_content_type_var_name,
               MAX(object_detail_page_url) as url_map_detail_page_url,
               MAX(url) AS url,
               CASE
                 WHEN MAX(CASE WHEN event_type = 'FILE_REQUEST' THEN 1 ELSE 0 END) = 1 THEN 'FILE'
                 WHEN MAX(CASE WHEN event_type = 'PAGE_REQUEST' THEN 1 ELSE 0 END) = 1 THEN 'PAGE'
                 ELSE 'NOTHING'
               END  AS what_am_i
        FROM events
        GROUP BY request_id`,
  dimensions: {
    requestId: { sql: 'request_id', type: `string` },
    sessionId: { sql: 'sessionid', type: `string` },
    isSessionNew: { sql: 'isSessionNew', type: `boolean` },
    createdAt: { sql: 'createdAt', type: `time`, },
    whatAmI: { sql: 'what_am_i', type: `string` },
    sourceIp: { sql: 'source_ip', type: `string` },
    language: { sql: 'language', type: `string` },
    userAgent: { sql: 'user_agent', type: `string` },
    referer: { sql: 'referer', type: `string` },
    renderMode: { sql: 'rendermode', type: `string` },
    persona: { sql: 'persona', type: `string` },
    host: { sql: 'host', type: `string` },
    url: { sql: 'url', type: `string` },
    pageId: { sql: 'page_id', type: `string` },
    pageTitle: { sql: 'page_title', type: `string` },
    fileId: { sql: 'file_id', type: `string` },
    fileTitle: { sql: 'file_title', type: `string` },
    wasVanityHit: { sql: 'was_vanity_url_hit', type: `boolean` },
    vanityId: { sql: 'vanity_id', type: `string` },
    vanityForwardTo: { sql: 'vanity_forward_to', type: `string` },
    vanityResponse: { sql: 'vanity_response', type: `string` },
    comeFromVanityURL: { sql: 'come_from_vanity_url', type: `boolean` },
    urlMapWasHit: { sql: 'url_map_match', type: `boolean` },
    isDetailPage: { sql: "url_map_detail_page_url is not null and url_map_detail_page_url != ''", type: `boolean` },
    urlMapContentDetailId: { sql: 'url_map_content_detail_id', type: `string` },
    urlMapContentDetailTitle: { sql: 'url_map_content_detail_title', type: `string` },
    urlMapContentId: { sql: 'url_map_content_type_id', type: `string` },
    urlMapContentTypeName: { sql: 'url_map_content_type_name', type: `string` },
    urlMapContentTypeVarName: { sql: 'url_map_content_type_var_name', type: `string` }
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
    pageRequest: {
      sql: 'page_id',
      type: 'count',
      title: 'Count of Page request'
    },
    uniquePageRequest: {
      sql: 'page_id',
      type: 'countDistinct',
      title: 'Unique Page Ids by Session'
    },
    pageRequestAverage: {
      sql: `${fileRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'FileRequest Average'
    },
    fileRequest: {
      sql: 'file_id',
      type: 'count',
      title: 'Count of File Request'
    },
    uniqueFileRequest: {
      sql: 'file_id',
      type: 'countDistinct',
      title: 'Unique Count of File Request'
    },
    fileRequestAverage: {
      sql: `${fileRequest} / NULLIF(${totalRequest}, 0)`,
      type: 'number',
      title: 'FileRequest Average'
    }
  }
});

