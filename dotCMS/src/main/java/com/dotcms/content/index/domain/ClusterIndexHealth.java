package com.dotcms.content.index.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Vendor-neutral immutable representation of the health state of a single index within a cluster.
 *
 * <p>Replaces {@code org.elasticsearch.cluster.health.ClusterIndexHealth} in the
 * public {@link com.dotcms.content.elasticsearch.business.IndexAPI} contract.</p>
 *
 * @author Fabrizio Araya
 */
@Value.Immutable
@JsonSerialize(as = ImmutableClusterIndexHealth.class)
@JsonDeserialize(as = ImmutableClusterIndexHealth.class)
public interface ClusterIndexHealth {

    /** Returns the number of primary shards for this index. */
    int numberOfShards();

    /** Returns the number of replica shards for this index. */
    int numberOfReplicas();

    /**
     * Returns the health status as a string (e.g., "GREEN", "YELLOW", "RED").
     * Used by UI to render a coloured indicator.
     */
    String status();

    static ImmutableClusterIndexHealth.Builder builder() {
        return ImmutableClusterIndexHealth.builder();
    }

    /**
     * Creates a {@code ClusterIndexHealth} from an Elasticsearch {@code ClusterIndexHealth}.
     *
     * @param esHealth the Elasticsearch cluster index health
     * @return a vendor-neutral ClusterIndexHealth instance
     */
    static ClusterIndexHealth from(
            org.elasticsearch.cluster.health.ClusterIndexHealth esHealth) {
        return builder()
                .numberOfShards(esHealth.getNumberOfShards())
                .numberOfReplicas(esHealth.getNumberOfReplicas())
                .status(esHealth.getStatus() != null ? esHealth.getStatus().name() : "n/a")
                .build();
    }
}
