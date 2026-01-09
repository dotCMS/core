/**
 * This model calculates how often a piece of content appears in the path leading to a conversion.
 * Let’s walk through an example to see what this means.
 *
 * Imagine we have the following events ordered by utc_time:
 *
 * page_view, url = /page-1, utc_time = 2025-11-26 08:09:49
 * content_impression, content_id = 1, utc_time = 2025-11-26 08:09:59
 * conversion, name = download, utc_time = 2025-11-26 08:10:09
 * page_view, url = /page-1, utc_time = 2025-11-27 08:11:00
 * content_click, content_id = 2, utc_time = 2025-11-27 08:11:31
 * conversion, name = download, utc_time = 2025-11-28 08:10:39
 *
 * With this model, if we aggregate the data with a daily granularity, we would get results like:
 *
 * page_view — identifier: /page-1, day: 2025-11-26, count: 1
 * content_impression — identifier: 1, day: 2025-11-26, count: 1
 * content_click — identifier: 2, day: 2025-11-28, count: 1
 * page_view — identifier: /page-1, day: 2025-11-28, count: 1
 */


cube('ContentAttribution', {
    sql: `
       WITH conversions_total AS (
           SELECT customer_id, cluster_id, context_user_id, context_site_id, event_type, identifier, title, conversion_name, max(day) as day, sum(conversion_count) as conversions
           FROM (
                    SELECT * FROM content_presents_in_conversion
                    WHERE ${FILTER_PARAMS.ContentAttribution.customerId.filter('customer_id')} AND (
                        ${FILTER_PARAMS.ContentAttribution.clusterId ? FILTER_PARAMS.ContentAttribution.clusterId.filter('cluster_id') : '1=1'}
                            OR (${FILTER_PARAMS.ContentAttribution.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})) AND
                       ${FILTER_PARAMS.ContentAttribution.day.filter('day')} AND
                       ${FILTER_PARAMS.ContentAttribution.conversionName.filter('conversion_name')}
               )
            GROUP BY customer_id, cluster_id, context_user_id, context_site_id, event_type, identifier, title, conversion_name
       ),
        events_total AS (
            SELECT customer_id, cluster_id, context_user_id, context_site_id, event_type, identifier, title, sum(content_events_counter.daily_total) as totalEvents
            FROM content_events_counter
            WHERE ${FILTER_PARAMS.ContentAttribution.customerId.filter('customer_id')} AND (
                   ${FILTER_PARAMS.ContentAttribution.clusterId ? FILTER_PARAMS.ContentAttribution.clusterId.filter('cluster_id') : '1=1'}
                        OR (${FILTER_PARAMS.ContentAttribution.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})) AND 
                  ${FILTER_PARAMS.ContentAttribution.day.filter('day')} AND
                  event_type <> 'conversion'    
            GROUP BY customer_id, cluster_id, context_user_id, context_site_id, event_type, identifier, title
        )
       SELECT customer_id, cluster_id, context_user_id, context_site_id, event_type, identifier, title, day, conversions, totalEvents, (conversions * 100)/totalEvents as convRate
       FROM conversions_total
                INNER JOIN events_total ON
           conversions_total.customer_id = events_total.customer_id AND
           conversions_total.cluster_id = events_total.cluster_id AND
           conversions_total.context_user_id = events_total.context_user_id AND
           conversions_total.context_site_id = events_total.context_site_id AND
           conversions_total.event_type = events_total.event_type AND
           conversions_total.identifier = events_total.identifier AND
           conversions_total.title = events_total.title`,
    dimensions: {
        day: {
            sql: `day`,
            type: `time`,
            title: 'Conversion Day',
            description: 'Day when the conversion happend'
        },
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
        siteId: {
            sql: `context_site_id`,
            type: `string`,
            title: 'Site ID',
            description: 'dotCMS site identifier'
        },
        eventType: {
            sql: `event_type`,
            type: `string`,
            title: 'Event Type',
            description: 'Type of tracked event (pageview, content_impression, content_click, etc.)'
        },
        userId: {
            sql: `context_user_id`,
            type: `string`,
            title: 'User ID',
            description: 'Unique identifier for the user'
        },
        identifier: {
            sql: `identifier`,
            type: `string`,
            title: 'Identifier',
            description: 'If the content is a Page it is the URL, otherwise it is the content identifier'
        },
        title: {
            sql: `title`,
            type: `string`,
            title: 'Identifier',
            description: 'If the content is a Page it is the Page title, otherwise it is the content title field value'
        },
        conversionName: {
            sql: `conversionName`,
            type: `string`,
            title: 'Conversion Name',
            description: 'Conversions this content helped achieve',
        },
        conversions: {
            sql: `conversions`,
            type: `number`,
            title: 'Conversions',
            description: 'Total of conversion that this content helped achieve'
        },
        events: {
            sql: `totalEvents`,
            type: `number`,
            title: 'Total of Events',
            description: 'Total of events'
        },
        convRate: {
            sql: `convRate`,
            type: `number`,
            title: 'Conversion Rate',
            description: '(conversion * 100)/ events'
        }
    },
    measures: {
        sumConversions: {
            sql: `conversions`,
            type: `sum`,
            title: 'Conversions',
            description: 'Total of conversion that this content helped achieve'
        },
        sumEvents: {
            sql: `totalEvents`,
            type: `sum`,
            title: 'Total of Events',
            description: 'Total of events'
        }
    }
});