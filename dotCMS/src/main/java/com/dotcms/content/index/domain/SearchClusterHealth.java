package com.dotcms.content.index.domain;

import com.dotcms.content.index.IndexAPI;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Vendor-neutral, whole-cluster health of the search engine that serves reads, as returned by
 * {@link IndexAPI#getClusterHealthSummary()}.
 *
 * <p>Unlike {@link ClusterIndexHealth}, which describes one index, this describes the cluster as a
 * whole: node and shard counts, pending work and overall status. Each implementation stamps the
 * {@link Engine} that produced it, so a caller can tell which engine actually answered, including
 * when the phase 2 read falls back from OpenSearch to Elasticsearch.</p>
 *
 * @author hassandotcms
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSearchClusterHealth.class)
@JsonDeserialize(as = ImmutableSearchClusterHealth.class)
public interface SearchClusterHealth {

    /** The search engine a health summary came from. */
    enum Engine {
        ELASTICSEARCH,
        OPENSEARCH
    }

    /** Returns the engine that produced this summary. */
    Engine engine();

    /** Returns the search cluster's name. */
    String clusterName();

    /** Returns the cluster status in lower case: {@code green}, {@code yellow} or {@code red}. */
    String status();

    /** Returns whether the health request timed out on the server before the cluster settled. */
    boolean timedOut();

    /** Returns the number of nodes in the cluster. */
    int numberOfNodes();

    /** Returns the number of data nodes in the cluster. */
    int numberOfDataNodes();

    /** Returns the number of active primary shards. */
    int activePrimaryShards();

    /** Returns the number of active shards, primaries and replicas. */
    int activeShards();

    /** Returns the number of shards being relocated. */
    int relocatingShards();

    /** Returns the number of shards being initialized. */
    int initializingShards();

    /** Returns the number of unassigned shards. */
    int unassignedShards();

    /** Returns the number of unassigned shards whose allocation is delayed. */
    int delayedUnassignedShards();

    /** Returns the number of cluster-level tasks waiting to run. */
    int numberOfPendingTasks();

    /** Returns the number of unfinished shard fetches. */
    int numberOfInFlightFetch();

    /** Returns how long the oldest pending task has waited, in milliseconds. */
    long taskMaxWaitingInQueueMillis();

    /** Returns the percentage of active shards, from 0 to 100. */
    double activeShardsPercent();

    /** Returns a builder for a cluster health summary. */
    static ImmutableSearchClusterHealth.Builder builder() {
        return ImmutableSearchClusterHealth.builder();
    }
}
