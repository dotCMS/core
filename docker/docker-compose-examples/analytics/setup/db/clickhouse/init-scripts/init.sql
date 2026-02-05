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
USE clickhouse_test_db;
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
    context_site_auth String,
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

CREATE MATERIALIZED VIEW clickhouse_test_db.content_events_counter_mv TO clickhouse_test_db.content_events_counter AS
SELECT customer_id,
       cluster_id,
       event_type,
       context_user_id,
       context_site_id,
       toStartOfDay(utc_time) as day,
       (CASE
           WHEN event_type = 'pageview' THEN doc_path
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
    conversion_count UInt32,
    events_count UInt32
)
    ENGINE = SummingMergeTree
    PARTITION BY (customer_id)
    ORDER BY (customer_id, cluster_id, context_user_id, event_type, conversion_name, identifier, title, day);


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
CREATE MATERIALIZED VIEW clickhouse_test_db.content_presents_in_conversion_mv
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
    HAVING (_timestamp >=  last_timestamp_previous_batch AND _timestamp <= now())
)
SELECT
    toStartOfDay(conversion.conversion_time) as day,
    customer_id,
    cluster_id,
    (CASE WHEN event_type = 'pageview' THEN doc_path ELSE content_identifier END) as identifier,
    (CASE WHEN event_type = 'pageview' THEN page_title ELSE content_title END) as title,
    event_type,
    context_user_id,
    context_site_id,
    conversion.conversion_name as conversion_name,
    count(*) AS events_count,
    count(DISTINCT conversion_time) AS conversion_count,
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


CREATE MATERIALIZED VIEW clickhouse_test_db.conversion_time_mv TO clickhouse_test_db.conversion_time AS
SELECT customer_id,
       cluster_id,
       context_user_id,
       context_site_id,
       maxState(last_timestamp) as timestamp_last_time,
       maxState(last_conversion_time) as conversion_last_time
FROM clickhouse_test_db.content_presents_in_conversion
GROUP BY customer_id, cluster_id, context_user_id, context_site_id;


/* =====================================================================================================
   Session Engagement Pipeline
   =====================================================================================================

   This script defines the complete analytics pipeline used to compute GA4-style engagement metrics
   while remaining dotCMS-specific, scalable, and explainable. The pipeline is layered:

     events (raw immutable events)
        ↓ (real-time materialized view)
     session_states (incremental, mergeable session aggregates)
        ↓ (refreshable MV - finalize only recent sessions)
     session_facts (one row per session, dashboard-friendly)
        ↓ (refreshable MVs - daily rollups)
     engagement_daily + sessions_by_*_daily (small rollups for dashboards)
        ↓
     CubeJS reads from rollup tables and exposes metrics via API

    ┌───────────────────────────────────────────────┐
    │               Browser / Site                  │
    │                                               │
    │  pageview | content_click | conversion | ...  │
    │                                               │
    └──────────────────────┬────────────────────────┘
                           │
                           ▼
    ┌───────────────────────────────────────────────┐
    │           events  (MergeTree)                 │
    │                                               │
    │  - one row per event                          │
    │  - sessionid                                  │
    │  - user agent (device, browser)               │
    │  - language id                                │
    │  - timestamp                                  │
    │                                               │
    └──────────────────────┬────────────────────────┘
                           │  (incremental MV)
                           ▼
    ┌───────────────────────────────────────────────┐
    │        session_states (AggregatingMT)         │
    │                                               │
    │  - one row per session (event states)         │
    │  - min/max timestamp                          │
    │  - pageview count                             │
    │  - conversion count                           │
    │  - last-seen device / browser / language      │
    │                                               │
    └──────────────────────┬────────────────────────┘
                           │  (refreshable MV)
                           ▼
    ┌───────────────────────────────────────────────┐
    │        session_facts (ReplacingMT)            │
    │                                               │
    │  - one row per session                        │
    │  - duration_seconds                           │
    │  - total events                               │
    │  - engaged (true/false)                       │
    │  - device_category                            │
    │  - browser_family                             │
    │  - language_id                                │
    │                                               │
    └────────────┬────────────────────────┬─────────┘
                 │                        │
                 │                        │
                 ▼                        ▼
    ┌─────────────────────────┐   ┌────────────────────────────┐
    │  engagement_daily       │   │ sessions_by_*_daily        │
    │  (daily rollup)         │   │ (device / browser / lang)  │
    │                         │   │                            │
    │ - total_sessions        │   │ - total_sessions           │
    │ - engaged_sessions      │   │ - engaged_sessions         │
    │ - total_duration_*      │   │ - total_duration_engaged   │
    │                         │   │                            │
    └────────────┬────────────┘   └──────────────┬─────────────┘
                 │                               │
                 ▼                               ▼
    ┌────────────────────────────────────────────────────────┐
    │                        CubeJS                          │
    │                                                        │
    │  - KPIs (rates, averages)                              │
    │  - Trends (per day)                                    │
    │  - Breakdowns (device / browser / language)            │
    │                                                        │
    └───────────────────────────┬────────────────────────────┘
                                │
                                ▼
    ┌───────────────────────────────────────────────┐
    │              Angular Dashboard                │
    │                                               │
    │  - KPI cards (Today vs Yesterday)             │
    │  - Distribution widgets                       │
    │  - Trends                                     │
    │                                               │
    └───────────────────────────────────────────────┘

   Data rollup = To take many detailed rows and aggregate them into fewer, higher-level rows.

   Key tenant scope:
     All data is scoped by BOTH:
       - customer_id  (tenant/customer)
       - cluster_id   (environment/cluster: prod/stage/etc.)
     Session identity is:
       (customer_id, cluster_id, sessionid)

   Why this matters:
     - data isolation between customers
     - isolation between environments
     - allows filtering dashboards by customer+cluster with predictable performance

   -----------------------------------------------------------------------------------------------------
   Operational Notes
   -----------------------------------------------------------------------------------------------------

   • Refreshable MVs (REFRESH EVERY ...) are used to periodically recompute recent results. This gives
     near-real-time dashboards without paying heavy costs on every query.

   • "Late arriving events":
     We assume some events can arrive late (ingestion delays, client retries, network issues). Therefore:
       - session_states is updated incrementally in real-time
       - session_facts is re-finalized for sessions active in a rolling time window (e.g., last 72h)
       - daily rollups are recomputed for last N days (e.g., 90 days)

   • IMPORTANT:
     The heuristics for device and browser bucketing should be validated against your UA parser output.
     You can refine the classification later without changing the pipeline structure.

===================================================================================================== */

