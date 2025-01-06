CREATE DATABASE IF NOT EXISTS clickhouse_test_db;
CREATE TABLE IF NOT EXISTS clickhouse_test_db.events
(
    _timestamp               DateTime,
    api_key                  String,
    cluster_id               String,
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
