/* =====================================================================================================
   dotCMS Content Analytics
   Session Engagement Pipeline
   =====================================================================================================

   OVERVIEW
   -----------------------------------------------------------------------------------------------------

   This script defines the complete session-engagement pipeline used to compute GA4-style engagement
   metrics for dotCMS Content Analytics while keeping the architecture scalable, explicit, and easy to
   reason about.

   This version assumes:

     1) session_id is a REAL browser session identifier
        - sessions are short-lived
        - sessions rotate normally
        - session_id is NOT a long-lived user identity

     2) late-arriving events are still possible
        - network retries
        - collector delays
        - buffering
        - eventual ingestion into ClickHouse

     3) ALL historical session data must be kept
        - TTL must be defined when ready for production
        - no dropping older sessions
        - the late-event window is only for recomputation

     4) downstream consumers will use RAW SQL
        - no semantic modeling layer on top of ClickHouse
        - the service layer / API can directly query the roll-up tables

   -----------------------------------------------------------------------------------------------------
   HIGH-LEVEL PIPELINE
   -----------------------------------------------------------------------------------------------------

     events (raw immutable event stream)
        ↓  real-time MV
     session_states (incremental mergeable session states)
        ↓  refreshable MV APPEND
     session_facts (full historical session table, versioned)
        ↓  refreshable MV
     session_facts_latest (latest effective row per session)
        ↓  refreshable MVs
     engagement_daily + sessions_by_*_daily
        ↓
     raw SQL queries / API / Angular dashboard

   -----------------------------------------------------------------------------------------------------
   WHY THIS SHAPE?
   -----------------------------------------------------------------------------------------------------

   We want to solve two competing needs:

     A) Keep ALL session history forever
     B) Still reprocess recent sessions to absorb late-arriving events

   A naive design would overwrite `session_facts` with only the recent sliding window, but that would make
   older sessions disappear from the table.

   Instead, this design works like this:

     - `session_states` continuously accumulates mergeable event states
     - `session_facts_rmv` recalculates ONLY recent sessions and APPENDS a newer version of the row into
       `session_facts`
     - `session_facts` therefore becomes a versioned historical store
     - `session_facts_latest_rmv` deduplicates `session_facts` into exactly one latest row per session key
     - all dashboard roll-ups read from `session_facts_latest`, so they do not need the `FINAL` keyword in
       the `SELECT` queries used to read data from it.

   -----------------------------------------------------------------------------------------------------
   IMPORTANT CONCEPTS
   -----------------------------------------------------------------------------------------------------

   1) "Sliding window" DOES NOT mean data retention
      The sliding window only determines which sessions are recalculated for late-event correction.

   2) `session_facts` keeps full history
      Old sessions remain stored forever unless you later add a TTL or retention job.

   3) `session_facts_latest` is the "current truth" layer
      It contains the latest effective version of each session and is the recommended source for roll-ups
      and direct SQL queries that need one row per session.

   4) This script is optimized for correctness and clarity first
      It is already production-friendly in shape, but you can later tune refresh frequencies, partitions,
      and roll-up scopes after observing real data volume and ingestion lag.

===================================================================================================== */