/* =====================================================================================================
   1) SESSION STATES (INCREMENTAL, MERGEABLE)
   =====================================================================================================

   Object: Table
   Name: clickhouse_test_db.session_states
   Engine: AggregatingMergeTree
   Updated by: session_states_mv (real-time MV on events inserts)

   Purpose:
     - Incrementally aggregates events into per-session "states" that can be merged efficiently. Initially,
       you get multiple partial rows for the same session key. However, at query time (or during background
       merges), ClickHouse merges them, so N partial rows become 1 correct session.
     - This enables:
         • real-time incremental ingestion
         • late arriving event correction (because new states merge in)
         • avoiding expensive GROUP BY on raw events repeatedly
         • Inserts that stay fast:
             • MV does simple aggregation on the batch
             • no global coordination
             • no locking
     - Instead of recomputing sessions from raw events every 15 mins, we continuously maintain aggregate
       states per session using an MV. This is extremely scalable in ClickHouse because:
         • inserts are append-only
         • aggregation states merge efficiently during background merges
         • your “session table” becomes tiny compared to raw events
     - The table ENGINE set to 'AggregatingMergeTree' means:
         • Rows are not final
         • Multiple partial rows for the same session will exist. ClickHouse will merge them later
         • Every column must be mergeable
         • This is why you never query session_states directly for dashboards
     - A mental analogy: Think of session_states as a shopping cart:
         • items get added in multiple steps
         • sometimes items arrive late
         • the cart is not “final” until checkout
            -> session_facts is the receipt.

   Key Grain (one logical session):
     (customer_id, cluster_id, sessionid, context_site_id)

   Column strategy:
     We store AggregateFunction(...) columns rather than finalized values.
     Example:
       - min_ts_state stores minState(_timestamp)
       - later, minMerge(min_ts_state) finalizes the min timestamp across merges

   Why using argMaxState for dimensions?
     - Session metadata can be emitted multiple times.
     - We want a deterministic "latest value" for device/browser/language.
     - argMaxState(value, _timestamp) keeps the value associated with the greatest timestamp.

===================================================================================================== */
CREATE TABLE clickhouse_test_db.session_states
(
    /* Tenant boundaries */
    customer_id String,        -- dotCMS customer / tenant identifier
    cluster_id  String,        -- environment/cluster identifier (prod/stage/etc.)
    /* We use argMax for context_site_id because session_states is mergeable, and ClickHouse needs an explicit rule
       to deterministically resolve the value when partial session data is merged. */
    context_site_id_state AggregateFunction(argMax, String, DateTime64(3, 'UTC')),
    context_site_id String,
    /* Session boundary */
    sessionid   String,        -- unique session identifier; all events with same sessionid belong together

    /* Session time window (mergeable aggregate states) */
    min_ts_state AggregateFunction(min, DateTime64(3, 'UTC')), -- earliest event timestamp seen in session
    max_ts_state AggregateFunction(max, DateTime64(3, 'UTC')), -- latest  event timestamp seen in session

    /* Event counters (mergeable) */
    total_events_state AggregateFunction(count),               -- total events in session
    pageviews_state    AggregateFunction(countIf, UInt8),      -- number of pageview events in session
    conversions_state  AggregateFunction(countIf, UInt8),      -- number of conversion events in session

    user_agent_state AggregateFunction(argMax, String, DateTime64(3, 'UTC')),

    /* Dimension "last known value" states (mergeable) */
    device_category_state AggregateFunction(argMax, String, DateTime64(3, 'UTC')),
    -- last-seen device category label for the session (Desktop/Mobile/Tablet/Other)
    -- derived from UA fields; stored as state so that late events can update the final value deterministically.

    browser_family_state  AggregateFunction(argMax, String, DateTime64(3, 'UTC')),
    -- last-seen browser family bucket (Chrome/Safari/Firefox/Edge/Other)

    language_id_state     AggregateFunction(argMax, String, DateTime64(3, 'UTC'))
    -- last-seen dotCMS language id (as String), defaulting to '0' if unknown
)
    /* Why this engine is mandatory:
        -> You are storing aggregate states
        -> You rely on merge correctness
        -> Without replication, different replicas would compute different session states */
    /*ENGINE = ReplicatedAggregatingMergeTree(
            '/clickhouse/tables/{shard}/session_states',
            '{replica}'
             )*/
    ENGINE = AggregatingMergeTree
    /* Partitioning note:
       We partition by a hash of (customer, cluster) to spread writes and merges.
       This avoids a single giant partition for big tenants and keeps merges parallelizable. */
    PARTITION BY sipHash64(customer_id, cluster_id) % 64
    /* Note for Sort key:
       ORDER BY includes tenant + session to keep session states physically clustered for merges/finalization.
       This also ensures stable grouping keys for session_facts refresh queries. */
    ORDER BY (
              customer_id,
              cluster_id,
              sessionid,
              context_site_id);


