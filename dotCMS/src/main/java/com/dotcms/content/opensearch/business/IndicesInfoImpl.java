package com.dotcms.content.opensearch.business;

import com.dotcms.content.elasticsearch.business.IndexType;
import com.dotcms.content.elasticsearch.business.IndicesInfo;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Modern immutable implementation for tracking search indices with versioning support.
 *
 * This class provides an immutable, thread-safe way to manage search index information
 * with enhanced features including:
 * - Version tracking for index evolution
 * - Modern Java time API support
 * - Immutable data structures
 * - Builder pattern for flexible construction
 *
 * Index naming convention: cluster_<CLUSTER_ID>.<INDEX_TYPE_PREFIX>_<TIMESTAMP>_v<VERSION>
 *
 * @author fabrizio
 */
public final class IndicesInfoImpl implements IndicesInfo {

    // Modern timestamp format for better readability and sortability

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final String CLUSTER_PREFIX = "cluster_";
    private static final String INDEX_NAME_PATTERN = CLUSTER_PREFIX + "%s.%s_%s_v%s";

    // Immutable fields
    private final String live;
    private final String working;
    private final String reindexLive;
    private final String reindexWorking;
    private final String siteSearch;
    private final String version;

    /**
     * Private constructor - use Builder
     */
    private IndicesInfoImpl(Builder builder) {
        this.live = builder.live;
        this.working = builder.working;
        this.reindexLive = builder.reindexLive;
        this.reindexWorking = builder.reindexWorking;
        this.siteSearch = builder.siteSearch;
        this.version = builder.version;
    }

    /**
     * Create a builder for ModernIndicesInfo
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a copy builder from existing instance
     */
    public Builder toBuilder() {
        return new Builder()
            .live(this.live)
            .working(this.working)
            .reindexLive(this.reindexLive)
            .reindexWorking(this.reindexWorking)
            .siteSearch(this.siteSearch)
            .version(this.version);
    }

    // Getters

    public String live() {
        return live;
    }

    public String working() {
        return working;
    }

    public String reindexLive() {
        return reindexLive;
    }

    public String reindexWorking() {
        return reindexWorking;
    }

    public String siteSearch() {
        return siteSearch;
    }

    public String version() {
        return version;
    }

    // IndicesInfo interface implementation

    @Override
    public String getLive() {
        return live;
    }

    @Override
    public String getWorking() {
        return working;
    }

    @Override
    public String getReindexLive() {
        return reindexLive;
    }

    @Override
    public String getReindexWorking() {
        return reindexWorking;
    }

    @Override
    public String getSiteSearch() {
        return siteSearch;
    }

    @Override
    public long getIndexTimeStamp(IndexType indexType) {
        Optional<String> indexName = getIndexNameForType(indexType);
        if (indexName.isEmpty()) {
            throw new DotRuntimeException("No index found for type: " + indexType);
        }

        try {
            String timestamp = extractTimestampFromIndexName(indexName.get());
            Date startTime = simpleDateFormat.parse(timestamp);
            return System.currentTimeMillis() - startTime.getTime();
        } catch (ParseException e) {
            Logger.error(IndicesInfoImpl.class, "Error parsing timestamp from index name: " + indexName, e);
            throw new DotRuntimeException("Failed to parse timestamp from index: " + indexName, e);
        }
    }

    @Override
    public String createNewIndicesName(IndexType... indexTypes) {
        String timestamp = simpleDateFormat.format(LocalDateTime.now());

        Builder builder = toBuilder();

        for (IndexType indexType : indexTypes) {
            String indexName = String.format(
                INDEX_NAME_PATTERN,
                ClusterFactory.getClusterId(),
                indexType.getPrefix(),
                timestamp,
                version
            );

            // Update builder with new index name
            switch (indexType) {
                case LIVE:
                    builder.live(indexName);
                    break;
                case WORKING:
                    builder.working(indexName);
                    break;
                case REINDEX_LIVE:
                    builder.reindexLive(indexName);
                    break;
                case REINDEX_WORKING:
                    builder.reindexWorking(indexName);
                    break;
                case SITE_SEARCH:
                    builder.siteSearch(indexName);
                    break;
                default:
                    Logger.warn(IndicesInfoImpl.class, "Unknown index type: " + indexType);
            }
        }

        return timestamp;
    }