/* =====================================================================================================
   PIPELINE DIAGRAM
   =====================================================================================================

    ┌───────────────────────────────────────────────┐
    │               Browser / Site                  │
    │                                               │
    │  pageview | content_click | conversion | ...  │
    │                                               │
    └──────────────────────┬────────────────────────┘
                           │
                           ▼
    ┌───────────────────────────────────────────────┐
    │              events  (MergeTree)              │
    │                                               │
    │  - one row per event                          │
    │  - raw immutable ingestion stream             │
    │  - device_category / browser_family set by    │
    │    Java at ingest time (UA parsing)           │
    │                                               │
    └──────────────────────┬────────────────────────┘
                           │  (incremental MV)
                           ▼
    ┌───────────────────────────────────────────────┐
    │        session_states (AggregatingMT)         │
    │                                               │
    │  - mergeable per-session states               │
    │  - late events naturally merge in             │
    │                                               │
    └──────────────────────┬────────────────────────┘
                           │  (refreshable MV APPEND)
                           ▼
    ┌───────────────────────────────────────────────┐
    │          session_facts (ReplacingMT)          │
    │                                               │
    │  - full historical session store              │
    │  - newer versions appended for recent rows    │
    │                                               │
    └──────────────────────┬────────────────────────┘
                           │  (refreshable MV)
                           ▼
    ┌───────────────────────────────────────────────┐
    │       session_facts_latest (ReplacingMT)      │
    │                                               │
    │  - exactly one latest row per session         │
    │  - source of truth for roll-ups               │
    │                                               │
    └────────────┬────────────────────────┬─────────┘
                 │                        │
                 ▼                        ▼
    ┌─────────────────────────┐   ┌────────────────────────────┐
    │    engagement_daily     │   │     sessions_by_*_daily    │
    │  (daily KPI roll-up)    │   │ (device / browser / lang)  │
    └────────────┬────────────┘   └──────────────┬─────────────┘
                 │                               │
                 ▼                               ▼
    ┌────────────────────────────────────────────────────────┐
    │            Raw SQL queries / service layer             │
    │                                                        │
    │  - KPI cards                                           │
    │  - trend charts                                        │
    │  - distribution widgets                                │
    │  - arbitrary date ranges                               │
    │                                                        │
    └───────────────────────────┬────────────────────────────┘
                                │
                                ▼
        ┌───────────────────────────────────────────────┐
        │              Angular Dashboard                │
        └───────────────────────────────────────────────┘

===================================================================================================== */


/* =====================================================================================================
   1) SESSION STATES
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Table

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.session_states

   ENGINE
   -----------------------------------------------------------------------------------------------------
   ReplicatedAggregatingMergeTree

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   This table stores mergeable per-session aggregate states derived from the raw events stream.

   Instead of repeatedly scanning analytics.events and running large GROUP BY queries every time we want
   session-level metrics, we continuously maintain aggregate states per session.

   This is the scalable "intermediate session layer."

   WHY IT EXISTS
   -----------------------------------------------------------------------------------------------------
   - keeps ingestion incremental and efficient
   - absorbs late-arriving events automatically
   - avoids rebuilding sessions from scratch from raw events over and over
   - allows downstream session finalization to work on a much smaller table than analytics.events

   GRAIN
   -----------------------------------------------------------------------------------------------------
   One logical session is identified by:

     (customer_id, environment, site_id, session_id)

   WHY AGGREGATE FUNCTION COLUMNS?
   -----------------------------------------------------------------------------------------------------
   Because AggregatingMergeTree expects mergeable states:
   - minState(...) / minMerge(...)
   - maxState(...) / maxMerge(...)
   - countState() / countMerge(...)
   - argMaxState(...) / argMaxMerge(...)

   This allows partial rows written from many insert batches to merge correctly into one logical session.

===================================================================================================== */
CREATE TABLE IF NOT EXISTS analytics.session_states
(
    /* Tenant scope: required to isolate customers and environments cleanly */
    customer_id LowCardinality(String),        -- dotCMS customer / tenant identifier
    environment LowCardinality(String),         -- deployment environment (prod/stage/etc.)
    site_id String,
    /* Session boundary */
    session_id  String,        -- unique session identifier. All events with the same session_id belong together

    /* Session time window (mergeable aggregate states) */
    min_ts_state AggregateFunction(min, DateTime64(3, 'UTC')), -- earliest event timestamp seen in session
    max_ts_state AggregateFunction(max, DateTime64(3, 'UTC')), -- latest event timestamp seen in session

    /* Event counters (mergeable) */
    total_events_state AggregateFunction(count),               -- total events in session
    pageviews_state    AggregateFunction(countIf, UInt8),      -- total number of pageview events in the session
    conversions_state  AggregateFunction(countIf, UInt8),      -- total number of conversion events in the session

    /* Dimension "last known value" states (mergeable) */
    -- last-seen device category label for the session (set by Java at ingestion time)
    -- stored as state so that late events can update the final value deterministically.
    device_category_state AggregateFunction(argMax, String, DateTime64(3, 'UTC')),
    -- last-seen browser family bucket (Chrome/Safari/Firefox/Edge/Other)
    browser_family_state  AggregateFunction(argMax, String, DateTime64(3, 'UTC')),
    -- last-seen dotCMS language ISO code, defaulting to '' ('undefined') if unknown
    locale_id_state       AggregateFunction(argMax, String, DateTime64(3, 'UTC'))
)
    /* Why this engine is mandatory:
        -> You are storing aggregate states
        -> You rely on merge correctness
        -> Without replication, different replicas would compute different session states */
    ENGINE = ReplicatedAggregatingMergeTree()
    /* Partitioning note:
       We partition by a hash of (customer, cluster) to spread writes and merges.
       This avoids a single giant partition for big tenants and keeps merges parallelizable. */
    PARTITION BY sipHash64(customer_id, environment) % 64
    /* Note for the sort key:
       ORDER BY includes tenant + session to keep session states physically clustered for merges/finalization.
       This also ensures stable grouping keys for session_facts refresh queries. */
    ORDER BY (
              customer_id,
              environment,
              site_id,
              session_id);