/* =====================================================================================================
   Device categorization
   ===================================================================================================== */
/*
   Object: Table
   Name: clickhouse_test_db.device_category_map
   Engine: MergeTree
   Used by: session_states_mv (during ingestion)

   This is a tiny dimension table that maps normalized UA parser outputs into a device category bucket. This
   table is meant to classify using normalized keys we already trust:

    - parsed_ua_device_family (dev_key)
    - parsed_ua_os_family (os_key)

   Those fields come from the UA parser and are typically already tokenized into a clean family name (e.g., iPhone,
   iPad, Windows, Mac OS X, etc.). After lowerUTF8(...), those become stable join keys. Think of this table as a
   trusted translation dictionary:

        • Your UA parser gives you tokens like “iphone”, “ipad”, “windows”.
        • This table translates those tokens into friendly categories like “Mobile”, “Tablet”, “Desktop”.
        • It’s clean, deterministic, and fast — like looking up a word in a dictionary.
 */
CREATE TABLE IF NOT EXISTS clickhouse_test_db.device_category_map
(
    -- What we match on (pick one: device_family, os_family, ua_family, etc.)
    -- For performance and simplicity, we store normalized lowercase keys.
    match_key String,          -- e.g. 'iphone', 'android', 'ipad',   'smarttv',  'windows'
    device_category String     -- e.g. 'Mobile',            'Tablet', 'Smart TV', 'Desktop', 'Other'
)
    ENGINE = MergeTree
    ORDER BY match_key;

/* Initial preload of the most common devices inside their respective categories */
INSERT INTO clickhouse_test_db.device_category_map VALUES
    ('smarttv', 'Smart TV'),
    ('smart tv', 'Smart TV'),
    ('tizen', 'Smart TV'),
    ('webos', 'Smart TV'),
    ('hbbtv', 'Smart TV'),
    ('roku', 'Smart TV'),
    ('appletv', 'Smart TV'),
    ('apple tv', 'Smart TV'),

    ('ipad', 'Tablet'),
    ('tablet', 'Tablet'),
    ('kindle', 'Tablet'),

    ('iphone', 'Mobile'),
    ('ios', 'Mobile'),
    ('android', 'Mobile'),
    ('pixel', 'Mobile'),
    ('samsung', 'Mobile'),

    ('windows', 'Desktop'),
    ('mac', 'Desktop'),
    ('macos', 'Desktop'),
    ('mac os x', 'Desktop'),
    ('os x', 'Desktop'),
    ('linux', 'Desktop');

/*
   Object: Table
   Name: clickhouse_test_db.device_category_fallback_rules
   Engine: MergeTree
   Used by: session_facts_rmv (during session finalization, not ingestion)

   Device fallback rules are set for when the base classification is too generic (“Desktop/Other”) and you want
   a last-resort rescue by scanning the raw UA string. This table is a small set of priority-ordered fallback
   heuristics used only when the base classification is too generic, and you still have the raw user-agent string
   available. This is valuable when:

    - the UA parser doesn’t recognize the device family cleanly; i.e., it fails to identify Smart TVs/consoles cleanly
    - or collapses unknown stuff into generic families
    - or you see real-world UAs where the relevant signal is only in the raw string
 */
CREATE TABLE IF NOT EXISTS clickhouse_test_db.device_category_fallback_rules
(
    priority UInt16,        -- lower = evaluated first
    category String,        -- e.g. 'Smart TV', 'Tablet', 'Mobile'
    patterns Array(String)  -- case-insensitive substrings
)
    ENGINE = MergeTree
    ORDER BY priority;

/* ⚠️ Keep this table small. Think “last-resort heuristics”, not full UA parsing. */
INSERT INTO clickhouse_test_db.device_category_fallback_rules VALUES
    (10, 'Smart TV', ['smarttv','smart tv','tizen','webos','hbbtv','roku','appletv']),
    (20, 'Tablet',   ['ipad','tablet','kindle']),
    (30, 'Mobile',   ['iphone','android','mobile']),
    (90, 'Desktop',  ['windows','macintosh','linux']),
    (15, 'Game Console', ['playstation','xbox','nintendo','switch']);


/* =====================================================================================================
   Browser categorization
   ===================================================================================================== */

/*
   Object: Table
   Name: clickhouse_test_db.browser_family_map
   Engine: MergeTree
   Used by: session_states_mv (during ingestion)

   This is a small lookup table that maps a normalized UA parser “browser family” token into a stable, dashboard-friendly
   browser bucket. This table is meant to classify using normalized keys we already trust:

    - parsed_ua_ua_family (browser_key)

   Those fields come from the UA parser and are typically already tokenized into a clean family name (e.g., Safari,
   Chrome, Firefox, etc.). After lowerUTF8(...), those become stable join keys.
 */
CREATE TABLE IF NOT EXISTS clickhouse_test_db.browser_family_map
(
    match_key String,          -- normalized lowercase
    browser_family String      -- 'Chrome','Safari','Firefox','Edge','Other'
)
    ENGINE = MergeTree
    ORDER BY match_key;

