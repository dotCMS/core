package com.dotcms.rest.api.v1.cluster;

/**
 * Status of a server in the cluster, computed on the backend from whether it answered this call's
 * status request and how fresh its heartbeat is (see {@link ClusterNodeMapper#status}).
 *
 * @author hassandotcms
 */
public enum NodeStatus {
    /** Answered, and its heartbeat is fresh. */
    UP,
    /** Answered with a stale heartbeat, or didn't answer but its heartbeat is fresh. */
    LAGGING,
    /** Didn't answer and its heartbeat is stale, or answered with a failed assets check. */
    DOWN
}
