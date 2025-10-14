/**
 * This model is used for conversions or data that do not require analyzing the full session history. Its scope
 * is limited to a single request.
 *
 * Currently, this model is not used for A/B Testing.
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
            userlanguage, persona, doc_path, doc_host, doc_protocol, doc_hash,
            doc_search, screen_resolution, user_language, viewport_height, viewport_width,
            utm_campaign, utm_medium, utm_source, utm_term, utm_content,
            context_site_key, context_site_id, sessionid, context_user_id, request_id,
            cluster_id, customer_id, utc_time,
            content_identifier, content_inode, content_title, content_content_type,
            position_viewport_offset_pct,position_dom_index,
            element_text, element_type, element_id, element_class, element_attributes,
            custom_1, custom_2, custom_3, custom_4, custom_5, custom_6, custom_7, custom_8, custom_9, custom_10,
            custom_11, custom_12, custom_13, custom_14, custom_15, custom_16, custom_17, custom_18, custom_19, custom_20,
            custom_21, custom_22, custom_23, custom_24, custom_25, custom_26, custom_27, custom_28, custom_29, custom_30,
            custom_31, custom_32, custom_33, custom_34, custom_35, custom_36, custom_37, custom_38, custom_39, custom_40,
            custom_41, custom_42, custom_43, custom_44, custom_45, custom_46, custom_47, custom_48, custom_49, custom_50
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
      description: 'Type of tracked event (pageview, content_impression, content_click, etc.)'
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
      description: 'Complete URL of the page, without query parameters'
    },
    path: {
      sql: `doc_path`,
      type: `string`,
      title: 'Page Path',
      description: 'URL path without query parameters, domain and protocol'
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
      description: 'URL query string parameters, including the "?" character'
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
      description: 'Browser user agent as string'
    },
    browserLanguage: {
      sql: `user_language`,
      type: `string`,
      title: 'Browser Language',
      description: 'Preferred language setting from the browser'
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
      description: 'Authentication key generated for every Site in the Content Analytics App'
    },
    languageId: {
      sql: `userlanguage`,
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
    },
    contentIdentifier: {
      sql: `content_identifier`,
      type: `string`,
      title: 'Content Identifier',
      description: 'Content Identifier'
    },
    contentInode: {
      sql: `content_inode`,
      type: `string`,
      title: 'Content Inode',
      description: 'Content Inode'
    },
    contentTitle: {
      sql: `content_title`,
      type: `string`,
      title: 'Content Title',
      description: 'Content Title'
    },
    contentType: {
      sql: `content_content_type`,
      type: `string`,
      title: 'Content Type',
      description: 'Content Type'
    },
    viewPostOffset: {
      sql: `position_viewport_offset_pct`,
      type: `number`,
      title: 'View Post Offset',
      description: 'Browser Viewport Offset when the event occurred'
    },
    domIndex: {
      sql: `position_dom_index`,
      type: `number`,
      title: 'Dom Index',
      description: 'Element index in the DOM'
    },
    elementText: {
      sql: `element_text`,
      type: `string`,
      title: 'Element Text',
      description: 'Text of the DOM Element that was clicked'
    },
    elementType: {
      sql: `element_type`,
      type: `string`,
      title: 'Element Type',
      description: 'Type of the DOM Element that was clicked'
    },
    elementId: {
      sql: `element_id`,
      type: `string`,
      title: 'Element Id',
      description: 'Id of the DOM Element that was clicked'
    },
    elementClass: {
      sql: `element_class`,
      type: `string`,
      title: 'Element Class',
      description: 'Classes of the DOM Element that was clicked'
    },
    elementAttributes: {
      sql: `JSONExtract(element_attributes, 'Array(String)')`,
      type: `string`,
      title: 'Element Attributes',
      description: 'Attributes of the DOM Element that was clicked'
    },

    // Custom Attributes - Flexible fields for additional analytics data
    custom_1: { sql: `custom_1`, type: `string`, title: 'Custom Field 1', description: 'Custom analytics field 1' },
    custom_2: { sql: `custom_2`, type: `string`, title: 'Custom Field 2', description: 'Custom analytics field 2' },
    custom_3: { sql: `custom_3`, type: `string`, title: 'Custom Field 3', description: 'Custom analytics field 3' },
    custom_4: { sql: `custom_4`, type: `string`, title: 'Custom Field 4', description: 'Custom analytics field 4' },
    custom_5: { sql: `custom_5`, type: `string`, title: 'Custom Field 5', description: 'Custom analytics field 5' },
    custom_6: { sql: `custom_6`, type: `string`, title: 'Custom Field 6', description: 'Custom analytics field 6' },
    custom_7: { sql: `custom_7`, type: `string`, title: 'Custom Field 7', description: 'Custom analytics field 7' },
    custom_8: { sql: `custom_8`, type: `string`, title: 'Custom Field 8', description: 'Custom analytics field 8' },
    custom_9: { sql: `custom_9`, type: `string`, title: 'Custom Field 9', description: 'Custom analytics field 9' },
    custom_10: { sql: `custom_10`, type: `string`, title: 'Custom Field 10', description: 'Custom analytics field 10' },
    custom_11: { sql: `custom_11`, type: `string`, title: 'Custom Field 11', description: 'Custom analytics field 11' },
    custom_12: { sql: `custom_12`, type: `string`, title: 'Custom Field 12', description: 'Custom analytics field 12' },
    custom_13: { sql: `custom_13`, type: `string`, title: 'Custom Field 13', description: 'Custom analytics field 13' },
    custom_14: { sql: `custom_14`, type: `string`, title: 'Custom Field 14', description: 'Custom analytics field 14' },
    custom_15: { sql: `custom_15`, type: `string`, title: 'Custom Field 15', description: 'Custom analytics field 15' },
    custom_16: { sql: `custom_16`, type: `string`, title: 'Custom Field 16', description: 'Custom analytics field 16' },
    custom_17: { sql: `custom_17`, type: `string`, title: 'Custom Field 17', description: 'Custom analytics field 17' },
    custom_18: { sql: `custom_18`, type: `string`, title: 'Custom Field 18', description: 'Custom analytics field 18' },
    custom_19: { sql: `custom_19`, type: `string`, title: 'Custom Field 19', description: 'Custom analytics field 19' },
    custom_20: { sql: `custom_20`, type: `string`, title: 'Custom Field 20', description: 'Custom analytics field 20' },
    custom_21: { sql: `custom_21`, type: `string`, title: 'Custom Field 21', description: 'Custom analytics field 21' },
    custom_22: { sql: `custom_22`, type: `string`, title: 'Custom Field 22', description: 'Custom analytics field 22' },
    custom_23: { sql: `custom_23`, type: `string`, title: 'Custom Field 23', description: 'Custom analytics field 23' },
    custom_24: { sql: `custom_24`, type: `string`, title: 'Custom Field 24', description: 'Custom analytics field 24' },
    custom_25: { sql: `custom_25`, type: `string`, title: 'Custom Field 25', description: 'Custom analytics field 25' },
    custom_26: { sql: `custom_26`, type: `string`, title: 'Custom Field 26', description: 'Custom analytics field 26' },
    custom_27: { sql: `custom_27`, type: `string`, title: 'Custom Field 27', description: 'Custom analytics field 27' },
    custom_28: { sql: `custom_28`, type: `string`, title: 'Custom Field 28', description: 'Custom analytics field 28' },
    custom_29: { sql: `custom_29`, type: `string`, title: 'Custom Field 29', description: 'Custom analytics field 29' },
    custom_30: { sql: `custom_30`, type: `string`, title: 'Custom Field 30', description: 'Custom analytics field 30' },
    custom_31: { sql: `custom_31`, type: `string`, title: 'Custom Field 31', description: 'Custom analytics field 31' },
    custom_32: { sql: `custom_32`, type: `string`, title: 'Custom Field 32', description: 'Custom analytics field 32' },
    custom_33: { sql: `custom_33`, type: `string`, title: 'Custom Field 33', description: 'Custom analytics field 33' },
    custom_34: { sql: `custom_34`, type: `string`, title: 'Custom Field 34', description: 'Custom analytics field 34' },
    custom_35: { sql: `custom_35`, type: `string`, title: 'Custom Field 35', description: 'Custom analytics field 35' },
    custom_36: { sql: `custom_36`, type: `string`, title: 'Custom Field 36', description: 'Custom analytics field 36' },
    custom_37: { sql: `custom_37`, type: `string`, title: 'Custom Field 37', description: 'Custom analytics field 37' },
    custom_38: { sql: `custom_38`, type: `string`, title: 'Custom Field 38', description: 'Custom analytics field 38' },
    custom_39: { sql: `custom_39`, type: `string`, title: 'Custom Field 39', description: 'Custom analytics field 39' },
    custom_40: { sql: `custom_40`, type: `string`, title: 'Custom Field 40', description: 'Custom analytics field 40' },
    custom_41: { sql: `custom_41`, type: `string`, title: 'Custom Field 41', description: 'Custom analytics field 41' },
    custom_42: { sql: `custom_42`, type: `string`, title: 'Custom Field 42', description: 'Custom analytics field 42' },
    custom_43: { sql: `custom_43`, type: `string`, title: 'Custom Field 43', description: 'Custom analytics field 43' },
    custom_44: { sql: `custom_44`, type: `string`, title: 'Custom Field 44', description: 'Custom analytics field 44' },
    custom_45: { sql: `custom_45`, type: `string`, title: 'Custom Field 45', description: 'Custom analytics field 45' },
    custom_46: { sql: `custom_46`, type: `string`, title: 'Custom Field 46', description: 'Custom analytics field 46' },
    custom_47: { sql: `custom_47`, type: `string`, title: 'Custom Field 47', description: 'Custom analytics field 47' },
    custom_48: { sql: `custom_48`, type: `string`, title: 'Custom Field 48', description: 'Custom analytics field 48' },
    custom_49: { sql: `custom_49`, type: `string`, title: 'Custom Field 49', description: 'Custom analytics field 49' },
    custom_50: { sql: `custom_50`, type: `string`, title: 'Custom Field 50', description: 'Custom analytics field 50' }
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
    totalRequest: {
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