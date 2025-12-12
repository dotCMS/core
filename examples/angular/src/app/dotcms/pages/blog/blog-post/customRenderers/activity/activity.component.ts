import { Component, computed, Input } from '@angular/core';
import { BlockEditorNode } from '@dotcms/types';

import { Activity } from '../../../../../types/contentlet.model';

@Component({
  selector: 'app-activity',
  template: `
    <div class="w-full p-4 my-2 bg-white rounded-lg border border-slate-400">
        <h4 class="text-lg font-bold">{{ contentlet().title }}</h4>
        <p class="line-clamp-2">{{ contentlet().description }}</p>
        <span class="text-sm text-cyan-700">{{ contentlet().contentType }}</span>
    </div>
  `,
  standalone: true,
})
export class ActivityComponent {
  @Input() node!: BlockEditorNode;

  contentlet = computed(() => {
    return this.node.attrs?.['data'] as Activity;
  });
}