/* Initial preload of the most common browsers inside their respective categories */
INSERT INTO clickhouse_test_db.browser_family_map VALUES
    ('chrome', 'Chrome'),
    ('chromium', 'Chrome'),
    ('headlesschrome', 'Chrome'),
    ('crios', 'Chrome'),
    ('safari', 'Safari'),
    ('mobile safari', 'Safari'),
    ('firefox', 'Firefox'),
    ('fxios', 'Firefox'),
    ('edge', 'Edge'),
    ('microsoft edge', 'Edge'),
    ('edg', 'Edge');

/*
   Object: Table
   Name: clickhouse_test_db.browser_family_fallback_rules
   Engine: MergeTree
   Used by: session_facts_rmv (during session finalization)

   Browser fallback rules. There are used only in `session_facts_rmv`, not during ingestion. They are
   applied once, at session finalization time. This table defines priority-ordered heuristic rules to
   classify a browser by scanning the raw UA string, but only when the base classification is too generic.
   Sometimes the browser family token from the UA parser is unhelpful:

    - it’s missing
    - it’s normalized into something too generic
    - or it maps to Other even though the UA string clearly contains a recognizable signature

   So this table lets you “rescue” classification only when needed, without making ingestion heavy.
 */
CREATE TABLE IF NOT EXISTS clickhouse_test_db.browser_family_fallback_rules
(
    priority UInt16,
    category String,
    patterns Array(String)
)
    ENGINE = MergeTree
    ORDER BY priority;

/* ⚠️ Keep this table small. Think “last-resort heuristics”, not full UA parsing. */
INSERT INTO clickhouse_test_db.browser_family_fallback_rules VALUES
    (10, 'Edge',    ['edg','edge']),
    (20, 'Chrome',  ['headlesschrome/','chrome','chromium','crios']),
    (30, 'Safari',  ['safari']),
    (40, 'Firefox', ['firefox']),
    (90, 'Other',   ['opera','ucbrowser']);


/* =====================================================================================================
   1.1) Real-time MV: events → session_states
   =====================================================================================================

   Object: Materialized View (incremental, real-time)
   Name: clickhouse_test_db.session_states_mv
   Pattern: CREATE MATERIALIZED VIEW ... TO session_states AS SELECT ...
   Source: clickhouse_test_db.events
   Target: clickhouse_test_db.session_states

   Purpose:
     - Runs on every insert to events table.
     - It takes only the NEWLY INSERTED BATCH and turns it into mergeable aggregate-state rows that get
       appended into session_states.
     - Writes those states into session_states where they will be merged with prior states.
     - This MV is the engine that makes the whole pipeline scalable. Without it, you’d be forced to compute
       sessions by scanning events with a big GROUP BY whenever you need session metrics (slow and expensive
       at scale).

   Key guarantee:
     - Correctness over time: if more events arrive for a session later, the session_states row merges in.

   Device bucketing:
     This is a heuristic based on the UA parsing fields you have:
       - Tablet if device family suggests ipad/tablet
       - Mobile if OS suggests iOS/Android or device hints phone/android
       - Desktop otherwise
     You can refine this as you validate UA outputs.

   Browser bucketing:
     - normalizes UA family into a stable set of display-friendly buckets
     - unrecognized → Other

   Language id:
     - uses `userlanguage`
     - replace this expression if language id comes from a different field in your tracking payload

   - Automatically replicated
   - No engine change required
   - They write into replicated tables → safe

===================================================================================================== */
CREATE MATERIALIZED VIEW clickhouse_test_db.session_states_mv
            TO clickhouse_test_db.session_states
AS
/* -------------------------------------------------------------------------------------------------
   Tables-only enrichment:
     - device_category_map and browser_family_map are tiny dimension tables in ClickHouse
     - We LEFT JOIN to them using normalized lowercase keys
     - We keep the same “last known value” strategy with argMaxState(..., _timestamp)
------------------------------------------------------------------------------------------------- */
WITH
    lowerUTF8(parsed_ua_device_family) AS device_key,
    lowerUTF8(parsed_ua_os_family)     AS os_key,
    lowerUTF8(parsed_ua_ua_family)     AS browser_key,

    /* Prefer device_family mapping, else os_family mapping, else Desktop */
    coalesce(nullIf(d_dev.device_category, ''), nullIf(d_os.device_category, ''), 'Desktop') AS device_category,

    /* Prefer browser mapping, else Other */
    coalesce(nullIf(b_map.browser_family, ''), 'Other') AS browser_family,

    nullIf(userlanguage, '') AS language_id
SELECT
    e.customer_id,
    e.cluster_id,
    e.context_site_id,
    e.sessionid,

    /* Session time window states */
    minState(e._timestamp) AS min_ts_state,
    maxState(e._timestamp) AS max_ts_state,

    /* Session-scoped ownership */
    argMaxState(e.context_site_id, e._timestamp) AS context_site_id_state,

    /* Event counters states */
    countState() AS total_events_state,
    countIfState(e.event_type = 'pageview')   AS pageviews_state,
    countIfState(e.event_type = 'conversion') AS conversions_state,

    /* Session UA stored as state (needed for fallback later) */
    argMaxState(e.user_agent, e._timestamp) AS user_agent_state,

    /* "last seen" dimension states (tables-only values) */
    argMaxState(device_category, e._timestamp) AS device_category_state,
    argMaxState(browser_family, e._timestamp)  AS browser_family_state,
    argMaxState(coalesce(language_id, '0'), e._timestamp) AS language_id_state
