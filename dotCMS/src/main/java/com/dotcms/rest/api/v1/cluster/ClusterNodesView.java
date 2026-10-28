package com.dotcms.rest.api.v1.cluster;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The servers of the cluster, as the Network portlet shows them.
 *
 * @param currentServerId the id of the server that handled the request
 * @param nodes           every alive server, in database order (startup time, then server id)
 * @author hassandotcms
 */
@Schema(description = "The servers of the cluster with their status")
public record ClusterNodesView(
        @Schema(description = "Id of the server that handled the request", requiredMode = Schema.RequiredMode.REQUIRED)
        String currentServerId,
        @Schema(description = "Every server with a recent heartbeat, ordered by startup time, then server id",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<ClusterNodeView> nodes) {
}
