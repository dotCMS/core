
cube(`EventSummary`, {
  // 1) Source table in ClickHouse
  sql: `
    SELECT day, cluster_id, customer_id, context_site_id, event_type, context_user_id, identifier, title, daily_total
    FROM content_events_counter
    WHERE ${FILTER_PARAMS.ContentAttribution.customerId.filter('customer_id')} AND (
      ${FILTER_PARAMS.ContentAttribution.clusterId ? FILTER_PARAMS.ContentAttribution.clusterId.filter('cluster_id') : '1=1'}
        OR (${FILTER_PARAMS.ContentAttribution.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})) AND
      ${FILTER_PARAMS.ContentAttribution.day.filter('day')}
  `,

  // 2) Measures
  measures: {
    totalUsers: {
      sql: `context_user_id`,
      type: `countDistinct`,
      title: 'Unique Users',
      description: 'Total number of unique users across all sessions'
    },
    // Generic measure: total events across all event types
    totalEvents: {
      sql: `daily_total`,
      type: `sum`,
      drillMembers: [
        day,
        eventType,
        customerId,
        clusterId,
        contextSiteId,
        contextUserId,
        identifier,
        title
      ]
    },

    // Filtered measure: Only conversion events
    conversionEvents: {
      sql: `daily_total`,
      type: `sum`,
      filters: [
        { sql: `${CUBE}.event_type = 'conversion'` }
      ],
      drillMembers: [
        day,
        customerId,
        clusterId,
        contextSiteId,
        contextUserId,
        identifier,
        title
      ],
      description: 'Total of events'
    },

    // Filtered measure: Converting visitors
    convertingVisitors: {
      sql: `daily_total`,
      type: `sum`,
      filters: [
        { sql: `${CUBE}.event_type = 'conversion'` }
      ],
      drillMembers: [
        day,
        customerId,
        clusterId,
        contextSiteId,
        contextUserId,
        identifier,
        title
      ],
      description: 'Total of conversion'
    },

    // (optional) If you later want a generic "by event type" measure:
    // you can just use `totalEvents` + a filter at query time.
  },

  // 3) Dimensions
  dimensions: {
    day: {
      sql: `day`,
      type: `time`
    },

    clusterId: {
      sql: `cluster_id`,
      type: `string`
    },

    customerId: {
      sql: `customer_id`,
      type: `string`
    },

    contextSiteId: {
      sql: `context_site_id`,
      type: `string`
    },

    eventType: {
      sql: `event_type`,
      type: `string`
    },

    contextUserId: {
      sql: `context_user_id`,
      type: `string`
    },

    identifier: {
      sql: `identifier`,
      type: `string`
    },

    title: {
      sql: `title`,
      type: `string`
    }
  },

  // 4) (Optional) Pre-aggregations – you can add these later if needed
  //    Right now your table is already daily-aggregated, so you may not
  //    need extra rollups immediately.
  /*
  preAggregations: {
    byDayCustomerSite: {
      type: `rollup`,
      measureReferences: [totalEvents],
      dimensionReferences: [customerId, clusterId, contextSiteId, eventType],
      timeDimensionReference: day,
      granularity: `day`,
      refreshKey: {
        every: `5 minutes`
      }
    }
  }
  */
});
