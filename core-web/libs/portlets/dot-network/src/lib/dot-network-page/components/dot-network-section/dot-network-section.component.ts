import { Component, computed, input } from '@angular/core';

import { PanelModule } from 'primeng/panel';
import { TagModule } from 'primeng/tag';

import { DotClusterHealth } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { HEALTH_TAGS } from '../../../utils/dot-network.utils';

/** One label/value row. A `null` value renders as an em dash. */
export interface DotNetworkSectionRow {
    readonly labelKey: string;
    readonly value: string | null;
    /** Monospace, for addresses and paths. */
    readonly mono?: boolean;
}

/**
 * Card for one area of a node's health (Cache Transport, Search Cluster, Assets): a title, an
 * optional subtitle, the section's health tag and its label/value rows.
 */
@Component({
    selector: 'dot-network-section',
    imports: [PanelModule, TagModule, DotMessagePipe],
    templateUrl: './dot-network-section.component.html',
    host: { class: 'block' }
})
export class DotNetworkSectionComponent {
    readonly titleKey = input.required<string>();
    readonly subtitle = input<string | null>(null);
    readonly health = input.required<DotClusterHealth>();
    readonly rows = input.required<DotNetworkSectionRow[]>();

    protected readonly $healthTag = computed(() => HEALTH_TAGS[this.health()]);
}
