/**
 * This model returns all conversions. You can also join it with the ContentAttribution cube
 * to include the top three content items that contributed the most to each conversion.
 */

cube('Conversion', {
    sql: `
        SELECT
            title,
            customer_id,
            cluster_id,
            context_site_id,
            SUM(daily_total) AS total
        FROM content_events_counter
        WHERE event_type = 'conversion' AND 
                ${FILTER_PARAMS.Conversion.customerId.filter('customer_id')}
                AND (
                    ${FILTER_PARAMS.Conversion.clusterId ? FILTER_PARAMS.Conversion.clusterId.filter('cluster_id') : '1=1'}
                    OR (${FILTER_PARAMS.Conversion.clusterId ? 'FALSE' : '(cluster_id IS NULL OR cluster_id = \'\')'})
              )
        GROUP BY title, customer_id, cluster_id, context_site_id`,
    joins: {
        ContentAttribution: {
            relationship: `one_to_many`,
            sql: `${CUBE.conversionName} = ${ContentAttribution.conversionName} AND
            ${CUBE.customerId} = ${ContentAttribution.customerId} AND
            ${CUBE.clusterId} = ${ContentAttribution.clusterId}`
        }
    },
    dimensions: {
        id: {
            sql: `concat(customer_id, '-', cluster_id, '-', title)`,
            type: `string`,
            primaryKey: true
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
        conversionName: {
            sql: `title`,
            type: `string`,
            title: 'Conversion Name',
            description: 'Conversion Name'
        }
    },
    measures: {
        countConversion: {
            type: `count`,
            title: 'Count',
            description: 'Total of conversion'
        },
        attributedContents: {
            sql: `groupArray(3)(${ContentAttribution.identifier})`,
            type: `string`,
            title: 'Total',
            description: 'Total'
        }
    }
});