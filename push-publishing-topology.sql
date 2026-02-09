-- Push Publishing Topology Query
-- Shows which environments exist and which endpoints (target servers) they push to

SELECT
    env.name AS environment_name,
    env.push_to_all,
    ep.server_name,
    ep.address,
    ep.port,
    ep.protocol,
    ep.enabled,
    ep.sending,
    CONCAT(ep.protocol, '://', ep.address, ':', ep.port) AS full_endpoint_url,
    CASE
        WHEN ep.enabled = true AND ep.sending = true THEN 'Active Sender'
        WHEN ep.enabled = true AND ep.sending = false THEN 'Active Receiver'
        WHEN ep.enabled = false THEN 'Disabled'
    END AS endpoint_status
FROM
    publishing_environment env
LEFT JOIN
    publishing_end_point ep ON ep.group_id = env.id
ORDER BY
    env.name, ep.server_name;

-- Summary: Count of endpoints per environment
SELECT
    env.name AS environment_name,
    COUNT(ep.id) AS endpoint_count,
    SUM(CASE WHEN ep.enabled = true THEN 1 ELSE 0 END) AS enabled_endpoints,
    SUM(CASE WHEN ep.sending = true THEN 1 ELSE 0 END) AS sending_endpoints
FROM
    publishing_environment env
LEFT JOIN
    publishing_end_point ep ON ep.group_id = env.id
GROUP BY
    env.id, env.name
ORDER BY
    env.name;

-- Recent Push Activity (last 30 days)
SELECT
    env.name AS environment_name,
    ep.server_name AS target_server,
    COUNT(DISTINCT pa.bundle_id) AS bundles_pushed,
    COUNT(pa.asset_id) AS assets_pushed,
    MAX(pa.push_date) AS last_push_date
FROM
    publishing_environment env
LEFT JOIN
    publishing_end_point ep ON ep.group_id = env.id
LEFT JOIN
    publishing_pushed_assets pa ON pa.environment_id = env.id
WHERE
    pa.push_date >= NOW() - INTERVAL '30 days'
GROUP BY
    env.id, env.name, ep.server_name
ORDER BY
    last_push_date DESC;
