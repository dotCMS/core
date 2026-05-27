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
    <aside class="callout">
      <span class="callout__icon" aria-hidden="true">💡</span>
      <p class="callout__text">{{ text }}</p>
    </aside>
  `,
  styles: `
    .callout {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      margin: 1rem 0;
      padding: 1rem 1.25rem;
      border-left: 4px solid hotpink;
      border-radius: 0.5rem;
      background: rgba(255, 105, 180, 0.08);
    }

    .callout__icon {
      font-size: 1.25rem;
      line-height: 1.5;
    }

    .callout__text {
      margin: 0;
      font-style: italic;
      color: #9d174d;
    }
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
