-- =====================================================================
-- Stores the latest known conversion timestamp per user, but in aggregate function format.
-- Two aggregated fields:
--
-- conversion_last_time → last conversion event time
--
-- timestamp_last_time → last processed timestamp inside content_presents_in_conversion
--
-- Why AggregatingMergeTree?
--
-- Because conversion_time_mv inserts aggregate states (maxState) and later merges them.
-- This table provides a "boundary" so that future incremental batches don't reprocess old records.
-- =====================================================================
CREATE TABLE IF NOT EXISTS analytics.conversion_time
(
    environment LowCardinality(String),
    customer_id LowCardinality(String),

    site_id String,
    user_id String CODEC(ZSTD(3)),

    conversion_last_time AggregateFunction( max, DateTime64(3, 'UTC')),
    timestamp_last_time  AggregateFunction( max, DateTime64(3, 'UTC'))
)
    ENGINE = ReplicatedAggregatingMergeTree()
PARTITION BY (customer_id, environment)
ORDER BY (customer_id, environment, user_id);



-- =====================================================================
-- Tracks which content a user interacted with prior to a conversion and after the user's previous conversion
-- =====================================================================
CREATE TABLE IF NOT EXISTS analytics.content_presents_in_conversion
(
    day Date,
    last_timestamp DateTime64(3, 'UTC'),
    last_conversion_time DateTime64(3, 'UTC'),

    environment LowCardinality(String),
    customer_id LowCardinality(String),

    site_id String,

    event_type LowCardinality(String),
    user_id String CODEC(ZSTD(3)),

    identifier String CODEC(ZSTD(3)),
    title String,

    conversion_name String,
    conversion_count UInt32,
    events_count UInt32
)
    ENGINE = ReplicatedSummingMergeTree()
PARTITION BY (customer_id, environment, toYYYYMM(day))
ORDER BY (customer_id, environment, user_id, event_type, conversion_name, identifier, title, day);



-- =====================================================================
-- It does:
--
-- Identifies new conversions since last refresh
-- Locates content seen by the user right before each conversion
-- Inserts attribution rows into content_presents_in_conversion
--
-- How it works (step-by-step)
-- A) Define conversion CTE
-- For each conversion event:
-- Joins against conversion_time to get the previous batch's last timestamps
-- Uses lag() to find previous conversion in current batch
--
-- Calculates:
-- previous_conversion_timestamp = max(previous_timestamp_current_batch, last_timestamp_previous_batch)
--
-- Filters conversions that are:
--
-- new (timestamp > last_timestamp_previous_batch)
-- recent (timestamp <= now())
--
-- This ensures incremental processing, no duplicates.
--
-- B) Join events leading to conversion
--
-- Matches events where:
--
-- e.event_time < conversion.conversion_time
-- e.event_time > conversion.conversion_last_time
-- event_type <> 'conversion'

-- Meaning:
--
-- Only consider events between the previous conversion timestamp and this conversion timestamp.
--
-- C) Group and insert
--
-- Inserts rows summarizing content presence before the conversion.
-- =====================================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.content_presents_in_conversion_mv
-- Refreshing every 30 seconds FOR LOCAL DEVELOPMENT ONLY! For DEV, use at least REFRESH EVERY 15 MINUTE
REFRESH EVERY 30 SECOND
    APPEND TO analytics.content_presents_in_conversion AS
WITH conversion AS (
    SELECT user_id,
           event_time AS conversion_time,
           timestamp,
           maxMerge(conversion_time.timestamp_last_time) as last_timestamp_previous_batch,
           maxMerge(conversion_time.conversion_last_time) as conversion_last_time,
           e.conversion_name,
           lag(timestamp, 1) OVER (
               PARTITION BY user_id
               ORDER BY timestamp
               ) AS previous_timestamp_current_batch,
           lag(event_time, 1) OVER (
               PARTITION BY user_id
               ORDER BY event_time
               ) AS previous_event_time_current_batch,
           (CASE WHEN previous_event_time_current_batch > conversion_last_time THEN previous_event_time_current_batch ELSE conversion_last_time END) as previous_conversion_time
    FROM analytics.events as e
             LEFT JOIN analytics.conversion_time on e.customer_id = conversion_time.customer_id AND e.environment = conversion_time.environment AND
                                                             e.user_id = conversion_time.user_id AND e.site_id = conversion_time.site_id
    WHERE event_type = 'conversion'
    group by user_id,event_time, timestamp, conversion_name
    HAVING (timestamp >=  last_timestamp_previous_batch AND timestamp <= now())
)
SELECT
    toStartOfDay(conversion.conversion_time) as day,
    customer_id,
    environment,
    (CASE WHEN event_type = 'pageview' THEN doc_path ELSE content_identifier END) as identifier,
    (CASE WHEN event_type = 'pageview' THEN page_title ELSE content_title END) as title,
    event_type,
    user_id,
    site_id,
    conversion.conversion_name as conversion_name,
    count(*) AS events_count,
    count(DISTINCT conversion_time) AS conversion_count,
    max(conversion.timestamp) as last_timestamp,
    max(conversion.conversion_time) as last_conversion_time
FROM analytics.events e
         INNER JOIN conversion ON e.user_id = conversion.user_id AND
                                  e.event_time < conversion.conversion_time AND
                                  e.event_time > conversion.previous_conversion_time AND
                                  event_type <> 'conversion'
GROUP BY customer_id, environment, identifier, title, event_type, user_id, conversion.conversion_name, day, site_id;



-- =====================================================================
-- Updates the conversion_time table using the output of content_presents_in_conversion. Every time new attribution rows are emitted
--
-- This ensures:
--
-- Next execution of the refreshable MV knows where the last batch ended
--
-- Prevents reprocessing or double counting
-- =====================================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.conversion_time_mv TO analytics.conversion_time AS
SELECT customer_id,
       environment,
       user_id,
       site_id,
       maxState(last_timestamp) as timestamp_last_time,
       maxState(last_conversion_time) as conversion_last_time
FROM analytics.content_presents_in_conversion
GROUP BY customer_id, environment, user_id, site_id;