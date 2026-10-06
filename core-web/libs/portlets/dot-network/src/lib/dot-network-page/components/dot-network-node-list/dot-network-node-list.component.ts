import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BadgeModule } from 'primeng/badge';
import { ListboxModule } from 'primeng/listbox';
import { TagModule } from 'primeng/tag';

import { DotClusterNode } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotNetworkElapsedPipe } from '../../../utils/dot-network-elapsed.pipe';
import { NODE_STATUS_TAGS } from '../../../utils/dot-network.utils';

/** Left pane: every cluster node in a single-selection `p-listbox`. */
@Component({
    selector: 'dot-network-node-list',
    imports: [
        FormsModule,
        BadgeModule,
        ListboxModule,
        TagModule,
        DotMessagePipe,
        DotNetworkElapsedPipe
    ],
    templateUrl: './dot-network-node-list.component.html',
    host: { class: 'flex min-h-0 flex-col' }
})
export class DotNetworkNodeListComponent {
    readonly nodes = input.required<DotClusterNode[]>();
    readonly selectedServerId = input<string | null>(null);
    readonly currentServerId = input<string>('');

    readonly selectNode = output<string>();

    protected readonly statusTags = NODE_STATUS_TAGS;

    // The list fills the pane, so the listbox drops its own frame: the pane's border already
    // separates it from the detail, and a second rounded border inside it would read as a nested box.
    protected readonly listboxPt = {
        root: { class: 'flex! flex-col! flex-1! min-h-0! border-0! rounded-none!' },
        listContainer: { class: 'flex-1! min-h-0!' }
    };

    /**
     * Clicking the selected option again makes a single-selection listbox emit `null`. A node
     * stays selected at all times, so that click is ignored.
     */
    protected onChange(serverId: string | null): void {
        if (serverId) {
            this.selectNode.emit(serverId);
        }
    }
}