/* =====================================================================================================
   4) REAL-TIME MV: events → session_states
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Materialized View (incremental, insert-triggered)

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.session_states_mv

   SOURCE
   -----------------------------------------------------------------------------------------------------
   analytics.events

   TARGET
   -----------------------------------------------------------------------------------------------------
   analytics.session_states

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Runs on every insert into analytics.events and converts the newly inserted batch into mergeable
   session aggregate states.

   WHY THIS MV IS IMPORTANT
   -----------------------------------------------------------------------------------------------------
   This is the object that keeps the whole pipeline scalable.

   Without it, every sessionization/finalization step would need to repeatedly scan raw events and group
   them again. With this MV:
   - inserts stay cheap
   - aggregation work is incremental
   - late events naturally merge into existing sessions

   DIMENSION STRATEGY
   -----------------------------------------------------------------------------------------------------
   device_category and browser_family are set by Java at ingestion time and read directly from analytics.events.

===================================================================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.session_states_mv
            TO analytics.session_states
AS
WITH
    /* Normalize empty locale values so they can be ignored cleanly in argMaxStateIf */
    nullIf(locale_id, '') AS normalized_locale_id
SELECT
    e.customer_id,
    e.environment,
    e.site_id,
    e.session_id,

    /* Time boundaries for the session */
    minState(e.event_time) AS min_ts_state,
    maxState(e.event_time) AS max_ts_state,

    /* Mergeable counters */
    countState() AS total_events_state,
    countIfState(e.event_type = 'pageview')   AS pageviews_state,
    countIfState(e.event_type = 'conversion') AS conversions_state,

    /* Mergeable latest dimension states — values already set by Java at ingestion time */
    argMaxState(e.device_category, e.event_time) AS device_category_state,
    argMaxState(e.browser_family, e.event_time)  AS browser_family_state,

    /* Locale is tracked only from pageview events and only when present */
    argMaxStateIf(
        normalized_locale_id,
        e.event_time,
        e.event_type = 'pageview' AND normalized_locale_id IS NOT NULL
    ) AS locale_id_state
FROM analytics.events AS e
WHERE e.session_id != ''
  AND e.customer_id != ''
  AND e.environment != ''
  AND e.site_id != ''
  /* Defensive guard to avoid broken/null-ish timestamps participating in session logic */
  AND e.event_time > toDateTime64(0, 3, 'UTC')
GROUP BY (
    e.customer_id,
    e.environment,
    e.site_id,
    e.session_id);


