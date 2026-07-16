package com.dotcms.rest.api.v1.system.cache;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import org.immutables.value.Value;

import java.util.List;

/**
 * Top-level cache statistics view combining JVM memory info
 * with per-provider cache statistics.
 *
 * @author hassandotcms
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = CacheStatsView.class)
@JsonDeserialize(as = CacheStatsView.class)
@Schema(description = "Cache statistics including cluster/server identity, JVM memory, and per-provider region stats")
public interface AbstractCacheStatsView {

    @Schema(
            description = "Cluster identifier (shared across all nodes in the cluster)",
            example = "b1c2d3e4-f5a6-7890-abcd-ef1234567890",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String clusterId();

    @Schema(
            description = "Server identifier (unique per node — identifies which node reported these stats)",
            example = "a9f3c1d2-e456-7890-bcde-f12345678901",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String serverId();

    @Schema(
            description = "Server hostname",
            example = "dotcms-node-1.example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String serverName();

    @Schema(
            description = "JVM memory statistics",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    JvmMemoryView memory();

    @Schema(
            description = "Per-provider cache statistics",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<CacheProviderStatsView> providers();
}
