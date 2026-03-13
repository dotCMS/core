package com.dotcms.content.index.opensearch;

/**
 * Enumeration of all OpenSearch configuration properties, each paired with its
 * Elasticsearch fallback key.
 *
 * <p>This enum is the single source of truth for property names used in the
 * OpenSearch index layer. It enables the fallback resolution logic in
 * {@link com.dotcms.content.index.IndexConfigHelper}: when an {@code OS_*}
 * property is not set in the environment, the helper automatically retries with
 * the corresponding {@code ES_*} key before returning the hard-coded default.</p>
 *
 * <h2>Fallback rules</h2>
 * <ol>
 *   <li>Load the {@link #osKey} from {@link com.dotmarketing.util.Config}.</li>
 *   <li>If absent and {@link #esFallback} is non-null, load the fallback key.</li>
 *   <li>If still absent, return the caller-supplied default value.</li>
 * </ol>
 *
 * <h2>OS-only properties</h2>
 * <p>Entries whose {@link #esFallback} is {@code null} have no Elasticsearch
 * equivalent (e.g. mTLS certificates, connection-pool tuning). For these,
 * step 2 is skipped and the caller default is used directly.</p>
 *
 * @see com.dotcms.content.index.IndexConfigHelper
 */
public enum OSIndexProperty {

    // -------------------------------------------------------------------------
    // Connection / network
    // -------------------------------------------------------------------------

    /** OpenSearch host. Falls back to {@code ES_HOSTNAME}. */
    HOSTNAME("OS_HOSTNAME", "ES_HOSTNAME"),

    /** HTTP protocol ({@code http} or {@code https}). Falls back to {@code ES_PROTOCOL}. */
    PROTOCOL("OS_PROTOCOL", "ES_PROTOCOL"),

    /** Port. Falls back to {@code ES_PORT}. */
    PORT("OS_PORT", "ES_PORT"),

    /** Number of connection attempts before giving up. Falls back to {@code ES_CONNECTION_ATTEMPTS}. */
    CONNECTION_ATTEMPTS("OS_CONNECTION_ATTEMPTS", "ES_CONNECTION_ATTEMPTS"),

    /** Connection establishment timeout in milliseconds. No ES equivalent. */
    CONNECTION_TIMEOUT("OS_CONNECTION_TIMEOUT", null),

    /** Retry sleep delay in seconds between connection attempts. No ES equivalent. */
    CONNECTION_RETRY_SLEEP_SECONDS("OS_CONNECTION_RETRY_SLEEP_SECONDS", null),

    /** Socket read timeout in milliseconds. No ES equivalent. */
    SOCKET_TIMEOUT("OS_SOCKET_TIMEOUT", null),

    /** Maximum total HTTP connections in the pool. No ES equivalent. */
    MAX_CONNECTIONS("OS_MAX_CONNECTIONS", null),

    /** Maximum HTTP connections per route. No ES equivalent. */
    MAX_CONNECTIONS_PER_ROUTE("OS_MAX_CONNECTIONS_PER_ROUTE", null),

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------

    /** Auth mechanism ({@code BASIC}, {@code JWT}, {@code CERT}). Falls back to {@code ES_AUTH_TYPE}. */
    AUTH_TYPE("OS_AUTH_TYPE", "ES_AUTH_TYPE"),

    /** Basic-auth username. Falls back to {@code ES_AUTH_BASIC_USER}. */
    AUTH_BASIC_USER("OS_AUTH_BASIC_USER", "ES_AUTH_BASIC_USER"),

    /** Basic-auth password. Falls back to {@code ES_AUTH_BASIC_PASSWORD}. */
    AUTH_BASIC_PASSWORD("OS_AUTH_BASIC_PASSWORD", "ES_AUTH_BASIC_PASSWORD"),

    /** Bearer token for JWT auth. Falls back to {@code ES_AUTH_JWT_TOKEN}. */
    AUTH_JWT_TOKEN("OS_AUTH_JWT_TOKEN", "ES_AUTH_JWT_TOKEN"),

    // -------------------------------------------------------------------------
    // TLS
    // -------------------------------------------------------------------------

    /** Enable TLS. Falls back to {@code ES_TLS_ENABLED}. */
    TLS_ENABLED("OS_TLS_ENABLED", "ES_TLS_ENABLED"),

    /** Trust self-signed certificates. No ES equivalent. */
    TLS_TRUST_SELF_SIGNED("OS_TLS_TRUST_SELF_SIGNED", null),

    /** Path to the PEM client certificate for mTLS. No ES equivalent. */
    TLS_CLIENT_CERT("OS_TLS_CLIENT_CERT", null),

