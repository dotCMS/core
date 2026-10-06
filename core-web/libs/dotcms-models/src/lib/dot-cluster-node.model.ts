/**
 * Cluster node models for the Network portlet.
 *
 * These mirror the response of the typed `GET /api/v1/cluster/nodes` endpoint proposed by the
 * Network spike (#37802, §2.8). That endpoint does not exist yet: until it does,
 * `DotClusterService` serves this shape from mock data. Every node — whether it answered the
 * status request or not — has the same shape; a node that did not answer has
 * `responded: false`, section health `UNKNOWN` and section values `null`.
 */

/** Overall state of a node, computed by the backend. Shown as the badge on its card. */
export type DotClusterNodeStatus = 'UP' | 'LAGGING' | 'DOWN';

/** Health of one section (cache, search, assets). `UNKNOWN` when the node did not answer. */
export type DotClusterHealth = 'GREEN' | 'YELLOW' | 'RED' | 'UNKNOWN';

/** Search engine serving reads in the current OpenSearch migration phase. */
export type DotClusterSearchEngine = 'ELASTICSEARCH' | 'OPENSEARCH';

export interface DotClusterCacheInfo {
    readonly health: DotClusterHealth;
    readonly clusterName: string | null;
    /** Transport type, e.g. `PubSub`. */
    readonly transport: string | null;
    /** Pub/sub provider, e.g. `JDBCPubSubImpl`. */
    readonly provider: string | null;
    readonly numberOfNodes: number | null;
    readonly open: boolean | null;
    /** Transport address. `null` when the transport has none (PubSub). */
    readonly address: string | null;
    /** `null` when the transport does not listen on a port (PubSub, Null). */
    readonly port: number | null;
    readonly receivedBytes: number | null;
    readonly sentBytes: number | null;
    readonly receivedMessages: number | null;
    readonly sentMessages: number | null;
}

export interface DotClusterSearchInfo {
    readonly health: DotClusterHealth;
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
    readonly health: DotClusterHealth;
    /** Shared assets volume path. */
    readonly path: string | null;
    readonly canRead: boolean | null;
    readonly canWrite: boolean | null;
}

export interface DotClusterNode {
    readonly serverId: string;
    readonly displayServerId: string;
    /** Masked license serial. */
    readonly licenseId: string;
    /** Server friendly name. */
    readonly name: string;
    readonly host: string;
    readonly ipAddress: string;
    /** dotCMS release version running on the node. */
    readonly version: string;
    /** Seconds since the node last wrote its heartbeat. `null` when it never did. */
    readonly lastContactSeconds: number | null;
    /** `false` when the node did not answer the cluster status request in time. */
    readonly responded: boolean;
    readonly status: DotClusterNodeStatus;
    readonly cache: DotClusterCacheInfo;
    readonly search: DotClusterSearchInfo;
    readonly assets: DotClusterAssetsInfo;
}

/** `entity` of `GET /api/v1/cluster/nodes`. */
export interface DotClusterNodes {
    /** `false` when the license does not allow cluster data. `nodes` is then empty. */
    readonly licensed: boolean;
    /** Id of the node that served the request. */
    readonly currentServerId: string;
    /** `GREEN` when every node seen in the last heartbeat window answered. */
    readonly clusterHealth: DotClusterHealth;
    readonly nodes: DotClusterNode[];
}