FROM clickhouse_test_db.events AS e
     /* Device mapping via table */
     LEFT JOIN clickhouse_test_db.device_category_map AS d_dev
        ON d_dev.match_key = device_key
     LEFT JOIN clickhouse_test_db.device_category_map AS d_os
        ON d_os.match_key = os_key
     /* Browser mapping via table */
     LEFT JOIN clickhouse_test_db.browser_family_map AS b_map
        ON b_map.match_key = browser_key
WHERE e.sessionid != ''
  AND e.customer_id != ''
  AND e.cluster_id != ''
  AND e.context_site_id != ''
GROUP BY (
    e.customer_id,
    e.cluster_id,
    e.context_site_id,
    e.sessionid);


/* =====================================================================================================
   2) SESSION FACTS (FINALIZED SNAPSHOT)
   =====================================================================================================

   Object: Table
   Name: clickhouse_test_db.session_facts
   Engine: ReplacingMergeTree(updated_at)
   Written by: session_facts_rmv (refreshable MV that finalizes session_states)
   Role in pipeline: “finalized session snapshot” — one row per session, query-friendly

   Purpose:
     - This is the first table in the pipeline that is meant to be read directly by downstream rollups and
       dashboards.Stores one finalized row per session with plain scalar values (per session aggregates).
     - This table represents “what happened in a session”, including duration, event counts, and the engaged
       flag. This gives you a stable, query-friendly “session dimension” with everything you need for engagement
       metrics.
     - This is the "source of truth" for session-level analytics and is the input for daily rollups.

   Why using ReplacingMergeTree?
     - Because we periodically "re-finalize" the same sessions as late events arrive.
     - Each refresh writes a newer version (updated_at).
     - ReplacingMergeTree keeps only the latest version during merges.

   IMPORTANT:
     - When verifying correctness, do NOT rely on FINAL in production queries. Use FINAL only for
       debugging/sanity checks.
     - For normal queries, the latest version will be used by merges/reads over time.

===================================================================================================== */
CREATE TABLE clickhouse_test_db.session_facts
(
    /* Tenant scope */
    customer_id String,
    cluster_id  String,
    context_site_id String,
    /* Session identity */
    sessionid   String,

    /* Finalized session times */
    session_start DateTime64(3, 'UTC'), -- earliest event timestamp
    session_end   DateTime64(3, 'UTC'), -- latest event timestamp
    duration_seconds UInt32,            -- session_end - session_start (seconds)

    /* Finalized counters */
    total_events UInt32,                -- total events in session
    pageviews    UInt32,                -- pageview events
    conversions  UInt32,                -- conversion events

    /* Engagement flag (GA4-style) */
    engaged UInt8,                      -- 1 if engaged, else 0

    /* Finalized dimensions */
    device_category String,             -- Desktop/Mobile/Tablet/Other
    browser_family  String,             -- Chrome/Safari/Firefox/Edge/Other
    language_id     String,             -- dotCMS language id as String ('0' unknown)

    /* Row version timestamp for ReplacingMergeTree */
    updated_at DateTime('UTC')
)
    /* Why this matters:
        -> Late events cause re-finalization
        -> Multiple versions of the same session will exist temporarily
        -> Replication guarantees:
            -> all replicas converge to the same “latest” row
            -> dashboards don’t disagree depending on which replica is queried */
    /*ENGINE = ReplicatedReplacingMergeTree(
            '/clickhouse/tables/{shard}/session_facts',
            '{replica}',
            updated_at
             )*/
    ENGINE = ReplacingMergeTree
    /* Partition by month of session_start. Keeps partitions time-bounded and supports TTL strategies
       later if desired. */
    PARTITION BY toYYYYMM(toDate(session_start))
    /* Sort key includes session identity for deterministic replacement. */
    ORDER BY (
              customer_id,
              cluster_id,
              context_site_id,
              sessionid);


/* =====================================================================================================
   2.1) Refreshable MV: session_states → session_facts
   =====================================================================================================

   Object: Refreshable Materialized View (RMV)
   Name: clickhouse_test_db.session_facts_rmv
   Type: REFRESH EVERY 15 MINUTE … TO session_facts
   Source: clickhouse_test_db.session_states
   Target: clickhouse_test_db.session_facts

   Purpose:
     - This RMV is the finalizer: it takes “mergeable session state fragments” from session_states and
       periodically produces (or re-produces) the latest finalized snapshot of each session in session_facts.
       It’s not triggered by inserts; it runs on a schedule.
     - Applies the engagement rules to compute engaged = 1 (engaged) or 0 (not engaged).
     - Recompute “recent sessions” every 15 minutes.
     - Important design choice: do not recompute “all history” every 15 minutes. Instead, recompute a
       sliding window that covers late-arriving events + sessions that are still “open”. A very typical
       window is 48 hours (tune as needed). We're setting 72 hours back for now.

   Rolling window (start_cutoff):
     - We only finalize sessions whose session_end is in the last 72 hours, which can be increased as long as
       the REFRESH EVERY value is increased as well.
     - This bounds cost and still corrects for late arriving events.
     - For older late events, a nightly "re-finalize last 30 days" job can be added in the future.

   Engagement rules (session is engaged if any):
     - duration > 10 seconds
     - pageviews >= 2
     - conversions >= 1

===================================================================================================== */
CREATE MATERIALIZED VIEW clickhouse_test_db.session_facts_rmv
    REFRESH EVERY 15 MINUTE
    TO clickhouse_test_db.session_facts
