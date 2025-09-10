CREATE DATABASE IF NOT EXISTS clickhouse_test_db;
CREATE TABLE IF NOT EXISTS clickhouse_test_db.events
(
    _timestamp               DateTime,
    api_key                  String,
    cluster_id               String,
    customer_name            String,
    customer_category        String,
    environment_name         String,
    environment_version      String,
    customer_id              String,
    doc_encoding             String,
    doc_host                 String,
    doc_path                 String,
    doc_search               String,
    event_type               String,
    eventn_ctx_event_id      String,
    ids_ajs_anonymous_id     String,
    local_tz_offset          Int64,
    page_title               String,
    parsed_ua_device_brand   String,
    parsed_ua_device_family  String,
    parsed_ua_device_model   String,
    parsed_ua_os_family      String,
    parsed_ua_os_version     String,
    parsed_ua_ua_family      String,
    parsed_ua_ua_version     String,
    referer                  String,
    screen_resolution        String,
    source_ip                String,
    src                      String,
    url                      String,
    original_url             String,
    user_agent               String,
    user_anonymous_id        String,
    user_hashed_anonymous_id String,
    userlanguage             String,
    user_language            String,
    utc_time                 DateTime,
    vp_size                  String,
    persona                  String,
    ip                       String,
    experiment               String,
    variant                  String,
    lookbackwindow           String,
    language                 String,
    runningid                String,
    isexperimentpage         Bool,
    istargetpage             Bool,
    ids_fbp                  String,
    ids_ga                   String,
    click_id_gclid           String,
    utm_campaign             String,
    utm_medium               String,
    utm_source               String,
    utm_term                 String,
    utm_content              String
) Engine = MergeTree()
    PARTITION BY customer_id
    ORDER BY (_timestamp, customer_id)
    SETTINGS index_granularity = 8192;

ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_utc_time (utc_time) TYPE minmax GRANULARITY 1;
ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_cluster_id (cluster_id) TYPE minmax GRANULARITY 1;

ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_id String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_title String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_content_type_id String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_content_type_name String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_content_type_var_name String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_response String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_forward_to String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_detail_page_url String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_url String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS comefromvanityurl bool;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS request_id String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS host String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS sessionid String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS sessionnew bool;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS rendermode String;

ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS languageid String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_live String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_working String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS useragent String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS conhost String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS conhostname String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_identifier String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_basetype String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_contenttype String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_contenttypename String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_contenttypeid String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_forwardto String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS object_action String;

ALTER TABLE clickhouse_test_db.events
UPDATE
    object_identifier = if(object_id != '', object_id, object_identifier),
    conhost = if(host != '', host, conhost),
    object_contenttypename = if(object_content_type_name != '', object_content_type_name, object_contenttypename),
    object_contenttypeid = if(object_content_type_id != '', object_content_type_id, object_contenttypeid),
    object_forwardto = if(object_forward_to != '', object_forward_to, object_forwardto),
    object_action = if(object_response != '', object_response, object_action)
WHERE 1=1;

ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_id;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS rendermode;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_content_type_var_name;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS host;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_response;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_content_type_id;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_content_type_name;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_detail_page_url;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_url;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS object_forward_to;
ALTER TABLE clickhouse_test_db.events DROP COLUMN IF EXISTS comefromvanityurl;


ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS customer_name String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS customer_category String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS environment_name String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS environment_version String;

ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS http_response_code UInt16;

ALTER TABLE clickhouse_test_db.events ADD INDEX IF NOT EXISTS idx_request_event (request_id, event_type) TYPE minmax GRANULARITY 1;


ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS context_site_id String;

ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS context_session_id String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS context_site_key String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS context_user_id String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS doc_hash String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS doc_protocol String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS viewport_height String;
ALTER TABLE clickhouse_test_db.events ADD COLUMN IF NOT EXISTS viewport_width String;