import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    signal
} from '@angular/core';

import { Editor, JSONContent } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

/**
 * Mirrors the **new** block editor ({@link DotCMSEditorComponent}) for compare-view tests:
 * content set via `[value]` is applied with `emitUpdate: false`, so it dispatches a TipTap
 * `transaction` but never emits `valueChange`. `valueChange` fires only on real user edits
 * (`update`). The compare component must rely on the `transaction` — not `valueChange` — to
 * populate the diff on load (issue #36550).
 */
@Component({
    selector: 'dot-block-editor',
    imports: [],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: '<div>Block Editor Mock</div>'
})
export class BlockEditorMockComponent implements OnInit {
    @Input() value: JSONContent;
    readonly editor = signal<Editor | null>(null);
    @Output() valueChange = new EventEmitter<JSONContent>();

    ngOnInit() {
        const editor = new Editor({
            extensions: [StarterKit]
        });

        editor.on('create', () => {
            if (this.value) {
                editor.commands.setContent(this.value, { emitUpdate: false });
            }
        });

        editor.on('update', () => {
            this.valueChange.emit(editor.getJSON());
        });

        this.editor.set(editor);
    }
}