AS
WITH
    (now() - INTERVAL 72 HOUR) AS start_cutoff,

    /* Load fallback rules for device and browser once (small arrays, evaluated per session row) */
    (
        SELECT arraySort(x -> x.1, groupArray((priority, category, patterns)))
        FROM clickhouse_test_db.device_category_fallback_rules
    ) AS device_rules_sorted,

    (
        SELECT arraySort(x -> x.1, groupArray((priority, category, patterns)))
        FROM clickhouse_test_db.browser_family_fallback_rules
    ) AS browser_rules_sorted
SELECT
    customer_id,
    cluster_id,
    context_site_id,
    sessionid,

    session_start,
    session_end,
    duration_seconds,

    total_events,
    pageviews,
    conversions,

    engaged,

    /* FINAL device_category (map-first, UA-pattern fallback second)
     *
     * - device_category_base is derived from session_states_mv using table joins:
     *     device_category_map (device_family/os_family) → category, else 'Desktop'
     * - We only apply fallback when base is generic ('Desktop' or 'Other') AND we have a UA string.
     * - Fallback scans ordered rules (priority asc) and returns the first match.
     * - If no fallback matches, keep base.
     */
    if ((device_category_base IN ('Desktop', 'Other')) AND ua_l != '',
        coalesce(
                nullIf(
                        ifNull(
                                arrayFirst(r -> multiSearchAnyCaseInsensitive(ua_l, r.3) > 0, device_rules_sorted).2,
                                ''
                        ),
                        ''
                ), device_category_base),
        -- else
        device_category_base
    ) AS device_category,

    /* FINAL browser_family (map-first, UA-pattern fallback second)
     *
     * - browser_family_base is derived from session_states_mv using table joins:
     *     browser_family_map (ua_family) → family, else 'Other'
     * - We only apply fallback when base resolves to 'Other' AND UA string is available.
     * - Fallback is priority ordered and returns the first match.
     * - If no fallback matches, keep base.
     */
    if ((browser_family_base = 'Other') AND ua_l != '',
        coalesce(
                nullIf(
                        ifNull(
                                arrayFirst(r -> multiSearchAnyCaseInsensitive(ua_l, r.3) > 0, browser_rules_sorted).2,
                                ''
                        ),
                        ''
                ), browser_family_base),
        -- else
        browser_family_base
    ) AS browser_family,

    language_id,

    now() AS updated_at
FROM
    (
        /* Aggregate session_states into finalized scalar columns */
        SELECT
            customer_id,
            cluster_id,
            context_site_id,
            sessionid,

            minMerge(min_ts_state) AS session_start,
            maxMerge(max_ts_state) AS session_end,

            toUInt32(greatest(0, dateDiff('second', session_start, session_end))) AS duration_seconds,

            toUInt32(countMerge(total_events_state)) AS total_events,
            toUInt32(countIfMerge(pageviews_state))  AS pageviews,
            toUInt32(countIfMerge(conversions_state)) AS conversions,

            /* Business rules that determines whether a session is flagged as 'engaged' or not */
            toUInt8(
                    -- Sessions that last at least 10 seconds
                    (dateDiff('second', session_start, session_end) > 10)
                    -- Sessions that trigger at least 2 events of type 'pageview'
                    OR (countIfMerge(pageviews_state) >= 2)
                    -- Sessions that trigger at least 1 event of type 'conversion'
                    OR (countIfMerge(conversions_state) >= 1)
            ) AS engaged,

            /* Base values now come from table-join mapping in session_states_mv */
            coalesce(nullIf(argMaxMerge(device_category_state), ''), 'Desktop') AS device_category_base,
            coalesce(nullIf(argMaxMerge(browser_family_state), ''), 'Other')    AS browser_family_base,

            /* UA for fallback matching */
            lowerUTF8(argMaxMerge(user_agent_state)) AS ua_l,

            argMaxMerge(language_id_state) AS language_id
        FROM clickhouse_test_db.session_states
        GROUP BY (
                  customer_id,
                  cluster_id,
                  context_site_id,
                  sessionid)
        HAVING session_end >= start_cutoff
    ) s;


/* =====================================================================================================
   3) DAILY ENGAGEMENT ROLLUP (DASHBOARD READY)
   =====================================================================================================

   Object: Table
   Name: clickhouse_test_db.engagement_daily
   Engine: ReplacingMergeTree(updated_at)
   Written by: engagement_daily_rmv (refreshable MV)
   Read by: CubeJS + dashboard KPI cards / trend charts

   Purpose:
     - Stores daily aggregates derived from session_facts. It's intentionally small and dashboard-friendly.
     - This table is meant to serve the dashboard fast (engagement rate, trend, average interactions,
       average session time, conversion rate) without scanning sessions.
     - This is the primary table queried by KPI cards and trend charts.
     - It avoids scanning session_facts for common dashboards.

   Why store daily sums?
     - For any time range, correct results are computed as:
         sum(numerator) / sum(denominator)
       NOT average of daily rates.
     - Having daily sums allows accurate ratios for arbitrary date ranges. Dashboards frequently query arbitrary
       date ranges (e.g., “last 28 days”, “Jan 10–Feb 2”). If you store daily rates (like daily engagement%),
       then combining days correctly becomes tricky (you must weight by denominators). Instead, you store daily
       numerators and denominators, so any date-range query can compute accurate rates.

   Grain:
     (customer_id, cluster_id, context_site_id, day)

   Metrics included:
     - total_sessions
     - engaged_sessions
     - engaged_conversion_sessions
     - total_events_all
     - total_duration_all
     - total_events_engaged
     - total_duration_engaged

===================================================================================================== */
CREATE TABLE clickhouse_test_db.engagement_daily
(
    customer_id String,
    cluster_id  String,
    context_site_id String,
    day Date,

    total_sessions UInt64,                    -- count of all sessions
    engaged_sessions UInt64,                  -- count of engaged sessions
    engaged_conversion_sessions UInt64,       -- engaged sessions that include >=1 conversion

    total_events_all UInt64,                  -- sum(total_events) across all sessions
    total_duration_all UInt64,                -- sum(duration_seconds) across all sessions

    total_events_engaged UInt64,              -- sum(total_events) across engaged sessions only
    total_duration_engaged UInt64,            -- sum(duration_seconds) across engaged sessions only

    updated_at DateTime('UTC')
)
    /* Why replicate rollups?
        -> Refreshable MVs rewrite rows
        -> Each replica must agree on the final row version
        -> Otherwise, engagement rates can differ between replicas */
    /*ENGINE = ReplicatedReplacingMergeTree(
            '/clickhouse/tables/{shard}/engagement_daily',
            '{replica}',
            updated_at
             )*/
    ENGINE = ReplacingMergeTree
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, cluster_id, context_site_id, day);