    /** Path to the PEM client private key for mTLS. No ES equivalent. */
    TLS_CLIENT_KEY("OS_TLS_CLIENT_KEY", null),

    /** Path to the PEM CA certificate used to verify the server. No ES equivalent. */
    TLS_CA_CERT("OS_TLS_CA_CERT", null),

    // -------------------------------------------------------------------------
    // Index management
    // -------------------------------------------------------------------------

    /**
     * Timeout for index operations (e.g. {@code "15s"}).
     * Falls back to {@code ES_INDEX_OPERATIONS_TIMEOUT}.
     */
    INDEX_OPERATIONS_TIMEOUT("OS_INDEX_OPERATIONS_TIMEOUT", "ES_INDEX_OPERATIONS_TIMEOUT"),

    /** Default query field for new indices. Falls back to {@code ES_INDEX_QUERY_DEFAULT_FIELD}. */
    INDEX_QUERY_DEFAULT_FIELD("OS_INDEX_QUERY_DEFAULT_FIELD", "ES_INDEX_QUERY_DEFAULT_FIELD"),

    /** Number of replicas. Falls back to {@code ES_INDEX_REPLICAS}. */
    INDEX_REPLICAS("OS_INDEX_REPLICAS", "ES_INDEX_REPLICAS"),

    /** Number of primary shards. Falls back to {@code es.index.number_of_shards}. */
    INDEX_NUMBER_OF_SHARDS("opensearch.index.number_of_shards", "es.index.number_of_shards"),

    /** Number of replicas */
    INDEX_AUTO_EXPAND_REPLICAS("OS_INDEX_AUTO_EXPAND_REPLICAS","ES_INDEX_AUTO_EXPAND_REPLICAS"),

    /** Suffix appended to generated index names. Falls back to {@code ES_INDEX_NAME}. */
    INDEX_NAME("OS_INDEX_NAME", "ES_INDEX_NAME"),

    // -------------------------------------------------------------------------
    // Scroll API
    // -------------------------------------------------------------------------

    /** How long the scroll context is kept alive (minutes). Falls back to {@code ES_SCROLL_KEEP_ALIVE_MINUTES}. */
    SCROLL_KEEP_ALIVE_MINUTES("OS_SCROLL_KEEP_ALIVE_MINUTES", "ES_SCROLL_KEEP_ALIVE_MINUTES"),

    /** Number of documents per scroll page. Falls back to {@code ES_SCROLL_BATCH_SIZE}. */
    SCROLL_BATCH_SIZE("OS_SCROLL_BATCH_SIZE", "ES_SCROLL_BATCH_SIZE"),

    // -------------------------------------------------------------------------
    // Bulk API
    // -------------------------------------------------------------------------

    /** Timeout for bulk index operations in milliseconds. Falls back to {@code ES_BULK_TIMEOUT}. */
    BULK_TIMEOUT("OS_BULK_TIMEOUT", "ES_BULK_TIMEOUT"),

    /** Number of documents per bulk request. Falls back to {@code ES_BULK_BATCH_SIZE}. */
    BULK_BATCH_SIZE("OS_BULK_BATCH_SIZE", "ES_BULK_BATCH_SIZE"),

    // -------------------------------------------------------------------------
    // Query behaviour
    // -------------------------------------------------------------------------

    /**
     * Use filter context instead of query context when sorting is non-score-based.
     * Falls back to {@code ELASTICSEARCH_USE_FILTERS_FOR_SEARCHING}.
     */
    USE_FILTERS_FOR_SEARCHING("OS_USE_FILTERS_FOR_SEARCHING", "ELASTICSEARCH_USE_FILTERS_FOR_SEARCHING"),

    /**
     * Maximum number of hits to track accurately.
     * No ES equivalent — OpenSearch-specific feature.
     */
    TRACK_TOTAL_HITS("OS_TRACK_TOTAL_HITS", null),

    /**
     * Enable in-memory caching of search query results.
     * No ES equivalent.
     */
    CACHE_SEARCH_QUERIES("OS_CACHE_SEARCH_QUERIES", null);

    // -------------------------------------------------------------------------

    /** The OpenSearch-specific configuration key ({@code OS_*} or {@code opensearch.*}). */
    public final String osKey;

    /**
     * The Elasticsearch fallback key, or {@code null} when no ES equivalent exists.
     * When {@code null}, resolution stops at step 1 and the caller default is used.
     */
    public final String esFallback;

    OSIndexProperty(final String osKey, final String esFallback) {
        this.osKey = osKey;
        this.esFallback = esFallback;
    }
}