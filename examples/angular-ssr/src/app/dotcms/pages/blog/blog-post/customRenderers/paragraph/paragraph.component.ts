import { Component, Input, OnInit, signal } from '@angular/core';
import { BlockEditorNode } from '@dotcms/types';

@Component({
  selector: 'app-paragraph',
  standalone: true,
  imports: [],
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
  @Input() content!: BlockEditorNode;

  protected $text = signal<string>('');

  ngOnInit() {
    if (!this.content.content) {
      return;
    }

    const [{ text }] = this.content.content;
    this.$text.set(text ?? '');
  }
}
