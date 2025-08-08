import { TiptapFloatingMenuDirective } from 'ngx-tiptap';

import { Component, input } from '@angular/core';

import { Editor, isNodeEmpty } from '@tiptap/core';
import { EditorState, PluginKey } from '@tiptap/pm/state';

@Component({
    selector: 'dot-add-button',
    template: `
        <div
            tiptapFloatingMenu
            [editor]="editor()"
            [pluginKey]="pluginKey"
            [tippyOptions]="tippyOptions"
            [shouldShow]="shouldShow">
            <button class="add-button" (click)="onClick()">
                <span class="pi pi-plus"></span>
            </button>
        </div>
    `,
    styleUrls: ['./dot-add-button.component.scss'],
    standalone: true,
    imports: [TiptapFloatingMenuDirective]
})
export class DotAddButtonComponent {
    editor = input.required<Editor>();
    protected readonly pluginKey = new PluginKey('dotCMSPlusButton');
    protected readonly tippyOptions = {
        placement: 'left',
        appendTo: () => document.body
    };

    protected onClick(): void {
        this.editor().chain().focus().insertContent('/').run();
    }

    protected shouldShow({ editor, state }: { editor: Editor; state: EditorState }) {
        if (!editor.isEditable) return false;
        const { empty, $from } = state.selection;
        if (!empty) return false;

        for (let depth = $from.depth; depth >= 0; depth--) {
            const node = $from.node(depth);
            const type = node.type?.name;
            if (type === 'paragraph' || type === 'heading') {
                return isNodeEmpty(node);
            }
        }
        return false;
    }
}
