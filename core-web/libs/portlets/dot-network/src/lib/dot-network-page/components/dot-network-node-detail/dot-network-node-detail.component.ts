import { Component, computed, inject, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotClusterNode } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { NODE_STATUS_TAGS } from '../../../utils/dot-network.utils';
import {
    DotNetworkSectionComponent,
    DotNetworkSectionRow
} from '../dot-network-section/dot-network-section.component';

const SEARCH_ENGINE_LABEL_KEYS = {
    ELASTICSEARCH: 'network.search.engine.elasticsearch',
    OPENSEARCH: 'network.search.engine.opensearch'
} as const;

function text(value: string | number | boolean | null): string | null {
    return value === null ? null : String(value);
}

/**
 * Right pane: the selected node's identity, its summary tiles and its Cache Transport,
 * Search Cluster and Assets sections.
 */
@Component({
    selector: 'dot-network-node-detail',
    imports: [
        ButtonModule,
        CardModule,
        MessageModule,
        TagModule,
        DotMessagePipe,
        DotNetworkSectionComponent
    ],
    templateUrl: './dot-network-node-detail.component.html',
    host: { class: 'flex min-h-0 flex-col' }
})
export class DotNetworkNodeDetailComponent {
    readonly #messageService = inject(DotMessageService);

    readonly node = input.required<DotClusterNode>();
    readonly isCurrent = input(false);
    readonly refreshing = input(false);

    readonly refresh = output<void>();

    protected readonly $statusTag = computed(() => NODE_STATUS_TAGS[this.node().status]);

    protected readonly $tiles = computed(() => {
        const node = this.node();

        return [
            { labelKey: 'network.tile.server-id', value: node.displayServerId, mono: true },
            { labelKey: 'network.tile.cluster', value: node.cache.clusterName, mono: false },
            { labelKey: 'network.tile.ip-address', value: node.ipAddress, mono: true },
            { labelKey: 'network.tile.version', value: node.version, mono: false }
        ];
    });

    protected readonly $cacheSubtitle = computed(() => {
        const clusterName = this.node().cache.clusterName;

        return clusterName ? this.#messageService.get('network.cache.subtitle', clusterName) : null;
    });

    protected readonly $cacheRows = computed<DotNetworkSectionRow[]>(() => {
        const { cache, responded } = this.node();
        const traffic =
            cache.receivedBytes === null && cache.sentBytes === null
                ? null
                : `${cache.receivedBytes ?? '—'} / ${cache.sentBytes ?? '—'}`;
        // On a node that answered, a missing address or port means the transport has none
        // (PubSub, Null). On a node that did not answer, the value is simply unknown.
        const notApplicable = responded
            ? this.#messageService.get('network.value.not-applicable')
            : null;

        return [
            { labelKey: 'network.cache.cluster-name', value: cache.clusterName },
            { labelKey: 'network.cache.transport', value: cache.transport },
            { labelKey: 'network.cache.number-of-nodes', value: text(cache.numberOfNodes) },
            { labelKey: 'network.cache.channel-open', value: text(cache.open) },
            {
                labelKey: 'network.cache.cluster-address',
                value: cache.address ?? notApplicable,
                mono: true
            },
            { labelKey: 'network.cache.received-sent', value: traffic },
            { labelKey: 'network.cache.port', value: text(cache.port) ?? notApplicable }
        ];
    });

    protected readonly $searchSubtitle = computed(() => {
        const engine = this.node().search.engine;

        return engine ? this.#messageService.get(SEARCH_ENGINE_LABEL_KEYS[engine]) : null;
    });

    protected readonly $searchRows = computed<DotNetworkSectionRow[]>(() => {
        const search = this.node().search;
        const percent =
            search.activeShardsPercent === null
                ? null
                : `${Math.round(search.activeShardsPercent * 10) / 10}%`;

        return [
            { labelKey: 'network.search.cluster-name', value: search.clusterName },
            { labelKey: 'network.search.timed-out', value: text(search.timedOut) },
            { labelKey: 'network.search.number-of-nodes', value: text(search.numberOfNodes) },
            {
                labelKey: 'network.search.number-of-data-nodes',
                value: text(search.numberOfDataNodes)
            },
            {
                labelKey: 'network.search.active-primary-shards',
                value: text(search.activePrimaryShards)
            },
            { labelKey: 'network.search.active-shards', value: text(search.activeShards) },
            { labelKey: 'network.search.relocating-shards', value: text(search.relocatingShards) },
            {
                labelKey: 'network.search.initializing-shards',
                value: text(search.initializingShards)
            },
            { labelKey: 'network.search.unassigned-shards', value: text(search.unassignedShards) },
            {
                labelKey: 'network.search.delayed-unassigned-shards',
                value: text(search.delayedUnassignedShards)
            },
            {
                labelKey: 'network.search.pending-tasks',
                value: text(search.numberOfPendingTasks)
            },
            {
                labelKey: 'network.search.unfinished-fetches',
                value: text(search.numberOfInFlightFetch)
            },
            {
                labelKey: 'network.search.max-queue-wait',
                value:
                    search.taskMaxWaitingInQueueMillis === null
                        ? null
                        : this.#messageService.get(
                              'network.value.milliseconds',
                              String(search.taskMaxWaitingInQueueMillis)
                          )
            },
            { labelKey: 'network.search.active-shards-percent', value: percent }
        ];
    });

    protected readonly $assetsRows = computed<DotNetworkSectionRow[]>(() => {
        const assets = this.node().assets;

        return [
            { labelKey: 'network.assets.shared-path', value: assets.path, mono: true },
            { labelKey: 'network.assets.read', value: text(assets.canRead) },
            { labelKey: 'network.assets.write', value: text(assets.canWrite) }
        ];
    });
}
