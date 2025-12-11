/**
 * This model is used for calculating basic metrics related to events of type `conversion`.
 */
cube(`EventSummary`, {

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

    uniqueVisitors: {
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
      description: 'Total of events of type `conversion`',
      drillMembers: [
        day,
        customerId,
        clusterId,
        contextSiteId,
        contextUserId,
        identifier,
        title
      ]
    },

    // Filtered measure: Converting visitors
    uniqueConvertingVisitors: {
      sql: `context_user_id`,
      type: `countDistinct`,
      filters: [
        { sql: `${CUBE}.event_type = 'conversion'` }
      ],
      description: 'Total number of unique User IDs -- visitors -- who triggered a conversion event at some point'
    }

  },

  // 3) Dimensions
  dimensions: {
    day: {
      sql: `day`,
      type: `time`,
      title: 'Day',
      description: 'The day when the event was created.'
    },

    clusterId: {
      sql: `cluster_id`,
      type: `string`,
      title: 'Cluster ID',
      description: 'The ID or type of customer environment where the event was created.'
    },

    customerId: {
      sql: `customer_id`,
      type: `string`,
      title: 'Customer ID',
      description: 'The ID or name of the customer environment where the event was created.'
    },

    siteId: {
      sql: `context_site_id`,
      type: `string`,
      title: 'Site ID',
      description: 'The ID of the Site that the event was created for.'
    },

    eventType: {
      sql: `event_type`,
      type: `string`,
      title: 'Event Type',
      description: 'Type of tracked event (pageview, content_impression, content_click, etc.).'
    },

    userId: {
      sql: `context_user_id`,
      type: `string`,
      title: 'User ID',
      description: 'The ID of the User tha triggered the creation of the event.'
    },

    contentIdentifier: {
      sql: `identifier`,
      type: `string`,
      title: 'Identifier',
      description: 'The Identifier of the Contentlet that the event is related to.'
    },

    title: {
      sql: `title`,
      type: `string`,
      title: 'Title',
      description: 'The title of the Contentlet that the event is related to.'
    }
  },

});