/* =====================================================================================================
   5) SESSION FACTS (FULL HISTORICAL VERSIONED TABLE)
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Table

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts

   ENGINE
   -----------------------------------------------------------------------------------------------------
   ReplicatedReplacingMergeTree(updated_at)

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Stores the full historical session table.

   This is NOT just a hot window table.
   This table is meant to retain all sessions across all history.

   WHY ReplacingMergeTree(updated_at)?
   -----------------------------------------------------------------------------------------------------
   Because recent sessions may be recalculated when late events arrive.

   Example:
     - session originally finalized at 10:05
     - delayed event arrives at 10:20
     - next RMV refresh recalculates that session and appends a newer row

   ReplacingMergeTree allows the newer version to win logically by updated_at.

   IMPORTANT
   -----------------------------------------------------------------------------------------------------
   Because session_facts_rmv uses APPEND TO, multiple physical versions of the same session may coexist
   temporarily in this table until background merges occur.

   That is exactly why we introduce analytics.session_facts_latest later:
   it provides a deduplicated "latest truth" layer for roll-ups and direct querying.

   RECOMMENDED USAGE
   -----------------------------------------------------------------------------------------------------
   - Keep this table as your durable historical versioned store
   - Do NOT use it directly for roll-ups if you need exactly one row per session
   - Use analytics.session_facts_latest for that

===================================================================================================== */
CREATE TABLE IF NOT EXISTS analytics.session_facts
(
    /* Tenant scope */
    customer_id LowCardinality(String),
    environment LowCardinality(String),
    site_id String,
    /* Session identity */
    session_id  String,

    /* Finalized session times */
    session_start DateTime64(3, 'UTC'), -- earliest event timestamp
    session_end   DateTime64(3, 'UTC'), -- latest event timestamp
    duration_seconds UInt32,            -- session_end - session_start (seconds)

    /* Finalized counters */
    total_events UInt32,                -- total events in session
    pageviews    UInt32,                -- total pageview events
    conversions  UInt32,                -- total conversion events

    /* Engagement flag (GA4-style) */
    engaged UInt8,                      -- 1 if engaged, else 0

    /* Finalized dimensions */
    device_category LowCardinality(String), -- Desktop/Mobile/Tablet/Other
    browser_family  LowCardinality(String), -- Chrome/Safari/Firefox/Edge/Other
    locale_id LowCardinality(String),       -- dotCMS language Locale ID ('' means undefined)

    /* Version column. Newer recalculations must have a greater timestamp. */
    updated_at DateTime64(3, 'UTC')
)
    ENGINE = ReplicatedReplacingMergeTree(updated_at)
    /* Partition by month of session_start. Keeps partitions time-bounded and supports TTL strategies
       later if desired. */
    PARTITION BY toYYYYMM(toDate(session_start))
    /* Sort key includes session identity for deterministic replacement. */
    ORDER BY (
              customer_id,
              environment,
              site_id,
              session_id);


/* =====================================================================================================
   6) REFRESHABLE MV: session_states → session_facts
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Refreshable Materialized View (RMV)

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts_rmv

   SOURCE
   -----------------------------------------------------------------------------------------------------
   analytics.session_states

   TARGET
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts

   WRITE MODE
   -----------------------------------------------------------------------------------------------------
   APPEND TO

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Re-finalizes only RECENT sessions and appends a new version into analytics.session_facts.

   This is the core late-event correction mechanism.

   SLIDING WINDOW
   -----------------------------------------------------------------------------------------------------
   start_cutoff = now() - 1 day

   This means:
     - only sessions whose latest activity is recent are recalculated
     - older sessions remain in analytics.session_facts untouched
     - this is NOT a retention window

   WHY 1 DAY?
   -----------------------------------------------------------------------------------------------------
   Good conservative default for local testing:
   - covers same-day late arrivals comfortably
   - easy to reason about
   - can later be reduced to 12h or 6h if actual ingestion lag is small

   ENGAGEMENT LOGIC
   -----------------------------------------------------------------------------------------------------
   A session is engaged if ANY of these is true:
     - duration > 10 seconds
     - pageviews >= 2
     - conversions >= 1

   DIMENSION FINALIZATION
   -----------------------------------------------------------------------------------------------------
   device_category and browser_family are read directly from session_states — values were set by Java
   at ingestion time and propagated via session_states_mv.

===================================================================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.session_facts_rmv
-- Refreshing every 30 seconds FOR LOCAL DEVELOPMENT ONLY! For DEV, use at least REFRESH EVERY 15 MINUTE
REFRESH EVERY 30 SECOND APPEND TO analytics.session_facts
AS
WITH
    /* Sliding recomputation window for late-event correction */
    (now64(3, 'UTC') - INTERVAL 1 DAY) AS start_cutoff
