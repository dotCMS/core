package com.dotcms.content.index.domain;

import com.dotcms.annotations.Nullable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Vendor-neutral immutable representation of statistics for a single cluster node.
 *
 * <p>Replaces {@code com.dotcms.content.elasticsearch.business.NodeStats} in the
 * public {@link com.dotcms.content.elasticsearch.business.IndexAPI} contract.</p>
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableNodeStats.class)
@JsonDeserialize(as = ImmutableNodeStats.class)
public interface NodeStats {

    /** Returns the node name. */
    @Value.Default
    default String name() {
        return "unknown";
    }

    /** Returns the node host address. */
    @Value.Default
    default String host() {
        return "unknown";
    }

    /** Returns the node transport address. */
    @Value.Default
    default String transportAddress() {
        return "unknown";
    }

    /** Returns {@code true} if this node is the cluster master. */
    boolean master();

    /** Returns the total document count across all indices on this node. */
    long docCount();

    /** Returns the raw storage size in bytes. */
    long sizeRaw();

    /** Returns the human-readable formatted storage size (e.g., "1.2gb"). */
    String size();

    static ImmutableNodeStats.Builder builder() {
        return ImmutableNodeStats.builder();
    }
}
