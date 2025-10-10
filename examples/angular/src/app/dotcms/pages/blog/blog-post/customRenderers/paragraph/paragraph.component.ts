import { Component, Input, OnInit, signal } from '@angular/core';
import { BlockEditorNode } from '@dotcms/types';

@Component({
  selector: 'app-paragraph',
  standalone: true,
  template: `
    <p>
      {{ $text() }}
    </p>
  `,
  styles: `
    p {
      font-size: 16px;
      line-height: 1.5;
      color: blue;
    }
  `,
})
export class ParagraphComponent implements OnInit {
  @Input() node!: BlockEditorNode;

  protected $text = signal<string>('');

  ngOnInit() {
    if (!this.node.content) {
      return;
    }

    const [{ text }] = this.node.content;
    this.$text.set(text ?? '');
  }
}