SELECT
    customer_id,
    environment,
    site_id,
    session_id,

    session_start,
    session_end,
    duration_seconds,

    total_events,
    pageviews,
    conversions,

    engaged,

    device_category,
    browser_family,

    locale_id,

    /* Version timestamp for ReplacingMergeTree */
    now64(3, 'UTC') AS updated_at
FROM
    (
        /* Aggregate session_states into finalized scalar columns */
        SELECT
            customer_id,
            environment,
            site_id,
            session_id,

            /* Finalized time boundaries */
            minMerge(min_ts_state) AS session_start,
            maxMerge(max_ts_state) AS session_end,

            /* Derived duration */
            toUInt32(greatest(0, dateDiff('second', session_start, session_end))) AS duration_seconds,

            /* Finalized counters */
            toUInt32(countMerge(total_events_state)) AS total_events,
            toUInt32(countIfMerge(pageviews_state))  AS pageviews,
            toUInt32(countIfMerge(conversions_state)) AS conversions,

            /* Business rules that determine whether a session is flagged as 'engaged' or not */
            toUInt8(
                -- 1. Sessions that last more than 10 seconds
                    (dateDiff('second', session_start, session_end) > 10)
                        -- 2. Or, sessions that trigger at least 2 events of type 'pageview'
                        OR (countIfMerge(pageviews_state) >= 2)
                        -- 3. Or, sessions that trigger at least 1 event of type 'conversion'
                        OR (countIfMerge(conversions_state) >= 1)
            ) AS engaged,

            /* Dimension values set by Java at ingestion time, propagated via session_states_mv */
            argMaxMerge(device_category_state) AS device_category,
            argMaxMerge(browser_family_state)  AS browser_family,

            /* Locale defaults to empty string when unknown */
            coalesce(argMaxMerge(locale_id_state), '') AS locale_id
        FROM analytics.session_states
        GROUP BY (
                  customer_id,
                  environment,
                  site_id,
                  session_id)
        /* Only recent sessions are recalculated */
        HAVING session_end >= start_cutoff
    ) finalized_sessions;


/* =====================================================================================================
   7) SESSION FACTS LATEST (DEDUPLICATED INTERMEDIATE TABLE)
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Table

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts_latest

   ENGINE
   -----------------------------------------------------------------------------------------------------
   ReplicatedReplacingMergeTree(updated_at)

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Stores exactly one latest effective row per session key.

   WHY THIS TABLE EXISTS
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts is a versioned historical store. Because it receives APPENDed updates for
   recent sessions, the same logical session may temporarily exist in multiple versions.

   We could read analytics.session_facts FINAL everywhere, but FINAL is heavier and we do not want every
   downstream roll-up to pay that cost repeatedly.

   So instead:
     - we deduplicate once into session_facts_latest
     - roll-ups and direct SQL queries can use this table
     - downstream SQL stays simpler and more efficient

   RECOMMENDED USAGE
   -----------------------------------------------------------------------------------------------------
   This is the preferred source when you want:
     - one row per session
     - latest metrics only
     - session-level raw SQL queries
     - roll-up generation

===================================================================================================== */
CREATE TABLE IF NOT EXISTS analytics.session_facts_latest
(
    customer_id LowCardinality(String),
    environment LowCardinality(String),
    site_id String,
    session_id  String,

    session_start DateTime64(3, 'UTC'),
    session_end   DateTime64(3, 'UTC'),
    duration_seconds UInt32,

    total_events UInt32,
    pageviews    UInt32,
    conversions  UInt32,

    engaged UInt8,

    device_category LowCardinality(String),
    browser_family  LowCardinality(String),
    locale_id LowCardinality(String),

    updated_at DateTime64(3, 'UTC')
)
    ENGINE = ReplicatedReplacingMergeTree(updated_at)
    PARTITION BY toYYYYMM(toDate(session_start))
    ORDER BY (
        customer_id,
        environment,
        site_id,
        session_id
    );


