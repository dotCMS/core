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
            [tippyOptions]="tippyOptions">
            <p-button
                class="add-button flex  items-center justify-center cursor-pointer"
                (onClick)="onClick()"
                size="small"
                variant="text"
                icon="pi pi-plus" />
        </div>
    `,
    styleUrls: ['./dot-add-button.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [TiptapFloatingMenuDirective, Button]
})
export class DotAddButtonComponent {
    $editor = input.required<Editor>({ alias: 'editor' });
    protected readonly pluginKey = new PluginKey('dotCMSPlusButton');
    protected readonly tippyOptions = {
        placement: 'left',
        appendTo: () => document.body
    };

    protected onClick(): void {
        this.$editor().chain().focus().insertContent('/').run();
    }
}
