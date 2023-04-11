import { Directive, ElementRef, Input, OnDestroy, OnInit, Renderer2 } from '@angular/core';

import { Editor } from '@tiptap/core';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tiptap[editor], [tiptap][editor], tiptap-editor[editor], [tiptapEditor][editor]'
})
export class EditorDirective implements OnInit, OnDestroy {
    @Input() editor!: Editor;

    constructor(private el: ElementRef<HTMLElement>, private _renderer: Renderer2) {}

    ngOnInit(): void {
        if (!this.editor) {
            throw new Error('Required: Input `editor`');
        }

        // insert the editor in the dom
        this.el.nativeElement.appendChild(this.editor.options.element.firstChild as ChildNode);

        // update the options for the editor
        this.editor.setOptions({ element: this.el.nativeElement });
    }

    ngOnDestroy(): void {
        this.editor.destroy();
    }
}
