package com.dotcms.rest.api.v1.cluster;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response envelope for {@code GET /api/v1/cluster/nodes}: the standard {@link ResponseEntityView}
 * around a {@link ClusterNodesView}.
 *
 * @author hassandotcms
 */
public class ResponseEntityClusterNodesView extends ResponseEntityView<ClusterNodesView> {

    /**
     * Wraps the cluster nodes.
     *
     * @param entity the servers of the cluster
     */
    public ResponseEntityClusterNodesView(final ClusterNodesView entity) {
        super(entity);
    }
}
