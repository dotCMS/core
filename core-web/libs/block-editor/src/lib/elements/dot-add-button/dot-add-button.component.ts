import { TiptapFloatingMenuDirective } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, input } from '@angular/core';

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
            <button
                class="add-button flex  items-center justify-center cursor-pointer"
                (click)="onClick()">
                <span class="pi pi-plus"></span>
            </button>
        </div>
    `,
    styleUrls: ['./dot-add-button.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [TiptapFloatingMenuDirective]
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