/* =====================================================================================================
   3.1) Refreshable MV: session_facts → engagement_daily
   =====================================================================================================

   Object: Refreshable Materialized View (RMV)
   Name: clickhouse_test_db.engagement_daily_rmv
   Type: REFRESH EVERY 15 MINUTE … TO engagement_daily
   Source: clickhouse_test_db.session_facts
   Target: clickhouse_test_db.engagement_daily

   Purpose:
     - Periodically recompute daily rollups for recent days from the finalized session snapshot table
       (session_facts) and writes the results into the dashboard-ready rollup table (engagement_daily).
     - Here we typically roll up a wider window (e.g., last 90 days) because session_facts is already
       small and cheap to scan, we can afford to recompute aggregates for many days back — like 90 days —
       every time the MV refreshes (much smaller than raw events).
     - Cheap because session_facts is far smaller than events.

   Start window (start_day):
     - Recompute last 90 days by default.
     - Tune based on retention needs + cost tolerance.
     - This makes daily rollups correct even if session_facts rows are updated by late events.

===================================================================================================== */
CREATE MATERIALIZED VIEW clickhouse_test_db.engagement_daily_rmv
    REFRESH EVERY 15 MINUTE
    TO clickhouse_test_db.engagement_daily
AS
WITH (today() - 90) AS start_day
SELECT
    customer_id,
    cluster_id,
    context_site_id,
    toDate(session_start) AS day,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    countIf(engaged = 1 AND conversions >= 1) AS engaged_conversion_sessions,

    sum(total_events) AS total_events_all,
    sum(duration_seconds) AS total_duration_all,

    sumIf(total_events, engaged = 1) AS total_events_engaged,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged,

    now() AS updated_at
FROM clickhouse_test_db.session_facts
WHERE toDate(session_start) >= start_day
GROUP BY (
          customer_id,
          cluster_id,
          context_site_id,
          day);


/* =====================================================================================================
   4) DIMENSION ROLLUPS (DEVICE / BROWSER / LANGUAGE)
   =====================================================================================================

   These rollups power the "distribution" widgets:
     - Sessions by device
     - Sessions by browser
     - Sessions by language

   For each bucket (e.g., Desktop):
     - show engaged_sessions (absolute count)
     - show engaged% within bucket = engaged_sessions / total_sessions
     - show avg engaged time = total_duration_engaged / engaged_sessions

   Therefore, each rollup stores BOTH:
     - total_sessions (all sessions in bucket)
     - engaged_sessions (subset)
     - total_duration_engaged_seconds (duration sum for engaged subset)

   Grain:
     (customer_id, cluster_id, context_site_id, day, {bucket_value})
     {bucket_value} = device, browser, or language ID

===================================================================================================== */

/* -----------------------------------------------------------------------------------------------------
   4.1) Sessions by Device (daily)
----------------------------------------------------------------------------------------------------- */

/*
   Object: Table
   Name: clickhouse_test_db.sessions_by_device_daily
   Engine: ReplacingMergeTree(updated_at)
   Written by: sessions_by_device_daily_rmv
   Role: daily rollup for the “Sessions by Device” distribution widget

   Mental model:
   Think of this table as a daily per-site scoreboard by device category.
   Each row stores raw counts (total sessions, engaged sessions, engaged time)
   for one device bucket on one day. The table is periodically rewritten from
   session_facts to correct late-arriving data. Dashboards compute percentages
   and averages from these raw totals instead of storing precomputed rates.
*/
CREATE TABLE clickhouse_test_db.sessions_by_device_daily
(
    customer_id String,
    cluster_id  String,
    context_site_id String,
    day Date,

    device_category String,                  -- Desktop/Mobile/Tablet/Other

    total_sessions UInt64,                   -- ALL sessions for this device_category
    engaged_sessions UInt64,                 -- Engaged sessions for this device_category
    total_duration_engaged_seconds UInt64,   -- Sum(duration_seconds) for engaged sessions only

    updated_at DateTime('UTC')
)
    /*ENGINE = ReplicatedReplacingMergeTree(
            '/clickhouse/tables/{shard}/sessions_by_device_daily',
            '{replica}',
            updated_at
             )*/
    ENGINE = ReplacingMergeTree
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, cluster_id, context_site_id, day, device_category);

