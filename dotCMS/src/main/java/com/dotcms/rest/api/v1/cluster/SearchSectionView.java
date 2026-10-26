package com.dotcms.rest.api.v1.cluster;

import com.dotcms.content.index.domain.SearchClusterHealth;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Health of the search engine that serves reads, as one server reports it. When the server
 * answered but couldn't reach its search engine, every value is {@code null}.
 *
 * @author hassandotcms
 */
@Schema(description = "Search engine health as a server reports it; every value is null when its search check failed")
public record SearchSectionView(
        @Schema(description = "Engine that answered; null when unknown", nullable = true)
        SearchClusterHealth.Engine engine,
        @Schema(description = "Search cluster name", nullable = true)
        String clusterName,
        @Schema(description = "Whether the health request timed out", nullable = true)
        Boolean timedOut,
        @Schema(nullable = true) Integer numberOfNodes,
        @Schema(nullable = true) Integer numberOfDataNodes,
        @Schema(nullable = true) Integer activePrimaryShards,
        @Schema(nullable = true) Integer activeShards,
        @Schema(nullable = true) Integer relocatingShards,
        @Schema(nullable = true) Integer initializingShards,
        @Schema(nullable = true) Integer unassignedShards,
        @Schema(nullable = true) Integer delayedUnassignedShards,
        @Schema(nullable = true) Integer numberOfPendingTasks,
        @Schema(nullable = true) Integer numberOfInFlightFetch,
        @Schema(description = "How long the oldest pending task has waited, in milliseconds", nullable = true)
        Long taskMaxWaitingInQueueMillis,
        @Schema(description = "Percentage of active shards, 0 to 100", nullable = true)
        Double activeShardsPercent) {
}
