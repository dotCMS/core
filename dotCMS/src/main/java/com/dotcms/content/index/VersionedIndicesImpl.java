package com.dotcms.content.index;

import com.dotcms.content.elasticsearch.business.IndexType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Immutable implementation of ModernIndicesInfo using Immutables library.
 * This class provides a concrete implementation of the ModernIndicesInfo interface
 * with full immutability support and builder pattern.
 *
 * @author Fabrizzio
 */
@Value.Immutable
@JsonSerialize(as = ImmutableVersionedIndicesImpl.class)
@JsonDeserialize(as = ImmutableVersionedIndicesImpl.class)
public abstract class VersionedIndicesImpl implements VersionedIndices {

    /**
     * The live index name
     */
    public abstract Optional<String> live();

    /**
     * The working index name
     */
    public abstract Optional<String> working();

    /**
     * The reindex live index name
     */
    public abstract Optional<String> reindexLive();

    /**
     * The reindex working index name
     */
    public abstract Optional<String> reindexWorking();

    /**
     * The site search index name
     */
    public abstract Optional<String> siteSearch();

    /**
     * The version associated with these indices
     */
    public abstract Optional<String> version();

    /**
     * Gets the index name for a specific IndexType
     */
    public String getIndexName(IndexType indexType) {
        switch (indexType) {
            case LIVE:
                return live().orElse(null);
            case WORKING:
                return working().orElse(null);
            case REINDEX_LIVE:
                return reindexLive().orElse(null);
            case REINDEX_WORKING:
                return reindexWorking().orElse(null);
            case SITE_SEARCH:
                return siteSearch().orElse(null);
            default:
                return null;
        }
    }

    /**
     * Creates a new builder instance
     */
    public static Builder builder() {
        return ImmutableVersionedIndicesImpl.builder();
    }

    /**
     * Creates a builder from an existing instance
     */
    public static Builder builder(VersionedIndicesImpl from) {
        return ImmutableVersionedIndicesImpl.builder().from(from);
    }

    /**
     * Creates an instance from individual index values with a version
     */
    public static VersionedIndicesImpl fromIndexValues(String live, String working, String reindexLive,
                                                        String reindexWorking, String siteSearch, String version) {
        return builder()
                .live(live)
                .working(working)
                .reindexLive(reindexLive)
                .reindexWorking(reindexWorking)
                .siteSearch(siteSearch)
                .version(version)
                .build();
    }

    /**
     * Builder interface for creating ModernIndicesInfoImpl instances
     */
    public interface Builder {
        Builder live(String live);
        Builder working(String working);
        Builder reindexLive(String reindexLive);
        Builder reindexWorking(String reindexWorking);
        Builder siteSearch(String siteSearch);
        Builder version(String version);
        VersionedIndicesImpl build();
    }
}