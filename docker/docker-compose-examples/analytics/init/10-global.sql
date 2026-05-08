-- =====================================================================
-- This is the raw event ingestion table.
-- =====================================================================
CREATE TABLE IF NOT EXISTS analytics.events
(
    -- ######################################################
    --                  General Event Properties
    -- ######################################################
    timestamp DateTime64(3, 'UTC') CODEC(DoubleDelta, ZSTD(3)),
    event_time DateTime64(3, 'UTC') CODEC(DoubleDelta, ZSTD(3)),
    event_type LowCardinality(String),
    environment LowCardinality(String),
    customer_id LowCardinality(String),


    -- ######################################################
    --                      URL Properties
    -- ######################################################
    url String,
    page_title String,
    site_id String,
    doc_host String,
    doc_path String,
    doc_search String,
    doc_encoding LowCardinality(String),
    doc_hash Nullable(String),
    doc_protocol LowCardinality(String),
    referer String CODEC(ZSTD(3)),


    -- ######################################################
    --                    Browser Properties
    -- ######################################################
    user_agent String,
    -- Raw parsed UA fields (set by Java at ingest time via uap-java)
    parsed_ua_device_family LowCardinality(String),
    parsed_ua_os_family LowCardinality(String),
    parsed_ua_ua_family LowCardinality(String),
    -- Derived bucketed categories (set by Java at event ingestion time via in-memory lookup)
    device_category LowCardinality(String) DEFAULT '',
    browser_family LowCardinality(String) DEFAULT '',
    screen_resolution String,
    viewport_size String,
    viewport_height String,
    viewport_width String,
    browser_language LowCardinality(String),
    locale_id LowCardinality(String) DEFAULT '',
    user_id String CODEC(ZSTD(3)),
    session_id String CODEC(ZSTD(3)),


    -- ######################################################
    --               Analytics Tool Properties
    -- ######################################################
    utm_campaign LowCardinality(String),
    utm_medium LowCardinality(String),
    utm_source LowCardinality(String),
    utm_term Nullable(String),
    utm_content Nullable(String),


    -- ######################################################
    --              Used in content_impression events
    -- ######################################################
    content_identifier Nullable(String) CODEC(ZSTD(3)),
    content_inode Nullable(String) CODEC(ZSTD(3)),
    content_title Nullable(String),
    content_content_type Nullable(String),
    position_viewport_offset_pct Nullable(Int16),
    position_dom_index Nullable(Int8),


    -- ######################################################
    --              Used in content_click events
    -- ######################################################
    dom_element_text Nullable(String),
    dom_element_type Nullable(String),
    dom_element_id Nullable(String),
    dom_element_class Nullable(String),
    dom_element_attributes Nullable(String),


    -- ######################################################
    --              Used in conversion events
    -- ######################################################
    conversion_name String,


    -- ######################################################
    --                 Data skipping indexes
    -- ######################################################
    INDEX idx_event_time event_time TYPE minmax GRANULARITY 1,
    INDEX idx_environment environment TYPE bloom_filter GRANULARITY 64,
    INDEX idx_customer_id customer_id TYPE bloom_filter GRANULARITY 64,
    INDEX idx_event_type event_type TYPE set(100) GRANULARITY 1,
    INDEX idx_conversion conversion_name TYPE set(100) GRANULARITY 1,
    INDEX idx_user_id user_id TYPE bloom_filter GRANULARITY 64,
    INDEX idx_content_identifier content_identifier TYPE bloom_filter GRANULARITY 64,
    INDEX idx_device_category device_category TYPE set(50) GRANULARITY 1,
    INDEX idx_browser_family browser_family TYPE set(50) GRANULARITY 1

) Engine = ReplicatedMergeTree()
    PARTITION BY customer_id
    ORDER BY (timestamp, customer_id)
    SETTINGS index_granularity = 8192;