/* =====================================================================================================
   8) REFRESHABLE MV: session_facts → session_facts_latest
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Refreshable Materialized View (RMV)

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts_latest_rmv

   SOURCE
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts

   TARGET
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts_latest

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Centralizes deduplication of the historical versioned session table into one latest row per session key.

   WHY THIS IS BETTER THAN USING FINAL EVERYWHERE
   -----------------------------------------------------------------------------------------------------
   Instead of every downstream roll-up doing its own deduplication or reading session_facts FINAL, this
   RMV does the work once and stores the result in a clean intermediate table.

   DEDUPLICATION RULE
   -----------------------------------------------------------------------------------------------------
   For each session key:
     - take the value associated with the greatest updated_at
     - that is done via argMax(..., updated_at)
     - store max(updated_at) as the effective row version

===================================================================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.session_facts_latest_rmv
-- Refreshing every 30 seconds FOR LOCAL DEVELOPMENT ONLY! For DEV, use at least REFRESH EVERY 15 MINUTE
REFRESH EVERY 30 SECOND DEPENDS ON analytics.session_facts_rmv
TO analytics.session_facts_latest
AS
SELECT
    sf.customer_id,
    sf.environment,
    sf.site_id,
    sf.session_id,

    argMax(sf.session_start, sf.updated_at) AS session_start,
    argMax(sf.session_end, sf.updated_at) AS session_end,
    argMax(sf.duration_seconds, sf.updated_at) AS duration_seconds,

    argMax(sf.total_events, sf.updated_at) AS total_events,
    argMax(sf.pageviews, sf.updated_at) AS pageviews,
    argMax(sf.conversions, sf.updated_at) AS conversions,

    argMax(sf.engaged, sf.updated_at) AS engaged,
    argMax(sf.device_category, sf.updated_at) AS device_category,
    argMax(sf.browser_family, sf.updated_at) AS browser_family,
    argMax(sf.locale_id, sf.updated_at) AS locale_id,

    max(sf.updated_at) AS updated_at
FROM analytics.session_facts AS sf
GROUP BY (
    sf.customer_id,
    sf.environment,
    sf.site_id,
    sf.session_id);


/* =====================================================================================================
   9) DAILY KPI ROLL-UP
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Table

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.engagement_daily

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Stores dashboard-ready daily KPI numerators and denominators.

   WHY STORE DAILY SUMS INSTEAD OF DAILY RATES?
   -----------------------------------------------------------------------------------------------------
   Because arbitrary date ranges must be computed correctly as:

     sum(numerator) / sum(denominator)

   not as:
     average(daily_rate)

   This table therefore stores the raw daily ingredients needed to compute:
   - engagement rate
   - conversion rate
   - average interactions
   - average session duration

   GRAIN
   -----------------------------------------------------------------------------------------------------
   (customer_id, environment, site_id, day)

===================================================================================================== */
CREATE TABLE IF NOT EXISTS analytics.engagement_daily
(
    customer_id LowCardinality(String),
    environment LowCardinality(String),
    site_id String,
    day Date,

    total_sessions UInt64,                    -- count of all sessions
    engaged_sessions UInt64,                  -- count of engaged sessions
    engaged_conversion_sessions UInt64,       -- engaged sessions that include >=1 conversion

    total_events_all UInt64,                  -- sum(total_events) across all sessions
    total_duration_all UInt64,                -- sum(duration_seconds) across all sessions

    total_events_engaged UInt64,              -- sum(total_events) across engaged sessions only
    total_duration_engaged UInt64,            -- sum(duration_seconds) across engaged sessions only

    updated_at DateTime64(3, 'UTC')
)
    ENGINE = ReplicatedReplacingMergeTree(updated_at)
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, environment, site_id, day);


