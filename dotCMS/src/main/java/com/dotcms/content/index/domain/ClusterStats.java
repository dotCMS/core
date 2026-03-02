package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

/**
 * Vendor-neutral immutable representation of cluster-wide statistics.
 *
 * <p>Replaces {@code com.dotcms.content.elasticsearch.business.ClusterStats} in the
 * public {@link com.dotcms.content.elasticsearch.business.IndexAPI} contract.</p>
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableClusterStats.class)
@JsonDeserialize(as = ImmutableClusterStats.class)
public interface ClusterStats {

    /** Returns the name of the cluster. */
    String clusterName();

    /** Returns the list of per-node statistics. */
    List<NodeStats> nodeStats();

    static ImmutableClusterStats.Builder builder() {
        return ImmutableClusterStats.builder();
    }
}
