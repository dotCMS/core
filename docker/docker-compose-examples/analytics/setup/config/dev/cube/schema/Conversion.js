/**
 * This model returns all conversions. You can also join it with the ContentAttribution cube
 * to include the top three content items that contributed the most to each conversion.
 */

cube('Conversion', {
    sql: `
        WITH conversion AS (
            SELECT 
                customer_id,
                cluster_id,
                title as conversion_name,
                context_site_id,
                SUM(daily_total) AS total,
                max(day) as day
            FROM content_events_counter
            WHERE event_type = 'conversion' AND
                ${FILTER_PARAMS.Conversion.customerId.filter('customer_id')} AND (
                ${FILTER_PARAMS.Conversion.clusterId ? FILTER_PARAMS.Conversion.clusterId.filter('cluster_id') : '1=1'}
                   OR (${FILTER_PARAMS.Conversion.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})) AND
                ${FILTER_PARAMS.Conversion.day.filter('day')} AND
                ${FILTER_PARAMS.Conversion.conversionName.filter('conversion_name')}
            GROUP BY customer_id, cluster_id,title, context_site_id),
        top_attributed_content AS (
            SELECT
                customer_id,
                cluster_id,
                conversion_name,
                context_site_id,
                groupArray(3)(
                    map(
                        'identifier', identifier,
                        'title', title,
                        'conversions', toString(total_conversion_count),
                        'event_type', event_type
                    )
                ) as top_attributed_content
            FROM (
                    SELECT
                        customer_id,
                        cluster_id,
                        conversion_name,
                        identifier,
                        title,
                        context_site_id,
                        event_type,
                        SUM(conversion_count) AS total_conversion_count
                    FROM content_presents_in_conversion
                    WHERE ${FILTER_PARAMS.Conversion.customerId.filter('customer_id')} AND (
                        ${FILTER_PARAMS.Conversion.clusterId ? FILTER_PARAMS.Conversion.clusterId.filter('cluster_id') : '1=1'}
                            OR (${FILTER_PARAMS.Conversion.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})) AND
                        ${FILTER_PARAMS.Conversion.day.filter('day')} AND
                        ${FILTER_PARAMS.Conversion.conversionName.filter('conversion_name')}
                    GROUP BY customer_id, cluster_id, conversion_name, identifier, title, context_site_id, event_type
                    ORDER BY conversion_name, total_conversion_count DESC
                ) as attributed_content
            GROUP BY customer_id, cluster_id, conversion_name, context_site_id
            LIMIT 3 BY conversion_name, context_site_id
        ),
        total_conversion AS (
            SELECT sum(total) as total FROM conversion 
            WHERE ${FILTER_PARAMS.Conversion.customerId.filter('customer_id')} AND (
                ${FILTER_PARAMS.Conversion.clusterId ? FILTER_PARAMS.Conversion.clusterId.filter('cluster_id') : '1=1'}
                   OR (${FILTER_PARAMS.Conversion.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})) AND
               ${FILTER_PARAMS.Conversion.day.filter('day')} AND
               ${FILTER_PARAMS.Conversion.conversionName.filter('conversion_name')}
        )
        SELECT
            customer_id,
            cluster_id,
            conversion_name,
            context_site_id,
            total as total_conversion,
            ((total_conversion * 100)/ (SELECT total FROM total_conversion)) as conv_rate,
            day,
            arrayMap(
                x -> mapUpdate(x, map('conv_rate', toString((toInt64(x['conversions']) * 100) / total_conversion))),
                top_attributed_content
            ) as top_attributed_content
        FROM conversion INNER JOIN top_attributed_content
            ON conversion.conversion_name = top_attributed_content.conversion_name
    `,
    dimensions: {
        conversionName: {
            sql: `conversion_name`,
            type: `string`,
            title: 'Conversion Name',
            description: 'Conversion Name'
        },
        siteId: {
            sql: `context_site_id`,
            type: `string`,
            title: 'Site ID',
            description: 'dotCMS site identifier'
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
        totalConversion: {
            sql: `total_conversion`,
            type: `number`,
            title: 'Total of Conversion',
            description: 'Total number of conversion triggerred'
        },
        convRate: {
            sql: `conv_rate`,
            type: `number`,
            title: 'Conversion Rate',
            description: '(totalConversion * 100)/ total'
        },
        day: {
            sql: `day`,
            type: `time`,
            title: 'Conversion Day',
            description: 'Day when the conversion happend'
        },
        topAttributedContent: {
            sql: `top_attributed_content`,
            type: `string`,
            title: 'Top Attributed Content',
            description: 'Top 3 Content Leading to the Conversion'
        }
    }
});