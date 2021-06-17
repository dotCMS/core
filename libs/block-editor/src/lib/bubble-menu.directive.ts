import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Editor } from '@tiptap/core';
import { BubbleMenuPlugin, BubbleMenuPluginKey, BubbleMenuPluginProps } from '@tiptap/extension-bubble-menu';

@Directive({
  selector: 'tiptap-bubble-menu[editor], [tiptapBubbleMenu][editor]'
})
export class BubbleMenuDirective implements OnInit, OnDestroy {
  @Input() editor!: Editor;
  @Input() tippyOptions: BubbleMenuPluginProps['tippyOptions'] = {};

  constructor(private _el: ElementRef<HTMLElement>) { }

  ngOnInit(): void {
    if (!this.editor) {
      throw new Error('Required: Input `editor`');
    }

    this.editor.registerPlugin(BubbleMenuPlugin({
      editor: this.editor,
      element: this._el.nativeElement,
      tippyOptions: this.tippyOptions
    }));
  }

  ngOnDestroy(): void {
    this.editor.unregisterPlugin(BubbleMenuPluginKey);
  }
}
