import { Component, computed, Input } from '@angular/core';
import { BlockEditorNode } from '@dotcms/types';

import { Destination } from '../../../../../types/contentlet.model';

@Component({
    selector: 'app-destination',
    standalone: true,
    template: `
    <div class="w-full p-4 my-2 bg-white rounded-lg border border-slate-400">
        <h4 class="text-lg font-bold">{{ contentlet().title }}</h4>
        <span class="text-sm text-blue-500">{{ contentlet().contentType }}</span>
    </div>
  `,
})
export class DestinationComponent {
    @Input() node!: BlockEditorNode;

    contentlet = computed(() => {
        return this.node.attrs?.['data'] as Destination;
    });
}

