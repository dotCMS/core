CREATE DATABASE IF NOT EXISTS clickhouse_test_db;
CREATE TABLE IF NOT EXISTS clickhouse_test_db.events
(
    -- ######################################################
    --                  Jitsu Properties
    -- ######################################################
    _timestamp DateTime,
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