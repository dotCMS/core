/**
 * Cluster node models for the Network portlet.
 *
 * These mirror the response of the typed `GET /api/v1/cluster/nodes` endpoint proposed by the
 * Network spike (#37802, §2.8). That endpoint does not exist yet: until it does,
 * `DotClusterService` serves this shape from mock data. Every node — whether it answered the
 * status request or not — has the same shape; a node that did not answer has
 * `responded: false` and `cache`, `search` and `assets` set to `null`. Unlicensed installs get a license 403
 * instead of a response.
 */

/** Overall state of a node, computed by the backend. Shown as the badge on its card. */
export type DotClusterNodeStatus = 'UP' | 'LAGGING' | 'DOWN';

/** Search engine serving reads in the current OpenSearch migration phase. */
export type DotClusterSearchEngine = 'ELASTICSEARCH' | 'OPENSEARCH';

export interface DotClusterCacheInfo {
    readonly clusterName: string | null;
    /** Transport type, e.g. `PubSub`. */
    readonly transport: string | null;
    readonly numberOfNodes: number | null;
    readonly open: boolean | null;
    /** Transport address. `null` when the transport has none (PubSub). */
    readonly address: string | null;
    /** `null` when the transport does not listen on a port (PubSub, Null). */
    readonly port: number | null;
    readonly receivedBytes: number | null;
    readonly sentBytes: number | null;
}

export interface DotClusterSearchInfo {
    readonly engine: DotClusterSearchEngine | null;
    readonly clusterName: string | null;
    readonly timedOut: boolean | null;
    readonly numberOfNodes: number | null;
    readonly numberOfDataNodes: number | null;
    readonly activePrimaryShards: number | null;
    readonly activeShards: number | null;
    readonly relocatingShards: number | null;
    readonly initializingShards: number | null;
    readonly unassignedShards: number | null;
    readonly delayedUnassignedShards: number | null;
    readonly numberOfPendingTasks: number | null;
    readonly numberOfInFlightFetch: number | null;
    readonly taskMaxWaitingInQueueMillis: number | null;
    readonly activeShardsPercent: number | null;
}

export interface DotClusterAssetsInfo {
    /** Shared assets volume path. */
    readonly path: string | null;
    readonly canRead: boolean | null;
    readonly canWrite: boolean | null;
}

export interface DotClusterNode {
    readonly serverId: string;
    readonly displayServerId: string;
    /** Server friendly name. `null` when the server's record has none. */
    readonly name: string | null;
    readonly ipAddress: string | null;
    /** dotCMS release version running on the node. */
    readonly version: string | null;
    /** Seconds since the node last wrote its heartbeat. `null` when it never did. */
    readonly lastContactSeconds: number | null;
    /** `false` when the node did not answer the cluster status request in time. */
    readonly responded: boolean;
    readonly status: DotClusterNodeStatus;
    /** `null` when the node did not answer (`responded: false`). */
    readonly cache: DotClusterCacheInfo | null;
    readonly search: DotClusterSearchInfo | null;
    readonly assets: DotClusterAssetsInfo | null;
}

/** `entity` of `GET /api/v1/cluster/nodes`. */
export interface DotClusterNodes {
    /** Id of the node that served the request. */
    readonly currentServerId: string;
    readonly nodes: DotClusterNode[];
}
