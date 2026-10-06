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
        'serverId' | 'displayServerId' | 'name' | 'host' | 'ipAddress' | 'version'
    > &
        Partial<DotClusterNode>
): DotClusterNode {
    return {
        licenseId: 'xxxx-xxxx-4f2a',
        lastContactSeconds: 4,
        responded: true,
        status: 'UP',
        cache: {
            health: 'GREEN',
            clusterName: CLUSTER_NAME,
            transport: 'PubSub',
            provider: 'JDBCPubSubImpl',
            numberOfNodes: 3,
            open: true,
            address: null,
            port: null,
            receivedBytes: 181314,
            sentBytes: 175002,
            receivedMessages: 1204,
            sentMessages: 1187
        },
        search: {
            health: 'GREEN',
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
            health: 'GREEN',
            path: '/data/shared/assets',
            canRead: true,
            canWrite: true
        },
        ...overrides
    };
}

export const DOT_CLUSTER_NODES_MOCK: DotClusterNodes = {
    licensed: true,
    currentServerId: '10b10fa5-7d2c-4c51-9a3e-2f6c1b8e0d41',
    clusterHealth: 'RED',
    nodes: [
        answeringNode({
            serverId: '10b10fa5-7d2c-4c51-9a3e-2f6c1b8e0d41',
            displayServerId: '10b10fa5',
            name: 'dotcms-demo-56cc78659b-kgzws',
            host: 'dotcms-demo-56cc78659b-kgzws',
            ipAddress: '10.2.66.19',
            version: '26.09.18-01'
        }),
        answeringNode({
            serverId: '7c41d0b2-1a9e-4f0b-8d6a-5e2f3c7b9a10',
            displayServerId: '7c41d0b2',
            name: 'dotcms-demo-56cc78659b-pq4mz',
            host: 'dotcms-demo-56cc78659b-pq4mz',
            ipAddress: '10.2.66.24',
            version: '26.09.18-01',
            lastContactSeconds: 2
        }),
        answeringNode({
            serverId: 'e93aa517-4b6d-4e2a-9c1f-8a7d6e5c4b32',
            displayServerId: 'e93aa517',
            name: 'dotcms-demo-56cc78659b-x8nvt',
            host: 'dotcms-demo-56cc78659b-x8nvt',
            ipAddress: '10.2.66.31',
            version: '26.09.17-04',
            lastContactSeconds: 360,
            status: 'LAGGING'
        }),
        {
            serverId: 'b2f19c84-6e3a-4d7b-a0c5-1f8e2d9b7c63',
            displayServerId: 'b2f19c84',
            licenseId: 'xxxx-xxxx-4f2a',
            name: 'dotcms-demo-56cc78659b-r2lkd',
            host: 'dotcms-demo-56cc78659b-r2lkd',
            ipAddress: '10.2.66.40',
            version: '26.09.18-01',
            lastContactSeconds: 540,
            responded: false,
            status: 'DOWN',
            cache: {
                health: 'UNKNOWN',
                clusterName: null,
                transport: null,
                provider: null,
                numberOfNodes: null,
                open: null,
                address: null,
                port: null,
                receivedBytes: null,
                sentBytes: null,
                receivedMessages: null,
                sentMessages: null
            },
            search: {
                health: 'UNKNOWN',
                engine: null,
                clusterName: null,
                timedOut: null,
                numberOfNodes: null,
                numberOfDataNodes: null,
                activePrimaryShards: null,
                activeShards: null,
                relocatingShards: null,
                initializingShards: null,
                unassignedShards: null,
                delayedUnassignedShards: null,
                numberOfPendingTasks: null,
                numberOfInFlightFetch: null,
                taskMaxWaitingInQueueMillis: null,
                activeShardsPercent: null
            },
            assets: { health: 'UNKNOWN', path: null, canRead: null, canWrite: null }
        }
    ]
};
