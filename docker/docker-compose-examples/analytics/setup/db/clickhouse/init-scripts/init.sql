CREATE DATABASE IF NOT EXISTS clickhouse_test_db;
CREATE TABLE IF NOT EXISTS clickhouse_test_db.events
(
    -- ######################################################
    --                  Jitsu Properties
    -- ######################################################
    _timestamp DateTime CODEC(DoubleDelta, ZSTD(3)),
    event_type LowCardinality(String),
    original_url String CODEC(ZSTD(3)),
    doc_host String,
    doc_path String,
    doc_search String,
    page_title String,
    referer String CODEC(ZSTD(3)),
    doc_encoding LowCardinality(String),
    local_tz_offset Int64,
    eventn_ctx_event_id String CODEC(ZSTD(3)),
    user_agent String LowCardinality(String),
    parsed_ua_device_brand Nullable(LowCardinality(String)) CODEC(ZSTD(3)),
    parsed_ua_device_family Nullable(LowCardinality(String)) CODEC(ZSTD(3)),
    parsed_ua_device_model Nullable(LowCardinality(String)),
    parsed_ua_os_family Nullable(LowCardinality(String)) CODEC(ZSTD(3)),
    parsed_ua_os_version Nullable(String),
    parsed_ua_ua_family Nullable(LowCardinality(String)) CODEC(ZSTD(3)),
    parsed_ua_ua_version Nullable(String),
    parsed_ua_bot UInt8,
    source_ip String,
    vp_size String,
    utm_campaign Nullable(LowCardinality(String)) CODEC(ZSTD(3)),
    utm_medium Nullable(LowCardinality(String)) CODEC(ZSTD(3)),
    utm_source Nullable(LowCardinality(String)) CODEC(ZSTD(3)),
    utm_term Nullable(String),
    utm_content Nullable(String) CODEC(ZSTD(3)),
    doc_hash Nullable(String),
    doc_protocol LowCardinality(String) CODEC(ZSTD(3)),
    user_language LowCardinality(String) CODEC(ZSTD(3)),
    context_site_key LowCardinality(String),
    context_user_id String CODEC(ZSTD(3)),
    screen_resolution String,
    viewport_height String,
    viewport_width String,
    context_site_id LowCardinality(String),
    api_key LowCardinality(String),
    cluster_id LowCardinality(String),
    customer_id LowCardinality(String),
    src String,
    user_anonymous_id String,
    user_hashed_anonymous_id String,
    custom_1 Nullable(String),
    custom_2 Nullable(String),
    custom_3 Nullable(String),
    custom_4 Nullable(String),
    custom_5 Nullable(String),
    custom_6 Nullable(String),
    custom_7 Nullable(String),
    custom_8 Nullable(String),
    custom_9 Nullable(String),
    custom_10 Nullable(String),
    custom_11 Nullable(String),
    custom_12 Nullable(String),
    custom_13 Nullable(String),
    custom_14 Nullable(String),
    custom_15 Nullable(String),
    custom_16 Nullable(String),
    custom_17 Nullable(String),
    custom_18 Nullable(String),
    custom_19 Nullable(String),
    custom_20 Nullable(String),
    custom_21 Nullable(String),
    custom_22 Nullable(String),
    custom_23 Nullable(String),
    custom_24 Nullable(String),
    custom_25 Nullable(String),
    custom_26 Nullable(String),
    custom_27 Nullable(String),
    custom_28 Nullable(String),
    custom_29 Nullable(String),
    custom_30 Nullable(String),
    custom_31 Nullable(String),
    custom_32 Nullable(String),
    custom_33 Nullable(String),
    custom_34 Nullable(String),
    custom_35 Nullable(String),
    custom_36 Nullable(String),
    custom_37 Nullable(String),
    custom_38 Nullable(String),
    custom_39 Nullable(String),
    custom_40 Nullable(String),
    custom_41 Nullable(String),
    custom_42 Nullable(String),
    custom_43 Nullable(String),
    custom_44 Nullable(String),
    custom_45 Nullable(String),
    custom_46 Nullable(String),
    custom_47 Nullable(String),
    custom_48 Nullable(String),
    custom_49 Nullable(String),
    custom_50 Nullable(String),


    -- ######################################################
    --      Jitsu and Backend Data Collectors Properties
    -- ######################################################
    url String,
    utc_time DateTime CODEC(DoubleDelta, ZSTD(3)),
    request_id String CODEC(ZSTD(3)),
    sessionid String CODEC(ZSTD(3)),
    sessionnew UInt8,


    -- ######################################################
    --                 Experiments Properties
    -- ######################################################
    isexperimentpage Bool DEFAULT false,
    experiment Nullable(String),
    variant Nullable(String),
    persona Nullable(String),
    userlanguage LowCardinality(String),
    lookbackwindow Nullable(String),
    runningid Nullable(String),
    istargetpage Bool DEFAULT false,
    ip Nullable(String),


    -- ######################################################
    --              Used in UVE_MODE_CHANGE event
    -- ######################################################
    frommode Nullable(String),
    tomode Nullable(String),


    -- ######################################################
    --              Used in content_impression event
    -- ######################################################
    content_identifier Nullable(String) CODEC(ZSTD(3)),
    content_inode Nullable(String) CODEC(ZSTD(3)),
    content_title Nullable(String),
    content_content_type Nullable(String),

    position_viewport_offset_pct Nullable(Int16),
    position_dom_index Nullable(Int8),

    -- ######################################################
    --              Used in content_click event
    -- ######################################################
    element_text Nullable(String),
    element_type Nullable(String),
    element_id Nullable(String),
    element_class Nullable(String),
    element_attributes Nullable(String),

    -- ######################################################
    --              Used in conversion event
    -- ######################################################
    conversion_name Nullable(LowCardinality(String)) CODEC(ZSTD(3))
) Engine = MergeTree()
PARTITION BY (customer_id, toYYYYMM(utc_time))
ORDER BY (customer_id, context_user_id, utc_time)
SETTINGS
    index_granularity = 8192,
    min_bytes_for_wide_part = 10485760,  -- 10MB
    min_rows_for_wide_part = 100000,     -- 100k rows
    merge_with_ttl_timeout = 3600;       -- merge once per hour

-- Time and cluster filtering
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_utc_time (utc_time) TYPE minmax GRANULARITY 1;
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_cluster_id (cluster_id) TYPE bloom_filter GRANULARITY 64;
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_cluster_id (customer_id) TYPE bloom_filter GRANULARITY 64;

-- Event-type based filtering (funnels)
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_event_type (event_type) TYPE set(100) GRANULARITY 1;
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_conversion (conversion_name) TYPE set(100) GRANULARITY 1;

-- Session/User/Content filtering
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_sessionid (sessionid) TYPE bloom_filter GRANULARITY 64;
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_content_identifier (content_identifier) TYPE bloom_filter GRANULARITY 64;