    @Override
    public Map<IndexType, String> asMap() {
        Map<IndexType, String> result = new EnumMap<>(IndexType.class);

        if (UtilMethods.isSet(live)) {
            result.put(IndexType.LIVE, live);
        }
        if (UtilMethods.isSet(working)) {
            result.put(IndexType.WORKING, working);
        }
        if (UtilMethods.isSet(reindexLive)) {
            result.put(IndexType.REINDEX_LIVE, reindexLive);
        }
        if (UtilMethods.isSet(reindexWorking)) {
            result.put(IndexType.REINDEX_WORKING, reindexWorking);
        }
        if (UtilMethods.isSet(siteSearch)) {
            result.put(IndexType.SITE_SEARCH, siteSearch);
        }

        return result;
    }

    // Modern helper methods

    /**
     * Get index name for a specific type
     */
    public String getIndexName(IndexType indexType) {
        switch (indexType) {
            case LIVE: return live;
            case WORKING: return working;
            case REINDEX_LIVE: return reindexLive;
            case REINDEX_WORKING: return reindexWorking;
            case SITE_SEARCH: return siteSearch;
            default: return null;
        }
    }

    /**
     * Create a new version of this indices configuration
     */
    public IndicesInfoImpl withVersion(String newVersion) {
        return toBuilder()
            .version(newVersion)
            .build();
    }

    /**
     * Check if index name is modern format (includes version)
     */
    public static boolean isModernIndexName(String indexName) {
        return UtilMethods.isSet(indexName) && indexName.contains("_v") &&
               indexName.matches(".*_v\\d+$");
    }

    // Private helper methods

    private Optional<String> getIndexNameForType(IndexType indexType) {
        switch (indexType) {
            case LIVE: return Optional.of(live);
            case WORKING: return Optional.of(working);
            case REINDEX_LIVE: return Optional.of(reindexLive);
            case REINDEX_WORKING: return Optional.of(reindexWorking);
            case SITE_SEARCH: return Optional.of(siteSearch);

        }
        return Optional.empty();
    }

    private String extractTimestampFromIndexName(String indexName) {
        if (isModernIndexName(indexName)) {
            // Modern format: cluster_id.prefix_timestamp_vVersion
            String withoutVersion = indexName.substring(0, indexName.lastIndexOf("_v"));
            return withoutVersion.substring(withoutVersion.lastIndexOf("_") + 1);
        } else {
            // Legacy format: cluster_id.prefix_timestamp
            return indexName.substring(indexName.lastIndexOf("_") + 1);
        }
    }

    // Object methods

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        IndicesInfoImpl that = (IndicesInfoImpl) obj;

        return Objects.equals(version, that.version) &&
               Objects.equals(live, that.live) &&
               Objects.equals(working, that.working) &&
               Objects.equals(reindexLive, that.reindexLive) &&
               Objects.equals(reindexWorking, that.reindexWorking) &&
               Objects.equals(siteSearch, that.siteSearch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(live, working, reindexLive, reindexWorking, siteSearch, version);
    }

    @Override
    public String toString() {
        return "ModernIndicesInfo{" +
               "live='" + live + '\'' +
               ", working='" + working + '\'' +
               ", reindexLive='" + reindexLive + '\'' +
               ", reindexWorking='" + reindexWorking + '\'' +
               ", siteSearch='" + siteSearch + '\'' +
               ", version=" + version +
               '}';
    }

    /**
     * Builder pattern for ModernIndicesInfo
     */
    public static final class Builder {
        private String live;
        private String working;
        private String reindexLive;
        private String reindexWorking;
        private String siteSearch;
        private String version = OPEN_SEARCH_VERSION; // Default version

        public Builder() {}

        public Builder live(String live) {
            this.live = live;
            return this;
        }

        public Builder working(String working) {
            this.working = working;
            return this;
        }

        public Builder reindexLive(String reindexLive) {
            this.reindexLive = reindexLive;
            return this;
        }

        public Builder reindexWorking(String reindexWorking) {
            this.reindexWorking = reindexWorking;
            return this;
        }

        public Builder siteSearch(String siteSearch) {
            this.siteSearch = siteSearch;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Set all indices with a specific version
         */
        public Builder withVersionedIndices(Map<IndexType, String> indices, String version) {
            indices.forEach((type, name) -> {
                switch (type) {
                    case LIVE:
                        live(name);
                        break;
                    case WORKING:
                        working(name);
                        break;
                    case REINDEX_LIVE:
                        reindexLive(name);
                        break;
                    case REINDEX_WORKING:
                        reindexWorking(name);
                        break;
                    case SITE_SEARCH:
                        siteSearch(name);
                        break;
                }
            });
            return version(version);
        }

        /**
         * Build the ModernIndicesInfo instance
         */
        public IndicesInfoImpl build() {
            return new IndicesInfoImpl(this);
        }
    }
}