/* =====================================================================================================
   10) REFRESHABLE MV: session_facts_latest → engagement_daily
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Refreshable Materialized View (RMV)

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.engagement_daily_rmv

   SOURCE
   -----------------------------------------------------------------------------------------------------
   analytics.session_facts_latest

   TARGET
   -----------------------------------------------------------------------------------------------------
   analytics.engagement_daily

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Rebuilds the daily KPI roll-up table from the latest one-row-per-session layer.

   WHY THIS SOURCE?
   -----------------------------------------------------------------------------------------------------
   Because session_facts_latest already contains one latest row per session, this roll-up can aggregate
   without FINAL and without inline dedup logic.

===================================================================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.engagement_daily_rmv
-- Refreshing every 30 seconds FOR LOCAL DEVELOPMENT ONLY! For DEV, use at least REFRESH EVERY 15 MINUTE
REFRESH EVERY 30 SECOND DEPENDS ON analytics.session_facts_latest_rmv
    TO analytics.engagement_daily
AS
SELECT
    customer_id,
    environment,
    site_id,
    toDate(session_start, 'UTC') AS day,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    countIf(engaged = 1 AND conversions >= 1) AS engaged_conversion_sessions,

    sum(total_events) AS total_events_all,
    sum(duration_seconds) AS total_duration_all,

    sumIf(total_events, engaged = 1) AS total_events_engaged,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged,

    now64(3, 'UTC') AS updated_at
FROM analytics.session_facts_latest
GROUP BY (
          customer_id,
          environment,
          site_id,
          day);


/* =====================================================================================================
   11) DEVICE BREAKDOWN ROLL-UP
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Table

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.sessions_by_device_daily

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Daily distribution table by device category.

   Typical dashboard uses:
   - total sessions by device
   - engaged sessions by device
   - average engaged duration by device

===================================================================================================== */
CREATE TABLE IF NOT EXISTS analytics.sessions_by_device_daily
(
    customer_id LowCardinality(String),
    environment LowCardinality(String),
    site_id String,
    day Date,

    device_category LowCardinality(String),  -- Desktop/Mobile/Tablet/Other

    total_sessions UInt64,                   -- ALL sessions for this device_category
    engaged_sessions UInt64,                 -- Engaged sessions for this device_category
    total_duration_engaged_seconds UInt64,   -- Sum(duration_seconds) for engaged sessions only

    updated_at DateTime64(3, 'UTC')
)
    ENGINE = ReplicatedReplacingMergeTree(updated_at)
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, environment, site_id, day, device_category);


/* =====================================================================================================
   12) REFRESHABLE MV: session_facts_latest → sessions_by_device_daily
   ===================================================================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.sessions_by_device_daily_rmv
-- Refreshing every 30 seconds FOR LOCAL DEVELOPMENT ONLY! For DEV, use at least REFRESH EVERY 15 MINUTE
REFRESH EVERY 30 SECOND DEPENDS ON analytics.session_facts_latest_rmv
    TO analytics.sessions_by_device_daily
AS
SELECT
    customer_id,
    environment,
    site_id,
    toDate(session_start, 'UTC') AS day,
    device_category,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged_seconds,

    now64(3, 'UTC') AS updated_at
FROM analytics.session_facts_latest
GROUP BY (
          customer_id,
          environment,
          site_id,
          day,
          device_category);


/* =====================================================================================================
   13) BROWSER BREAKDOWN ROLL-UP
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Table

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.sessions_by_browser_daily

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Daily distribution table by browser family.

===================================================================================================== */
CREATE TABLE IF NOT EXISTS analytics.sessions_by_browser_daily
(
    customer_id LowCardinality(String),
    environment LowCardinality(String),
    site_id String,
    day Date,

    browser_family LowCardinality(String), -- Chrome/Safari/Firefox/Edge/Other

    total_sessions UInt64,
    engaged_sessions UInt64,
    total_duration_engaged_seconds UInt64,

    updated_at DateTime64(3, 'UTC')
)
    ENGINE = ReplicatedReplacingMergeTree(updated_at)
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, environment, site_id, day, browser_family);


