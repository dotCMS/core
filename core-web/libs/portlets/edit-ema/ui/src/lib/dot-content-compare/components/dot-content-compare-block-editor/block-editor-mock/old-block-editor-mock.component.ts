import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output
} from '@angular/core';

import { Editor, JSONContent } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

/**
 * Mirrors the **legacy** block editor ({@link DotBlockEditorComponent}) for compare-view tests:
 * `editor` is a plain property (not a signal) and content set through `ngModel` emits
 * `valueChange` on the initial load. This is the trigger the compare component relies on for the
 * flag-OFF (legacy) path.
 */
@Component({
    selector: 'dot-old-block-editor',
    imports: [],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: '<div>Old Block Editor Mock</div>'
})
export class OldBlockEditorMockComponent implements OnInit {
    @Input() value: JSONContent;
    editor: Editor;
    @Output() valueChange = new EventEmitter<JSONContent>();

    ngOnInit() {
        const editor = new Editor({
            extensions: [StarterKit]
        });

        editor.on('create', () => {
            if (this.value) {
                editor.commands.setContent(this.value, { emitUpdate: false });
            }

            // Legacy editor emits valueChange once ngModel applies the initial content.
            this.valueChange.emit(editor.getJSON());
        });

        this.editor = editor;
    }
}
