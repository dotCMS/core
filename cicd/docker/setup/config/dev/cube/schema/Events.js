cube(`Events`, {
  sql: `SELECT * FROM clickhouse_test_db.events`,

  preAggregations: {
    // Pre-Aggregations definitions go here
    // Learn more here: https://cube.dev/docs/caching/pre-aggregations/getting-started  
  },

  joins: {

  },

  measures: {
    count: {
      type: `count`,
      drillMembers: [clusterId, customerId, eventnCtxEventId, idsAjsAnonymousId, pageTitle, userAnonymousId, userHashedAnonymousId]
    }
  },

  dimensions: {
    apiKey: {
      sql: `api_key`,
      type: `string`
    },

    clusterId: {
      sql: `cluster_id`,
      type: `string`
    },

    customerId: {
      sql: `customer_id`,
      type: `string`
    },

    eventnCtxEventId: {
      sql: `eventn_ctx_event_id`,
      type: `string`
    },

    sourceIp: {
      sql: `source_ip`,
      type: `string`
    },

    ip: {
      sql: `ip`,
      type: `string`
    },

    experiment: {
      sql: `experiment`,
      type: `string`
    },

    runningId: {
      sql: `runningId`,
      type: `string`
    },

    variant: {
      sql: `variant`,
      type: `string`
    },

    lookBackWindow: {
      sql: `lookbackwindow`,
      type: `string`
    },

    src: {
      sql: `src`,
      type: `string`
    },

    testStrField: {
      sql: `test_str_field`,
      type: `string`
    },

    docEncoding: {
      sql: `doc_encoding`,
      type: `string`
    },

    docHost: {
      sql: `doc_host`,
      type: `string`
    },

    docPath: {
      sql: `doc_path`,
      type: `string`
    },

    docSearch: {
      sql: `doc_search`,
      type: `string`
    },

    eventType: {
      sql: `event_type`,
      type: `string`
    },

    idsAjsAnonymousId: {
      sql: `ids_ajs_anonymous_id`,
      type: `string`
    },

    pageTitle: {
      sql: `page_title`,
      type: `string`
    },

    parsedUaDeviceBrand: {
      sql: `parsed_ua_device_brand`,
      type: `string`
    },

    parsedUaDeviceFamily: {
      sql: `parsed_ua_device_family`,
      type: `string`
    },

    parsedUaDeviceModel: {
      sql: `parsed_ua_device_model`,
      type: `string`
    },

    parsedUaOsFamily: {
      sql: `parsed_ua_os_family`,
      type: `string`
    },

    parsedUaOsVersion: {
      sql: `parsed_ua_os_version`,
      type: `string`
    },

    parsedUaUaFamily: {
      sql: `parsed_ua_ua_family`,
      type: `string`
    },

    parsedUaUaVersion: {
      sql: `parsed_ua_ua_version`,
      type: `string`
    },

    referer: {
      sql: `referer`,
      type: `string`
    },

    screenResolution: {
      sql: `screen_resolution`,
      type: `string`
    },

    url: {
      sql: `url`,
      type: `string`
    },

    userAgent: {
      sql: `user_agent`,
      type: `string`
    },

    userAnonymousId: {
      sql: `user_anonymous_id`,
      type: `string`
    },

    userHashedAnonymousId: {
      sql: `user_hashed_anonymous_id`,
      type: `string`
    },

    userLanguage: {
      sql: `userlanguage`,
      type: `string`
    },

    persona: {
      sql: `persona`,
      type: `string`
    },

    vpSize: {
      sql: `vp_size`,
      type: `string`
    },

    utcTime: {
      sql: `utc_time`,
      type: `time`
    },

    language: {
      sql: `language`,
      type: `string`
    },
  },

  dataSource: `default`
});