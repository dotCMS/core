package com.dotcms.rest.api.v1.cluster;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The cache transport as one server reports it. A {@code null} value means unknown or not
 * applicable.
 *
 * @author hassandotcms
 */
@Schema(description = "Cache transport details of a server; a null value means unknown or not applicable")
public record CacheSectionView(
        @Schema(description = "Cache cluster name", nullable = true)
        String clusterName,
        @Schema(description = "Cache transport", example = "PubSub", nullable = true)
        String transport,
        @Schema(description = "Number of servers the transport sees", nullable = true)
        Integer numberOfNodes,
        @Schema(description = "Whether the transport is open", nullable = true)
        Boolean open,
        @Schema(description = "Transport address; null for transports without one, such as PubSub", nullable = true)
        String address,
        @Schema(description = "Transport port; null for transports without one, such as PubSub", nullable = true)
        Integer port,
        @Schema(description = "Bytes received", nullable = true)
        Long receivedBytes,
        @Schema(description = "Bytes sent", nullable = true)
        Long sentBytes) {
}
