import { TiptapFloatingMenuDirective } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Button } from 'primeng/button';

import { Editor } from '@tiptap/core';
import { PluginKey } from '@tiptap/pm/state';

@Component({
    selector: 'dot-add-button',
    template: `
        <div
            tiptapFloatingMenu
            [editor]="$editor()"
            [pluginKey]="pluginKey"
            [options]="floatingOptions">
            <p-button
                class="add-button flex  items-center justify-center cursor-pointer"
                (onClick)="onClick()"
                size="small"
                variant="text"
                icon="pi pi-plus" />
        </div>
    `,
    styleUrls: ['./dot-add-button.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [TiptapFloatingMenuDirective, Button]
})
export class DotAddButtonComponent {
    $editor = input.required<Editor>({ alias: 'editor' });
    protected readonly pluginKey = new PluginKey('dotCMSPlusButton');
    // ngx-tiptap v14 swapped tippy for floating-ui — `placement`/`strategy` map to floating-ui;
    // `strategy: 'fixed'` is the closest equivalent to the previous `appendTo: document.body`
    // (positions relative to viewport so the button isn't clipped by the editor container).
    protected readonly floatingOptions = {
        placement: 'left' as const,
        strategy: 'fixed' as const
    };

    protected onClick(): void {
        this.$editor().chain().focus().insertContent('/').run();
    }
}
