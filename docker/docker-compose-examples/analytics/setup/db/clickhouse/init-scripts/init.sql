-- =====================================================================
-- dotCMS Analytics: ClickHouse schema initialization
--
-- This script creates the analytics database, core fact tables, and
-- materialized views used by dotCMS/Jitsu ingestion and reporting.
--
-- Objects documented below:
-- - Database: clickhouse_test_db
-- - Table: events
--   Purpose: Raw analytics event stream (pageviews, content events,
--            conversions, etc.). Grain: one row per event.
-- - Table: content_events_counter
--   Purpose: Daily rollup of events per user/content/page for faster
--            querying. Grain: per day, per user, per identifier/title/type.
--   Populated by: content_events_counter_mv.
-- - Table: conversion_time
--   Purpose: Tracks the last seen timestamps per user to incrementally
--            process conversions in subsequent batches.
--   Populated by: conversion_time_mv.
-- - Table: content_presents_in_conversion
--   Purpose: Aggregates which content/page impressions/click/view occurred before a
--            conversion within the same user session/window. Used to
--            attribute conversions to content exposure.
--   Populated by: content_presents_in_conversion_mv.
-- - Materialized Views:
--   * content_events_counter_mv: maintains daily counts in content_events_counter.
--   * content_presents_in_conversion_mv: incremental windowed joins to maintain
--     content_presents_in_conversion; runs every minute.
--   * conversion_time_mv: maintains last processed timestamps in conversion_time.
-- =====================================================================


-- =====================================================================
-- This is the raw event ingestion table. All data from Jitsu/collectors enters here first.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS clickhouse_test_db;
CREATE TABLE IF NOT EXISTS clickhouse_test_db.events
(
    -- ######################################################
    --                  Jitsu Properties
    -- ######################################################
    _timestamp DateTime64(3, 'UTC'),
    event_type String,
    original_url String,
    doc_host String,
    doc_path String,
    doc_search String,
    page_title String,
    referer String,
    doc_encoding String,
    local_tz_offset Int64,
    eventn_ctx_event_id String,
    user_agent String,
    parsed_ua_device_brand String,
    parsed_ua_device_family String,
    parsed_ua_device_model String,
    parsed_ua_os_family String,
    parsed_ua_os_version String,
    parsed_ua_ua_family String,
    parsed_ua_ua_version String,
    parsed_ua_bot UInt8,
    source_ip String,
    vp_size String,
    utm_campaign String,
    utm_medium String,
    utm_source String,
    utm_term String,
    utm_content String,
    doc_hash String,
    doc_protocol String,
    user_language String,
    context_site_key String,
    context_user_id String,
    screen_resolution String,
    viewport_height String,
    viewport_width String,
    context_site_id String,
    api_key String,
    cluster_id String,
    customer_id String,
    src String,
    user_anonymous_id String,
    user_hashed_anonymous_id String,
    custom_1 String,
    custom_2 String,
    custom_3 String,
    custom_4 String,
    custom_5 String,
    custom_6 String,
    custom_7 String,
    custom_8 String,
    custom_9 String,
    custom_10 String,
    custom_11 String,
    custom_12 String,
    custom_13 String,
    custom_14 String,
    custom_15 String,
    custom_16 String,
    custom_17 String,
    custom_18 String,
    custom_19 String,
    custom_20 String,
    custom_21 String,
    custom_22 String,
    custom_23 String,
    custom_24 String,
    custom_25 String,
    custom_26 String,
    custom_27 String,
    custom_28 String,
    custom_29 String,
    custom_30 String,
    custom_31 String,
    custom_32 String,
    custom_33 String,
    custom_34 String,
    custom_35 String,
    custom_36 String,
    custom_37 String,
    custom_38 String,
    custom_39 String,
    custom_40 String,
    custom_41 String,
    custom_42 String,
    custom_43 String,
    custom_44 String,
    custom_45 String,
    custom_46 String,
    custom_47 String,
    custom_48 String,
    custom_49 String,
    custom_50 String,


    -- ######################################################
    --      Jitsu and Backend Data Collectors Properties
    -- ######################################################
    url String,
    utc_time DateTime,
    request_id String,
    sessionid String,
    sessionnew UInt8,


    -- ######################################################
    --                 Experiments Properties
    -- ######################################################
    isexperimentpage Bool DEFAULT false,
    experiment String,
    variant String,
    persona String,
    userlanguage String,
    lookbackwindow String,
    runningid String,
    istargetpage Bool DEFAULT false,
    ip String,


    -- ######################################################
    --              Used in UVE_MODE_CHANGE event
    -- ######################################################
    frommode String,
    tomode String,


    -- ######################################################
    --              Used in content_impression event
    -- ######################################################
    content_identifier String,
    content_inode String,
    content_title String,
    content_content_type String,

    position_viewport_offset_pct Int16,
    position_dom_index Int8,

    -- ######################################################
    --              Used in content_click event
    -- ######################################################
    element_text String,
    element_type String,
    element_id String,
    element_class String,
    element_attributes String,

    -- ######################################################
    --              Used in conversion event
    -- ######################################################
    conversion_name String
) Engine = MergeTree()
    PARTITION BY customer_id
    ORDER BY (_timestamp, customer_id)
    SETTINGS index_granularity = 8192;

ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_utc_time (utc_time) TYPE minmax GRANULARITY 1;
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_cluster_id (cluster_id) TYPE minmax GRANULARITY 1;


