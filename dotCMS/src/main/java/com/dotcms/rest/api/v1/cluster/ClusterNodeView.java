package com.dotcms.rest.api.v1.cluster;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One server of the cluster. Identity always comes from the server's database row, so it is
 * filled whether or not the server answered; the sections are {@code null} when it didn't.
 *
 * @author hassandotcms
 */
@Schema(description = "One server of the cluster")
public record ClusterNodeView(
        @Schema(description = "Full server id", requiredMode = Schema.RequiredMode.REQUIRED)
        String serverId,
        @Schema(description = "Short id shown in the server list", requiredMode = Schema.RequiredMode.REQUIRED)
        String displayServerId,
        @Schema(description = "Friendly name, shown as the host name", nullable = true)
        String name,
        @Schema(description = "Cluster id the server belongs to (cluster_server.cluster_id)", nullable = true)
        String clusterName,
        @Schema(description = "IP address", nullable = true)
        String ipAddress,
        @Schema(description = "dotCMS version the server runs", nullable = true)
        String version,
        @Schema(description = "Seconds since the server's last heartbeat; never null, never negative",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long lastContactSeconds,
        @Schema(description = "True when the server answered this call's status request within the wait window",
                requiredMode = Schema.RequiredMode.REQUIRED)
        boolean responded,
        @Schema(description = "UP: answered with a fresh heartbeat. LAGGING: only one of the two. "
                + "DOWN: neither, or answered with a failed assets check", requiredMode = Schema.RequiredMode.REQUIRED)
        NodeStatus status,
        @Schema(description = "Cache details; null when the server didn't answer", nullable = true)
        CacheSectionView cache,
        @Schema(description = "Search engine details; null when the server didn't answer", nullable = true)
        SearchSectionView search,
        @Schema(description = "Shared assets folder details; null when the server didn't answer", nullable = true)
        AssetsSectionView assets) {
}
