import { Component, Input } from '@angular/core';

import { DotCMSBlockEditorRendererNativeComponent } from '@dotcms/angular';
import { BlockEditorNode } from '@dotcms/types';

/**
 * Custom renderer for the built-in `bulletList` block.
 *
 * Demonstrates overriding a default block type via `customRenderers`: it renders a
 * real <ul> with hot-pink, oversized bullets and delegates each list item's inner
 * content back to the native renderer so nested formatting (bold, links, etc.)
 * still works.
 */
@Component({
  selector: 'app-bullet-list',
  standalone: true,
  imports: [DotCMSBlockEditorRendererNativeComponent],
  template: `
    <ul class="custom-bullet-list">
      @for (item of items; track $index) {
        <li>
          <dotcms-block-editor-renderer-native [blocks]="asDoc(item.content)" />
        </li>
      }
    </ul>
  `,
  styles: `
    .custom-bullet-list {
      list-style: none;
      padding-left: 1.5rem;
    }

    .custom-bullet-list > li {
      position: relative;
      margin: 0.25rem 0;
    }

    /* The custom "bullet": a big hot-pink dot you can't miss. */
    .custom-bullet-list > li::before {
      content: '';
      position: absolute;
      left: -1.25rem;
      top: 0.45em;
      width: 0.6rem;
      height: 0.6rem;
      border-radius: 9999px;
      background: hotpink;
      box-shadow: 0 0 0 3px rgba(255, 105, 180, 0.25);
    }
  `,
})
export class BulletListComponent {
  @Input() node!: BlockEditorNode;

  /** The `listItem` children of this bullet list. */
  protected get items(): BlockEditorNode[] {
    return this.node?.content ?? [];
  }

  /** Wraps a list item's content in a `doc` node the native renderer can render. */
  protected asDoc(content: BlockEditorNode[] | undefined): BlockEditorNode {
    return { type: 'doc', content: content ?? [] };
  }
}
