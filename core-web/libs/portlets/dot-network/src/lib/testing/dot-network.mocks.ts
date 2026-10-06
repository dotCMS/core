import { DotClusterNode } from '@dotcms/dotcms-models';

/** A node that answered, with every section filled in. */
export function createNode(overrides: Partial<DotClusterNode> = {}): DotClusterNode {
    return {
        serverId: '10b10fa5-full-id',
        displayServerId: '10b10fa5',
        licenseId: 'xxxx-xxxx-4f2a',
        name: 'dotcms-demo-56cc78659b-kgzws',
        host: 'dotcms-demo-host',
        ipAddress: '10.2.66.19',
        version: '26.09.18-01',
        lastContactSeconds: 4,
        responded: true,
        status: 'UP',
        cache: {
            health: 'GREEN',
            clusterName: 'dotcms-demo',
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
            health: 'YELLOW',
            engine: null,
            clusterName: 'opensearch-ovh-east',
            timedOut: false,
            numberOfNodes: 3,
            numberOfDataNodes: 2,
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

/** A node that did not answer: identity only, every section value empty. */
export function createDownNode(overrides: Partial<DotClusterNode> = {}): DotClusterNode {
    const base = createNode();

    return createNode({
        serverId: 'e93aa517-full-id',
        displayServerId: 'e93aa517',
        name: 'dotcms-demo-56cc78659b-x8nvt',
        ipAddress: '10.2.66.31',
        version: '26.09.17-04',
        lastContactSeconds: 720,
        responded: false,
        status: 'DOWN',
        cache: {
            ...base.cache,
            health: 'UNKNOWN',
            clusterName: null,
            transport: null,
            provider: null,
            numberOfNodes: null,
            open: null,
            address: null,
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
        assets: { health: 'UNKNOWN', path: null, canRead: null, canWrite: null },
        ...overrides
    });
}