/* =====================================================================================================
   14) REFRESHABLE MV: session_facts_latest → sessions_by_browser_daily
   ===================================================================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.sessions_by_browser_daily_rmv
-- Refreshing every 30 seconds FOR LOCAL DEVELOPMENT ONLY! For DEV, use at least REFRESH EVERY 15 MINUTE
REFRESH EVERY 30 SECOND DEPENDS ON analytics.session_facts_latest_rmv
    TO analytics.sessions_by_browser_daily
AS
SELECT
    customer_id,
    environment,
    site_id,
    toDate(session_start, 'UTC') AS day,
    browser_family,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged_seconds,

    now64(3, 'UTC') AS updated_at
FROM analytics.session_facts_latest
GROUP BY (
          customer_id,
          environment,
          site_id,
          day,
          browser_family);


/* =====================================================================================================
   15) LANGUAGE BREAKDOWN ROLL-UP
   =====================================================================================================

   OBJECT TYPE
   -----------------------------------------------------------------------------------------------------
   Table

   OBJECT NAME
   -----------------------------------------------------------------------------------------------------
   analytics.sessions_by_language_daily

   PURPOSE
   -----------------------------------------------------------------------------------------------------
   Daily distribution table by locale_id.

   NOTE
   -----------------------------------------------------------------------------------------------------
   locale_id remains the raw dotCMS locale/language identifier.
   If you later want user-friendly names, that translation can happen in SQL joins or in the service layer.

===================================================================================================== */
CREATE TABLE IF NOT EXISTS analytics.sessions_by_language_daily
(
    customer_id LowCardinality(String),
    environment LowCardinality(String),
    site_id String,
    day Date,

    locale_id LowCardinality(String),    -- dotCMS language Locale ID ('' means undefined)

    total_sessions UInt64,
    engaged_sessions UInt64,
    total_duration_engaged_seconds UInt64,

    updated_at DateTime64(3, 'UTC')
)
    ENGINE = ReplicatedReplacingMergeTree(updated_at)
    PARTITION BY toYYYYMM(day)
    ORDER BY (customer_id, environment, site_id, day, locale_id);


/* =====================================================================================================
   16) REFRESHABLE MV: session_facts_latest → sessions_by_language_daily
   ===================================================================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.sessions_by_language_daily_rmv
-- Refreshing every 30 seconds FOR LOCAL DEVELOPMENT ONLY! For DEV, use at least REFRESH EVERY 15 MINUTE
REFRESH EVERY 30 SECOND DEPENDS ON analytics.session_facts_latest_rmv
    TO analytics.sessions_by_language_daily
AS
SELECT
    customer_id,
    environment,
    site_id,
    toDate(session_start, 'UTC') AS day,
    locale_id,

    count() AS total_sessions,
    countIf(engaged = 1) AS engaged_sessions,
    sumIf(duration_seconds, engaged = 1) AS total_duration_engaged_seconds,

    now64(3, 'UTC') AS updated_at
FROM analytics.session_facts_latest
GROUP BY (
          customer_id,
          environment,
          site_id,
          day,
          locale_id);


/* =====================================================================================================
   QUERYING GUIDANCE
   =====================================================================================================

   RECOMMENDED TABLES FOR RAW SQL
   -----------------------------------------------------------------------------------------------------

   1) Query analytics.session_facts_latest when you need:
      - one row per session
      - latest session metrics only
      - session-level exploration/debugging
      - session KPI calculations on the fly

   2) Query analytics.engagement_daily when you need:
      - KPI cards
      - trends over time
      - engagement/conversion/avg-interaction metrics over arbitrary date ranges

   3) Query analytics.sessions_by_device_daily / browser / language when you need:
      - dashboard distribution widgets
      - grouped daily breakdowns
      - top-N device/browser/language reports

   4) Query analytics.session_facts only when you specifically need:
      - historical row versions
      - debugging of late-event recalculations
      - low-level understanding of how session versions changed over time

   EXAMPLE MENTAL MODEL
   -----------------------------------------------------------------------------------------------------

     analytics.session_facts         = durable version history
     analytics.session_facts_latest  = current one-row-per-session truth
     analytics.engagement_daily      = daily KPI roll-up
     analytics.sessions_by_*_daily   = daily grouped dashboard roll-ups

===================================================================================================== */