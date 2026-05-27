import { Component, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

/**
 * Custom renderer for the built-in `blockquote` block.
 *
 * Demonstrates the natural custom-block use case: it renders **self-contained**
 * markup (a styled "💡 callout" card) from the node's content, without
 * re-rendering its children through the renderer. Because it doesn't delegate
 * back to the SDK, the output stays a clean single element with no wrapper
 * elements.
 */
@Component({
  selector: 'app-callout',
  standalone: true,
  template: `
    <aside
      class="flex items-start gap-3 my-4 py-4 px-5 rounded-lg border-l-4 border-pink-500 bg-pink-50">
      <span class="text-xl leading-6" aria-hidden="true">💡</span>
      <p class="m-0 italic text-pink-800">{{ text }}</p>
    </aside>
  `,
})
export class CalloutComponent {
  @Input() node!: BlockEditorNode;

  /** Flattens all descendant text nodes into a single string. */
  protected get text(): string {
    return this.flatten(this.node?.content).trim();
  }

  private flatten(nodes: BlockEditorNode[] | undefined): string {
    return (nodes ?? [])
      .map((node) => node.text ?? this.flatten(node.content))
      .join('');
  }
}
