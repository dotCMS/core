import { DotClusterNode } from '@dotcms/dotcms-models';

/** A node that answered, with every section filled in. */
export function createNode(overrides: Partial<DotClusterNode> = {}): DotClusterNode {
    return {
        serverId: '10b10fa5-full-id',
        displayServerId: '10b10fa5',
        name: 'dotcms-demo-56cc78659b-kgzws',
        ipAddress: '10.2.66.19',
        version: '26.09.18-01',
        lastContactSeconds: 4,
        responded: true,
        status: 'UP',
        cache: {
            clusterName: 'dotcms-demo',
            transport: 'PubSub',
            numberOfNodes: 3,
            open: true,
            address: null,
            port: null,
            receivedBytes: 181314,
            sentBytes: 175002
        },
        search: {
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
            path: '/data/shared/assets',
            canRead: true,
            canWrite: true
        },
        ...overrides
    };
}

/** A node that did not answer: identity only, every section value empty. */
export function createDownNode(overrides: Partial<DotClusterNode> = {}): DotClusterNode {
    return createNode({
        serverId: 'e93aa517-full-id',
        displayServerId: 'e93aa517',
        name: 'dotcms-demo-56cc78659b-x8nvt',
        ipAddress: '10.2.66.31',
        version: '26.09.17-04',
        lastContactSeconds: 720,
        responded: false,
        status: 'DOWN',
        cache: null,
        search: null,
        assets: null,
        ...overrides
    });
}
