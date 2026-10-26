package com.dotcms.content.index.domain;

import com.dotcms.content.index.IndexAPI;
import com.dotmarketing.util.Config;
import org.immutables.value.Value;

/**
 * Vendor-neutral health of the search engine that serves reads, as returned by
 * {@link IndexAPI#getClusterHealthSummary()}.
 *
 * <p>Unlike {@link ClusterIndexHealth}, which describes one index, this covers all of this
 * install's indices (the cluster prefix). The status, shard counts and active-shards percentage
 * describe only those indices; the cluster name, node counts, pending tasks and delayed unassigned
 * shards stay cluster-wide. When the cluster holds none of this install's indices, the status is
 * {@code green} with zero shards. Each implementation stamps the {@link Engine} that produced it,
 * so a caller can tell which engine actually answered, including when the phase 2 read falls back
 * from OpenSearch to Elasticsearch.</p>
 *
 * <p>Each attempt of the health call gives up after {@value #TIMEOUT_PROPERTY} milliseconds
 * (default {@value #DEFAULT_TIMEOUT_MILLIS}). The limit is per attempt, not per call: after a
 * failure the search client tries the next configured host, and in phase 2 a failed OpenSearch
 * call is followed by the Elasticsearch fallback. With one host per engine a hung engine still
 * leaves the report inside the requester's wait window; when every configured host hangs, or both
 * engines hang in phase 2, the call can take a multiple of the limit and the server may be listed
 * as not responding for that check.</p>
 *
 * @author hassandotcms
 */
@Value.Immutable
public interface SearchClusterHealth {

    /** Config key for the client-side time limit, in milliseconds, of each attempt of the health call. */
    String TIMEOUT_PROPERTY = "CLUSTER_SEARCH_HEALTH_TIMEOUT_MILLIS";

    /** Default client-side time limit of each attempt of the health call, in milliseconds. */
    int DEFAULT_TIMEOUT_MILLIS = 750;

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

    /**
     * Returns the client-side time limit of each attempt of the health call, read from {@link #TIMEOUT_PROPERTY} on
     * every call. A value of zero or less would mean "no limit" to the HTTP clients, so it falls
     * back to {@link #DEFAULT_TIMEOUT_MILLIS}.
     *
     * @return the time limit in milliseconds, always greater than zero
     */
    static int timeoutMillis() {
        final int configured = Config.getIntProperty(TIMEOUT_PROPERTY, DEFAULT_TIMEOUT_MILLIS);
        return configured > 0 ? configured : DEFAULT_TIMEOUT_MILLIS;
    }

    /** Returns a builder for a cluster health summary. */
    static ImmutableSearchClusterHealth.Builder builder() {
        return ImmutableSearchClusterHealth.builder();
    }
}
