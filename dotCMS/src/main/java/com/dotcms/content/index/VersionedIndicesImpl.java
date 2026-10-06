package com.dotcms.content.index;

import com.dotcms.content.elasticsearch.business.IndexType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
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
     * The version associated with these indices
     * Default value is OPENSEARCH_3X
     */
    @Nullable
    @Value.Default
    public String version() {
        return OPENSEARCH_3X;
    }

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
     * Creates a builder pre-filled with every slot of an existing record — the safe starting point
     * for a partial update.
     *
     * <p><strong>Prefer this over {@link #builder()} when rebuilding a record that already exists.</strong>
     * {@code VersionedIndicesAPI.saveIndices} is a delete-by-version followed by a re-insert, so a
     * slot the builder omits is not left alone — it is <em>erased</em>. Starting from scratch
     * therefore means every rebuild has to remember to re-list every slot it does not mean to touch,
     * including the ones it does not own: {@code siteSearch} lives in the same version row as the
     * content pointers but belongs to {@code SiteSearchAPI}, and content-side rebuilds that forgot it
     * silently deactivated the active Site Search index (issue #36360).</p>
     *
     * <p>Copying first and clearing explicitly inverts that: forgetting a slot preserves it, and
     * clearing one is something the code has to say out loud, via the {@link Optional} overloads on
     * {@link Builder}:</p>
     * <pre>{@code
     * VersionedIndicesImpl.builder(existing)
     *         .working(promotedWorking)
     *         .reindexWorking(Optional.empty())   // cleared on purpose, and it shows
     *         .build();
     * }</pre>
     *
     * @param from the record to copy every slot (and the version) from
     * @return a builder holding {@code from}'s state, ready to be overridden slot by slot
     */
    public static Builder builder(VersionedIndices from) {
        return ImmutableVersionedIndicesImpl.builder().from(from);
    }

    /**
     * Builder interface for creating ModernIndicesInfoImpl instances.
     *
     * <p>Each slot has an {@link Optional} overload alongside the plain one: passing
     * {@link Optional#empty()} clears the slot. On a builder started from {@link #builder()} that is
     * a no-op, but on one started from {@link #builder(VersionedIndices)} it is how a slot is
     * deliberately dropped — see that method for why rebuilds should start from the existing
     * record.</p>
     */
    public interface Builder {
        Builder live(String live);
        Builder live(Optional<String> live);
        Builder working(String working);
        Builder working(Optional<String> working);
        Builder reindexLive(String reindexLive);
        Builder reindexLive(Optional<String> reindexLive);
        Builder reindexWorking(String reindexWorking);
        Builder reindexWorking(Optional<String> reindexWorking);
        Builder siteSearch(String siteSearch);
        Builder siteSearch(Optional<String> siteSearch);
        Builder version(String version);
        VersionedIndicesImpl build();
    }
}