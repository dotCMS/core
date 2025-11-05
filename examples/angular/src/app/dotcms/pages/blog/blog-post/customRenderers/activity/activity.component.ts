import { Component, computed, Input } from '@angular/core';
import { BlockEditorNode } from '@dotcms/types';

// You can define the type of the contentlet in the component
interface Activity {
  title: string;
  description: string;
}

@Component({
  selector: 'app-activity',
  template: '<div>{{ contentlet().title }}</div>',
  standalone: true,
  styles: `
    :host {
      display: block;
    }
  `,
})
export class ActivityComponent {
  @Input() node!: BlockEditorNode;

  contentlet = computed(() => {
    return this.node.attrs?.['data'] as Activity;
  });
}
