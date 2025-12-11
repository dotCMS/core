import { Component, computed, Input } from '@angular/core';
import { BlockEditorNode } from '@dotcms/types';

import { Product } from '../../../../../types/contentlet.model';

@Component({
  selector: 'app-product',
  template: `
    <div class="w-full p-4 my-2 bg-white rounded-lg border border-slate-400">
        <h4 class="text-lg font-bold">{{ contentlet().title }}</h4>
        <span class="text-sm text-slate-500">{{ contentlet().contentType }}</span>
    </div>
  `,
  standalone: true,
})
export class ProductComponent {
  @Input() node!: BlockEditorNode;

  contentlet = computed(() => {
    return this.node.attrs?.['data'] as Product;
  });
}