/*
   Object: RMV
   Name: clickhouse_test_db.sessions_by_device_daily_rmv
   Type: REFRESH EVERY 15 MINUTE … TO sessions_by_device_daily
   Source: session_facts
   Target: sessions_by_device_daily

   Recomputes a bounded window (this script uses last 90 days via start_day)
 */
CREATE MATERIALIZED VIEW clickhouse_test_db.sessions_by_device_daily_rmv
    REFRESH EVERY 15 MINUTE
    TO clickhouse_test_db.sessions_by_device_daily
AS
WITH (today() - 90) AS start_day
SELECT
    customer_id,
    cluster_id,
    context_site_id,
    toDate(session_start) AS day,
    device_category,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged_seconds,

    now() AS updated_at
FROM clickhouse_test_db.session_facts
WHERE toDate(session_start) >= start_day
GROUP BY (
          customer_id,
          cluster_id,
          context_site_id,
          day,
          device_category);


/* -----------------------------------------------------------------------------------------------------
   4.2) Sessions by Browser (daily)
----------------------------------------------------------------------------------------------------- */

/*
   Object: Table
   Name: clickhouse_test_db.sessions_by_browser_daily
   Engine: ReplacingMergeTree(updated_at)
   Written by: sessions_by_browser_daily_rmv
   Role: daily rollup for the “Sessions by Browser” distribution widget

   Mental model:
   Think of this table as a daily per-site scoreboard by browser.
   Each row stores raw counts (total sessions, engaged sessions, engaged time)
   for one browser bucket on one day. The table is periodically rewritten from
   session_facts to correct late-arriving data. Dashboards compute percentages
   and averages from these raw totals instead of storing precomputed rates.
*/
CREATE TABLE clickhouse_test_db.sessions_by_browser_daily
(
    customer_id String,
    cluster_id  String,
    context_site_id String,
    day Date,

    browser_family String,                   -- Chrome/Safari/Firefox/Edge/Other

    total_sessions UInt64,
    engaged_sessions UInt64,
    total_duration_engaged_seconds UInt64,

    updated_at DateTime('UTC')
)
    /*ENGINE = ReplicatedReplacingMergeTree(
            '/clickhouse/tables/{shard}/sessions_by_browser_daily',
            '{replica}',
            updated_at
             )*/
    ENGINE = ReplacingMergeTree
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, cluster_id, context_site_id, day, browser_family);

/*
   Object: RMV
   Name: clickhouse_test_db.sessions_by_browser_daily_rmv
   Type: REFRESH EVERY 15 MINUTE … TO sessions_by_browser_daily

   Same pattern as device: bounded window (e.g., last 90 days)
 */
CREATE MATERIALIZED VIEW clickhouse_test_db.sessions_by_browser_daily_rmv
    REFRESH EVERY 15 MINUTE
    TO clickhouse_test_db.sessions_by_browser_daily
AS
WITH (today() - 90) AS start_day
SELECT
    customer_id,
    cluster_id,
    context_site_id,
    toDate(session_start) AS day,
    browser_family,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged_seconds,

    now() AS updated_at
FROM clickhouse_test_db.session_facts
WHERE toDate(session_start) >= start_day
GROUP BY (
          customer_id,
          cluster_id,
          context_site_id,
          day,
          browser_family);


/* -----------------------------------------------------------------------------------------------------
   4.3) Sessions by Language (daily)
----------------------------------------------------------------------------------------------------- */

/*
   Object: Table
   Name: clickhouse_test_db.sessions_by_language_daily
   Engine: ReplacingMergeTree(updated_at)
   Written by: sessions_by_language_daily_rmv
   Role: daily rollup for “Sessions by Language” distribution widget

   Mental model:
   Think of this table as a daily per-site scoreboard by language.
   Each row stores raw counts (total sessions, engaged sessions, engaged time)
   for one language bucket on one day. The table is periodically rewritten from
   session_facts to correct late-arriving data. Dashboards compute percentages
   and averages from these raw totals instead of storing precomputed rates.
*/
CREATE TABLE clickhouse_test_db.sessions_by_language_daily
(
    customer_id String,
    cluster_id  String,
    context_site_id String,
    day Date,

    language_id String,                      -- dotCMS language id as String ('0' unknown)

    total_sessions UInt64,
    engaged_sessions UInt64,
    total_duration_engaged_seconds UInt64,

    updated_at DateTime('UTC')
)
    /*ENGINE = ReplicatedReplacingMergeTree(
            '/clickhouse/tables/{shard}/sessions_by_language_daily',
            '{replica}',
            updated_at
             )*/
    ENGINE = ReplacingMergeTree
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, cluster_id, context_site_id, day, language_id);

/*
   Object: RMV
   Name: clickhouse_test_db.sessions_by_language_daily_rmv
   Type: REFRESH EVERY 15 MINUTE … TO sessions_by_language_daily

   Same recompute model: scan session_facts for last N days (90)
 */
CREATE MATERIALIZED VIEW clickhouse_test_db.sessions_by_language_daily_rmv
    REFRESH EVERY 15 MINUTE
    TO clickhouse_test_db.sessions_by_language_daily
AS
WITH (today() - 90) AS start_day
SELECT
    customer_id,
    cluster_id,
    context_site_id,
    toDate(session_start) AS day,
    language_id,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged_seconds,

    now() AS updated_at
FROM clickhouse_test_db.session_facts
WHERE toDate(session_start) >= start_day
GROUP BY (
          customer_id,
          cluster_id,
          context_site_id,
          day,
          language_id);
