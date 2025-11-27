
cube(`ConversionSummary`, {
  // 1) Source table in ClickHouse
  sql_table: `content_events_counter`,

  // 2) Measures
  measures: {
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
      description: 'Something here'
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
      description: 'Something here'
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
