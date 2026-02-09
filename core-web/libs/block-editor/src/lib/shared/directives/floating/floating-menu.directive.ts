import { Directive, ElementRef, Input, OnDestroy, OnInit, inject } from '@angular/core';

import { Editor } from '@tiptap/core';
import { FloatingMenuPlugin, FloatingMenuPluginProps } from '@tiptap/extension-floating-menu';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tiptap-floating-menu[editor], [tiptapFloatingMenu][editor]',
    standalone: false
})
export class FloatingMenuDirective implements OnInit, OnDestroy {
    @Input() pluginKey: FloatingMenuPluginProps['pluginKey'] = 'NgxTiptapFloatingMenu';
    @Input() editor!: Editor;
    @Input() tippyOptions: FloatingMenuPluginProps['tippyOptions'] = {};
    @Input() shouldShow: FloatingMenuPluginProps['shouldShow'] = null;

    private readonly _el = inject(ElementRef<HTMLElement>);

    ngOnInit(): void {
        if (!this.editor) {
            throw new Error('Required: Input `editor`');
        }

        this.editor.registerPlugin(
            FloatingMenuPlugin({
                pluginKey: this.pluginKey,
                editor: this.editor,
                element: this._el.nativeElement,
                tippyOptions: this.tippyOptions,
                shouldShow: this.shouldShow
            })
        );
    }

    ngOnDestroy(): void {
        this.editor.unregisterPlugin(this.pluginKey);
    }
}
