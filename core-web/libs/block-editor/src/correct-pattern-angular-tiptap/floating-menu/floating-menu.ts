import { TiptapEditorDirective, TiptapFloatingMenuDirective } from 'ngx-tiptap';

import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

@Component({
    selector: 'dot-floating-menu',
    imports: [CommonModule, FormsModule, TiptapEditorDirective, TiptapFloatingMenuDirective],
    templateUrl: './floating-menu.html',
    styleUrls: ['./floating-menu.css']
})
export class FloatingMenu implements OnDestroy {
    value =
        'This is an example of a Medium-like editor. Enter a new line and some buttons will appear.';

    editor = new Editor({
        extensions: [StarterKit],
        editorProps: {
            attributes: {
                class: 'p-2 border-black focus:border-blue-700 border-2 rounded-md outline-hidden',
                spellCheck: 'false'
            }
        }
    });

    ngOnDestroy(): void {
        this.editor.destroy();
    }
}
