

CREATE TABLE content_events_counter
(
    day Date,

    cluster_id LowCardinality(String),
    customer_id LowCardinality(String),

    event_type LowCardinality(String),
    context_user_id String CODEC(ZSTD(3)),

    identifier String CODEC(ZSTD(3)),
    title String,

    daily_total UInt64
)
ENGINE = SummingMergeTree(daily_total)
ORDER BY (customer_id, cluster_id, context_user_id, day, identifier, title, event_type);


CREATE MATERIALIZED VIEW content_events_counter_mv TO content_events_counter AS
SELECT customer_id,
       cluster_id,
       event_type,
       context_user_id,
       toStartOfDay(utc_time) as day,
       (CASE WHEN event_type = 'pageview' THEN url ELSE content_identifier END) as identifier,
       (CASE WHEN event_type = 'pageview' THEN page_title ELSE content_title END) as title,
       count(*) as daily_total
FROM clickhouse_test_db.events
GROUP BY customer_id, cluster_id, context_user_id, day, identifier, title, event_type;

---

CREATE TABLE conversion_time
(
    cluster_id LowCardinality(String),
    customer_id LowCardinality(String),

    context_user_id String CODEC(ZSTD(3)),

    conversion_last_time AggregateFunction( max, DateTime64(3, 'UTC')),
    timestamp_last_time  AggregateFunction( max, DateTime64(3, 'UTC'))
)
ENGINE = AggregatingMergeTree
PARTITION BY (customer_id)
ORDER BY (customer_id, cluster_id, context_user_id);

---

CREATE TABLE content_presents_in_conversion
(
    day Date,
    last_timestamp DateTime64(3, 'UTC'),
    last_conversion_time DateTime64(3, 'UTC'),

    cluster_id LowCardinality(String),
    customer_id LowCardinality(String),

    event_type LowCardinality(String),
    context_user_id String CODEC(ZSTD(3)),

    identifier String CODEC(ZSTD(3)),
    title String,

    conversion_name String,
    conversion_count UInt32
)
ENGINE = SummingMergeTree
PARTITION BY (customer_id)
ORDER BY (customer_id, cluster_id, context_user_id, event_type, conversion_name, identifier, title);



CREATE MATERIALIZED VIEW content_presents_in_conversion_mv
REFRESH EVERY 1 MINUTE TO content_presents_in_conversion AS
WITH conversion AS (
    SELECT context_user_id,
           utc_time AS conversion_time,
           _timestamp,
           maxMerge(conversion_time.timestamp_last_time) as last_timestamp_previous_batch,
           maxMerge(conversion_time.conversion_last_time) as conversion_last_time,
           e.conversion_name,
           lag(_timestamp, 1) OVER (
               PARTITION BY context_user_id
               ORDER BY _timestamp
               ) AS previous_timestamp_current_batch,
           (CASE WHEN previous_timestamp_current_batch > last_timestamp_previous_batch THEN previous_timestamp_current_batch ELSE last_timestamp_previous_batch END) as previous_conversion_timestamp
    FROM events as e
             LEFT JOIN conversion_time on e.customer_id = conversion_time.customer_id AND e.cluster_id = conversion_time.cluster_id AND
                                          e.context_user_id = conversion_time.context_user_id
    WHERE event_type = 'conversion'
    group by context_user_id,utc_time, _timestamp, conversion_name
    HAVING (_timestamp >  last_timestamp_previous_batch AND _timestamp <= now())
)
SELECT
    toStartOfDay(conversion.conversion_time) as day,
    customer_id,
    cluster_id,
    (CASE WHEN event_type = 'pageview' THEN url ELSE content_identifier END) as identifier,
    (CASE WHEN event_type = 'pageview' THEN page_title ELSE content_title END) as title,
    event_type,
    context_user_id,
    conversion.conversion_name as conversion_name,
    count(*) AS conversion_count,
    max(conversion._timestamp) as last_timestamp,
    max(conversion.conversion_time) as last_conversion_time
FROM events e
         INNER JOIN conversion ON e.context_user_id = conversion.context_user_id AND
                                  e.utc_time < conversion.conversion_time AND
                                  e.utc_time > conversion.conversion_last_time AND
                                  event_type <> 'conversion'
GROUP BY customer_id, cluster_id, identifier, title, event_type, context_user_id, conversion.conversion_name, day;


---


CREATE MATERIALIZED VIEW conversion_time_mv
REFRESH EVERY 2 MINUTE TO conversion_time AS
SELECT customer_id,
       cluster_id,
       context_user_id,
       maxState(last_timestamp) as timestamp_last_time--,
       --maxState(last_conversion_time) as conversion_last_time
FROM clickhouse_test_db.content_presents_in_conversion
GROUP BY customer_id, cluster_id, context_user_id;







