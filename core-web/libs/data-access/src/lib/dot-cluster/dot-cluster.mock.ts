import { DotClusterNode, DotClusterNodes } from '@dotcms/dotcms-models';

/**
 * Stand-in for `GET /api/v1/cluster/nodes` until the backend ships it. Remove together with the
 * `of(...)` in `DotClusterService#getNodes`.
 *
 * Mirrors the design's three-node cluster (two Up, one Lagging: it answered with a stale
 * heartbeat) and adds a node that did not answer, so every state the portlet draws is visible.
 */

const CLUSTER_NAME = 'dotcms-demo';

function answeringNode(
    overrides: Pick<
        DotClusterNode,
        'serverId' | 'displayServerId' | 'name' | 'ipAddress' | 'version'
    > &
        Partial<DotClusterNode>
): DotClusterNode {
    return {
        lastContactSeconds: 4,
        responded: true,
        status: 'UP',
        cache: {
            clusterName: CLUSTER_NAME,
            transport: 'PubSub',
            numberOfNodes: 3,
            open: true,
            address: null,
            port: null,
            receivedBytes: 181314,
            sentBytes: 175002
        },
        search: {
            engine: 'OPENSEARCH',
            clusterName: 'opensearch-ovh-east',
            timedOut: false,
            numberOfNodes: 3,
            numberOfDataNodes: 3,
            activePrimaryShards: 293,
            activeShards: 589,
            relocatingShards: 0,
            initializingShards: 0,
            unassignedShards: 0,
            delayedUnassignedShards: 0,
            numberOfPendingTasks: 0,
            numberOfInFlightFetch: 0,
            taskMaxWaitingInQueueMillis: 0,
            activeShardsPercent: 100
        },
        assets: {
            path: '/data/shared/assets',
            canRead: true,
            canWrite: true
        },
        ...overrides
    };
}

export const DOT_CLUSTER_NODES_MOCK: DotClusterNodes = {
    currentServerId: '10b10fa5-7d2c-4c51-9a3e-2f6c1b8e0d41',
    nodes: [
        answeringNode({
            serverId: '10b10fa5-7d2c-4c51-9a3e-2f6c1b8e0d41',
            displayServerId: '10b10fa5',
            name: 'dotcms-demo-56cc78659b-kgzws',
            ipAddress: '10.2.66.19',
            version: '26.09.18-01'
        }),
        answeringNode({
            serverId: '7c41d0b2-1a9e-4f0b-8d6a-5e2f3c7b9a10',
            displayServerId: '7c41d0b2',
            name: 'dotcms-demo-56cc78659b-pq4mz',
            ipAddress: '10.2.66.24',
            version: '26.09.18-01',
            lastContactSeconds: 2
        }),
        answeringNode({
            serverId: 'e93aa517-4b6d-4e2a-9c1f-8a7d6e5c4b32',
            displayServerId: 'e93aa517',
            name: 'dotcms-demo-56cc78659b-x8nvt',
            ipAddress: '10.2.66.31',
            version: '26.09.17-04',
            lastContactSeconds: 360,
            status: 'LAGGING'
        }),
        {
            serverId: 'b2f19c84-6e3a-4d7b-a0c5-1f8e2d9b7c63',
            displayServerId: 'b2f19c84',
            name: 'dotcms-demo-56cc78659b-r2lkd',
            ipAddress: '10.2.66.40',
            version: '26.09.18-01',
            lastContactSeconds: 540,
            responded: false,
            status: 'DOWN',
            cache: null,
            search: null,
            assets: null
        }
    ]
};
