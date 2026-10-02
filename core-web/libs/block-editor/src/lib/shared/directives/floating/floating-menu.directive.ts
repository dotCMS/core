import { Props as TippyProps } from 'tippy.js';

import { Directive, ElementRef, OnDestroy, OnInit, inject, input } from '@angular/core';

import { Editor } from '@tiptap/core';
import { FloatingMenuPlugin, FloatingMenuPluginProps } from '@tiptap/extension-floating-menu';

@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tiptap-floating-menu[editor], [tiptapFloatingMenu][editor]',
    standalone: false
})
export class FloatingMenuDirective implements OnInit, OnDestroy {
    readonly pluginKey = input<FloatingMenuPluginProps['pluginKey']>('NgxTiptapFloatingMenu');
    readonly editor = input.required<Editor>();
    // v3 dropped `tippyOptions` from FloatingMenuPluginProps; type against tippy directly.
    readonly tippyOptions = input<Partial<TippyProps>>({});
    readonly shouldShow = input<FloatingMenuPluginProps['shouldShow']>(null);

    private readonly _el = inject(ElementRef<HTMLElement>);

    ngOnInit(): void {
        const editor = this.editor();
        if (!editor) {
            throw new Error('Required: Input `editor`');
        }

        // v3 dropped tippyOptions from the public type; cast and pass through.
        editor.registerPlugin(
            FloatingMenuPlugin({
                pluginKey: this.pluginKey(),
                editor: editor,
                element: this._el.nativeElement,
                shouldShow: this.shouldShow(),
                tippyOptions: this.tippyOptions()
            } as unknown as FloatingMenuPluginProps)
        );
    }

    ngOnDestroy(): void {
        this.editor().unregisterPlugin(this.pluginKey());
    }
}
