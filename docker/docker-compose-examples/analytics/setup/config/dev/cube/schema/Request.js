/**
 * Request Cube - General Analytics Data
 * 
 * SECURITY: This cube implements defense-in-depth security filtering:
 * - Layer 1 (Base SQL): FILTER_PARAMS provide customer_id/cluster_id filtering at SQL level  
 * - Layer 2 (Runtime): cube.js queryRewrite adds additional runtime security filters
 * 
 * PERFORMANCE: Optimized for ClickHouse with explicit column selection and base SQL filtering
 */
cube('request', {
  sql: `SELECT 
          event_type, user_agent, referer, url, doc_encoding, page_title,
          language_id, persona, doc_path, doc_host, doc_protocol, doc_hash,
          doc_search, screen_resolution, user_language, viewport_height, viewport_width,
          utm_campaign, utm_medium, utm_source, utm_term, utm_content,
          context_site_key, context_site_id, sessionid, context_user_id, request_id,
          cluster_id, customer_id, utc_time
        FROM events 
        WHERE ${FILTER_PARAMS.request.customerId.filter('customer_id')}
        AND (
          ${FILTER_PARAMS.request.clusterId ? FILTER_PARAMS.request.clusterId.filter('cluster_id') : '1=1'}
          OR (${FILTER_PARAMS.request.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})
        )`,
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
    // Event and Request Information
    eventType: { 
      sql: `event_type`, 
      type: `string`,
      title: 'Event Type',
      description: 'Type of tracked event (pageview, click, etc.)'
    },
    requestId: { 
      sql: `request_id`, 
      type: `string`,
      title: 'Request ID',
      description: 'Unique identifier for the HTTP request'
    },
    
    // URL and Page Information
    url: { 
      sql: `url`, 
      type: `string`,
      title: 'Full URL',
      description: 'Complete URL of the page visited'
    },
    path: { 
      sql: `doc_path`, 
      type: `string`,
      title: 'Page Path',
      description: 'URL path component (without domain)'
    },
    domain: { 
      sql: `doc_host`, 
      type: `string`,
      title: 'Domain',
      description: 'Hostname/domain of the visited page'
    },
    protocol: { 
      sql: `doc_protocol`, 
      type: `string`,
      title: 'Protocol',
      description: 'HTTP protocol (http/https)'
    },
    urlHash: { 
      sql: `doc_hash`, 
      type: `string`,
      title: 'URL Hash',
      description: 'Fragment identifier part of URL (after #)'
    },
    queryParameters: { 
      sql: `doc_search`, 
      type: `string`,
      title: 'Query Parameters',
      description: 'URL query string parameters'
    },
    pageTitle: { 
      sql: `page_title`, 
      type: `string`,
      title: 'Page Title',
      description: 'HTML title of the page'
    },
    
    // User and Session Information
    userId: { 
      sql: `context_user_id`, 
      type: `string`,
      title: 'User ID',
      description: 'Unique identifier for the user'
    },
    sessionId: { 
      sql: `sessionid`, 
      type: `string`,
      title: 'Session ID',
      description: 'Unique identifier for the user session'
    },
    
    // Browser and Device Information
    userAgent: { 
      sql: `user_agent`, 
      type: `string`,
      title: 'User Agent',
      description: 'Browser user agent string'
    },
    browserLanguage: { 
      sql: `user_language`, 
      type: `string`,
      title: 'Browser Language',
      description: 'Preferred language setting from browser'
    },
    screenResolution: { 
      sql: `screen_resolution`, 
      type: `string`,
      title: 'Screen Resolution',
      description: 'Screen resolution (width x height)'
    },
    viewportHeight: { 
      sql: `viewport_height`, 
      type: `number`,
      title: 'Viewport Height',
      description: 'Height of browser viewport in pixels'
    },
    viewportWidth: { 
      sql: `viewport_width`, 
      type: `number`,
      title: 'Viewport Width',
      description: 'Width of browser viewport in pixels'
    },
    
    // Traffic Source and Marketing
    referer: { 
      sql: `referer`, 
      type: `string`,
      title: 'Referer',
      description: 'HTTP referer header (previous page URL)'
    },
    utmSource: { 
      sql: `utm_source`, 
      type: `string`,
      title: 'UTM Source',
      description: 'Marketing campaign source parameter'
    },
    utmMedium: { 
      sql: `utm_medium`, 
      type: `string`,
      title: 'UTM Medium',
      description: 'Marketing campaign medium parameter'
    },
    utmCampaign: { 
      sql: `utm_campaign`, 
      type: `string`,
      title: 'UTM Campaign',
      description: 'Marketing campaign name parameter'
    },
    utmTerm: { 
      sql: `utm_term`, 
      type: `string`,
      title: 'UTM Term',
      description: 'Marketing campaign keyword parameter'
    },
    utmContent: { 
      sql: `utm_content`, 
      type: `string`,
      title: 'UTM Content',
      description: 'Marketing campaign content parameter'
    },
    
    // Site and Content Information
    siteId: { 
      sql: `context_site_id`, 
      type: `string`,
      title: 'Site ID',
      description: 'dotCMS site identifier'
    },
    siteKey: { 
      sql: `context_site_key`, 
      type: `string`,
      title: 'Site Key',
      description: 'dotCMS site key/hostname'
    },
    languageId: { 
      sql: `language_id`, 
      type: `string`,
      title: 'Language ID',
      description: 'Content language identifier'
    },
    persona: { 
      sql: `persona`, 
      type: `string`,
      title: 'Persona',
      description: 'User persona/segment classification'
    },
    encoding: { 
      sql: `doc_encoding`, 
      type: `string`,
      title: 'Document Encoding',
      description: 'Character encoding of the document'
    },
    
    // Time Information
    createdAt: { 
      sql: `utc_time`, 
      type: `time`,
      title: 'Timestamp',
      description: 'UTC timestamp when the event occurred'
    },
    
    // Security dimensions for multi-tenant filtering
    customerId: { 
      sql: `customer_id`, 
      type: `string`,
      title: 'Customer ID',
      description: 'Multi-tenant customer identifier for security filtering'
    },
    clusterId: { 
      sql: `cluster_id`, 
      type: `string`,
      title: 'Cluster ID',
      description: 'Multi-tenant cluster identifier for security filtering'
    }
  },
  measures: {
    count: {
      type: `count`,
      title: 'Total Events',
      description: 'Total number of tracked events/requests'
    },
    totalSessions: {
      sql: `sessionid`,
      type: `countDistinct`,
      title: 'Unique Sessions',
      description: 'Total number of unique user sessions'
    },
    totalRequests: {
      sql: `request_id`,
      type: `countDistinct`,
      title: 'Unique Requests',
      description: 'Total number of unique HTTP requests'
    },
    totalUsers: {
      sql: `context_user_id`,
      type: `countDistinct`,
      title: 'Unique Users',
      description: 'Total number of unique users across all sessions'
    }
  }
});