-- =====================================================================
-- Stores daily aggregated counts of events per:
--
-- day
-- cluster_id
-- customer_id
-- event_type
-- context_user_id
-- identifier (URL or content_id)
-- title

-- Why SummingMergeTree?

-- Because the MV inserts pre-aggregated rows, and daily_total is summed on merge.

--This allows:
--fast incremental updates
--easy “daily counts” reporting
--low storage overhead
-- =====================================================================

CREATE TABLE clickhouse_test_db.content_events_counter
(
    day Date,

    cluster_id LowCardinality(String),
    customer_id LowCardinality(String),

    context_site_id String,

    event_type LowCardinality(String),
    context_user_id String CODEC(ZSTD(3)),

    identifier String CODEC(ZSTD(3)),
    title String,

    daily_total UInt64
)
    ENGINE = SummingMergeTree(daily_total)
        ORDER BY (customer_id, cluster_id, context_user_id, day, identifier, title, event_type);


-- =====================================================================
-- Transforms raw events into daily activity counters.
-- For every event inserted into events, it computes:
--
-- day → start-of-day from utc_time
-- identifier → URL for pageview, content_identifier otherwise
-- title → page_title or content_title
--
-- Then groups by:
-- customer_id, cluster_id, context_user_id, day, identifier, title, event_type
--
-- And inserts:
--
-- count(*) AS daily_total
-- =====================================================================

CREATE MATERIALIZED VIEW content_events_counter_mv TO clickhouse_test_db.content_events_counter AS
SELECT customer_id,
       cluster_id,
       event_type,
       context_user_id,
       context_site_id,
       toStartOfDay(utc_time) as day,
       (CASE
           WHEN event_type = 'pageview' THEN url
           WHEN event_type = 'conversion' THEN conversion_name
           ELSE content_identifier
        END) as identifier,
       (CASE
           WHEN event_type = 'pageview' THEN page_title
           WHEN event_type = 'conversion' THEN conversion_name
           ELSE content_title
        END) as title,
       count(*) as daily_total
FROM clickhouse_test_db.events
GROUP BY customer_id, cluster_id, context_user_id, day, identifier, title, event_type, context_site_id;

-- =====================================================================
-- Stores the latest known conversion timestamp per user, but in aggregate function format.
-- Two aggregated fields:
--
-- conversion_last_time → last conversion event time
--
-- timestamp_last_time → last processed _timestamp inside content_presents_in_conversion
--
-- Why AggregatingMergeTree?
--
-- Because conversion_time_mv inserts aggregate states (maxState) and later merges them.
-- This table provides a “boundary” so that future incremental batches don’t reprocess old records.
-- =====================================================================

