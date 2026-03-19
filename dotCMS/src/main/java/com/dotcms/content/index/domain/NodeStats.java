package com.dotcms.content.index.domain;

import com.dotcms.annotations.Nullable;
import com.dotcms.content.index.IndexAPI;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Vendor-neutral immutable representation of statistics for a single cluster node.
 *
 * <p>Replaces {@code com.dotcms.content.elasticsearch.business.NodeStats} in the
 * public {@link IndexAPI} contract.</p>
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableNodeStats.class)
@JsonDeserialize(as = ImmutableNodeStats.class)
public interface NodeStats {

    /** Returns the node name. */
    @Nullable
    String name();

    /** Returns the node host address. */
    @Nullable
    String host();

    /** Returns the node transport address. */
    @Nullable
    String transportAddress();

    /** Returns {@code true} if this node is the cluster master. */
    boolean master();

    /** Returns the total document count across all indices on this node. */
    long docCount();

    /** Returns the raw storage size in bytes. */
    long sizeRaw();

    /** Returns the human-readable formatted storage size (e.g., "1.2gb"). */
    String size();

    @Value.Check
    default NodeStats normalize() {
        if (name() == null || host() == null || transportAddress() == null) {
            return ImmutableNodeStats.builder().from(this)
                    .name(name() != null ? name() : "unknown")
                    .host(host() != null ? host() : "unknown")
                    .transportAddress(transportAddress() != null ? transportAddress() : "unknown")
                    .build();
        }
        return this;
    }

    static ImmutableNodeStats.Builder builder() {
        return ImmutableNodeStats.builder();
    }
}
