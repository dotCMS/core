CREATE DATABASE IF NOT EXISTS clickhouse_test_db;
CREATE TABLE IF NOT EXISTS clickhouse_test_db.events
(
    -- ######################################################
    --                  Jitsu Properties
    -- ######################################################
    _timestamp DateTime CODEC(DoubleDelta, ZSTD(3)),
    event_type LowCardinality(String) CODEC(ZSTD(3)),
    original_url String CODEC(ZSTD(3)),
    doc_host String,
    doc_path String,
    doc_search String,
    page_title String,
    referer String CODEC(ZSTD(3)),
    doc_encoding LowCardinality(String),
    local_tz_offset Int64,
    eventn_ctx_event_id String CODEC(ZSTD(3)),
    user_agent String CODEC(ZSTD(3)),
    parsed_ua_device_brand LowCardinality(String) CODEC(ZSTD(3)),
    parsed_ua_device_family LowCardinality(String) CODEC(ZSTD(3)),
    parsed_ua_device_model LowCardinality(String),
    parsed_ua_os_family LowCardinality(String) CODEC(ZSTD(3)),
    parsed_ua_os_version String,
    parsed_ua_ua_family LowCardinality(String) CODEC(ZSTD(3)),
    parsed_ua_ua_version String,
    parsed_ua_bot UInt8,
    source_ip String,
    vp_size String,
    utm_campaign LowCardinality(String) CODEC(ZSTD(3)),
    utm_medium LowCardinality(String) CODEC(ZSTD(3)),
    utm_source LowCardinality(String) CODEC(ZSTD(3)),
    utm_term String,
    utm_content String CODEC(ZSTD(3)),
    doc_hash String,
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
    utc_time DateTime CODEC(DoubleDelta, ZSTD(3)),
    request_id String CODEC(ZSTD(3)),
    sessionid String CODEC(ZSTD(3)),
    sessionnew UInt8,


    -- ######################################################
    --                 Experiments Properties
    -- ######################################################
    isexperimentpage Bool DEFAULT false,
    experiment String,
    variant String,
    persona String,
    userlanguage LowCardinality(String),
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
    content_identifier String CODEC(ZSTD(3)),
    content_inode String CODEC(ZSTD(3)),
    content_title String,
    content_content_type String,

    position_viewport_offset_pct Int16,
    position_dom_index Int8,

    -- ######################################################
    --              Used in content_click event
    -- ######################################################
    element_text Nullable(String),
    element_type String,
    element_id String,
    element_class String,
    element_attributes String,

    -- ######################################################
    --              Used in conversion event
    -- ######################################################
    conversion_name LowCardinality(String) CODEC(ZSTD(3))
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