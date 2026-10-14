import { Component, input } from '@angular/core';

import { PanelModule } from 'primeng/panel';

import { DotMessagePipe } from '@dotcms/ui';

/** One label/value row. A `null` value renders as an em dash. */
export interface DotNetworkSectionRow {
    readonly labelKey: string;
    readonly value: string | null;
    /** Monospace, for addresses and paths. */
    readonly mono?: boolean;
}

/**
 * Card for one area of a node (Cache Transport, Search Cluster, Assets): a title, an optional
 * subtitle and its label/value rows. Sections carry no health indicator (spec FR-034).
 */
@Component({
    selector: 'dot-network-section',
    imports: [PanelModule, DotMessagePipe],
    templateUrl: './dot-network-section.component.html',
    host: { class: 'block' }
})
export class DotNetworkSectionComponent {
    readonly titleKey = input.required<string>();
    readonly subtitle = input<string | null>(null);
    readonly rows = input.required<DotNetworkSectionRow[]>();
}