CREATE TABLE clickhouse_test_db.conversion_time
(
    cluster_id LowCardinality(String),
    customer_id LowCardinality(String),

    context_site_id String,
    context_user_id String CODEC(ZSTD(3)),

    conversion_last_time AggregateFunction( max, DateTime64(3, 'UTC')),
    timestamp_last_time  AggregateFunction( max, DateTime64(3, 'UTC'))
)
    ENGINE = AggregatingMergeTree
        PARTITION BY (customer_id)
        ORDER BY (customer_id, cluster_id, context_user_id);

-- =====================================================================
-- Tracks which content a user interacted with prior to a conversion and after the user's previous conversion
-- =====================================================================

CREATE TABLE clickhouse_test_db.content_presents_in_conversion
(
    day Date,
    last_timestamp DateTime64(3, 'UTC'),
    last_conversion_time DateTime64(3, 'UTC'),

    cluster_id LowCardinality(String),
    customer_id LowCardinality(String),

    context_site_id String,

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
-- Joins against conversion_time to get the previous batch’s last timestamps
-- Uses lag() to find previous conversion in current batch
--
-- Calculates:
-- previous_conversion_timestamp = max(previous_timestamp_current_batch, last_timestamp_previous_batch)
--
-- Filters conversions that are:
--
-- new (_timestamp > last_timestamp_previous_batch)
-- recent (_timestamp <= now())
--
-- This ensures incremental processing, no duplicates.
--
-- B) Join events leading to conversion
--
-- Matches events where:
--
-- e.utc_time < conversion.conversion_time
-- e.utc_time > conversion.conversion_last_time
-- event_type <> 'conversion'

-- Meaning:
--
-- Only consider events between the previous conversion timestamp and this conversion timestamp.
--
-- C) Group and insert
--
-- Inserts rows summarizing content presence before the conversion.
-- =====================================================================
CREATE MATERIALIZED VIEW content_presents_in_conversion_mv
    REFRESH EVERY 15 MINUTE APPEND TO clickhouse_test_db.content_presents_in_conversion AS
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
           lag(utc_time, 1) OVER (
               PARTITION BY context_user_id
               ORDER BY utc_time
               ) AS previous_utc_time_current_batch,
           (CASE WHEN previous_utc_time_current_batch > conversion_last_time THEN previous_utc_time_current_batch ELSE conversion_last_time END) as previous_conversion_time
    FROM clickhouse_test_db.events as e
             LEFT JOIN clickhouse_test_db.conversion_time on e.customer_id = conversion_time.customer_id AND e.cluster_id = conversion_time.cluster_id AND
                                                             e.context_user_id = conversion_time.context_user_id AND e.context_site_id = conversion_time.context_site_id
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
    context_site_id,
    conversion.conversion_name as conversion_name,
    count(*) AS conversion_count,
    max(conversion._timestamp) as last_timestamp,
    max(conversion.conversion_time) as last_conversion_time
FROM clickhouse_test_db.events e
         INNER JOIN conversion ON e.context_user_id = conversion.context_user_id AND
                                  e.utc_time < conversion.conversion_time AND
                                  e.utc_time > conversion.previous_conversion_time AND
                                  event_type <> 'conversion'
GROUP BY customer_id, cluster_id, identifier, title, event_type, context_user_id, conversion.conversion_name, day, context_site_id;


-- =====================================================================
-- Updates the conversion_time table using the output of content_presents_in_conversion. Every time new attribution rows are emitted
--
-- This ensures:
--
-- Next execution of the refreshable MV knows where the last batch ended
--
-- Prevents reprocessing or double counting
-- =====================================================================


CREATE MATERIALIZED VIEW conversion_time_mv TO clickhouse_test_db.conversion_time AS
SELECT customer_id,
       cluster_id,
       context_user_id,
       context_site_id,
       maxState(last_timestamp) as timestamp_last_time,
       maxState(last_conversion_time) as conversion_last_time
FROM clickhouse_test_db.content_presents_in_conversion
GROUP BY customer_id, cluster_id, context_user_id, context_site_id;

