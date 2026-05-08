-- =====================================================================
-- Stores daily aggregated counts of events per:
--
-- day
-- environment
-- customer_id
-- event_type
-- user_id
-- identifier (URL or content_id)
-- title

-- Why SummingMergeTree?

-- Because the MV inserts pre-aggregated rows, and daily_total is summed on merge.

--This allows:
--fast incremental updates
--easy "daily counts" reporting
--low storage overhead
-- =====================================================================

CREATE TABLE IF NOT EXISTS analytics.content_events_counter
(
    day Date,

    environment LowCardinality(String),
    customer_id LowCardinality(String),

    site_id String,

    event_type LowCardinality(String),
    user_id String CODEC(ZSTD(3)),

    identifier String CODEC(ZSTD(3)),
    title String,

    daily_total UInt64
)
    ENGINE = ReplicatedSummingMergeTree(daily_total)
PARTITION BY (customer_id, environment, toYYYYMM(day))
ORDER BY (customer_id, environment, user_id, day, identifier, title, event_type);


-- =====================================================================
-- Transforms raw events into daily activity counters.
-- For every event inserted into events, it computes:
--
-- day → start-of-day from event_time
-- identifier → URL for pageview, content_identifier otherwise
-- title → page_title or content_title
--
-- Then groups by:
-- customer_id, environment, user_id, day, identifier, title, event_type
--
-- And inserts:
--
-- count(*) AS daily_total
-- =====================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.content_events_counter_mv TO analytics.content_events_counter AS
SELECT customer_id,
       environment,
       event_type,
       user_id,
       site_id,
       toStartOfDay(event_time) as day,
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
FROM analytics.events
GROUP BY customer_id, environment, user_id, day, identifier, title, event_type, site_id;


-- =====================================================================
-- Stores daily pre-aggregated pageview counts grouped by device category and browser family.
--
-- Why SummingMergeTree?
--   The same reasoning as content_events_counter: the MV inserts pre-aggregated rows,
--   and pageview_count is summed on merge. Allows fast, scalable reads for the
--   "Pageviews by Device & Browser" dashboard metric.
--
-- Java sets device_category and browser_family at ingestion time (UA parsing).
-- The MV normalizes empty strings (pre-enrichment historical events) to
-- 'unknown' / 'unknown' so the table never stores blanks.
--
-- =====================================================================
CREATE TABLE IF NOT EXISTS analytics.pageviews_by_device_browser_daily
(
    day             Date,
    customer_id     LowCardinality(String),
    environment     LowCardinality(String),
    site_id         String,
    device_category LowCardinality(String),
    browser_family  LowCardinality(String),
    pageview_count  UInt64
)
    ENGINE = ReplicatedSummingMergeTree(pageview_count)
    PARTITION BY (customer_id, environment, toYYYYMM(day))
    ORDER BY (customer_id, environment, site_id, day, device_category, browser_family);


CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.pageviews_by_device_browser_daily_mv
    TO analytics.pageviews_by_device_browser_daily AS
SELECT
    customer_id,
    environment,
    site_id,
    toStartOfDay(event_time)                                AS day,
    if(device_category = '', 'unknown', device_category)    AS device_category,
    if(browser_family  = '', 'unknown', browser_family)     AS browser_family,
    count(*)                                                AS pageview_count
FROM analytics.events
WHERE event_type = 'pageview'
GROUP BY customer_id, environment, site_id, day, device_category, browser_family;
