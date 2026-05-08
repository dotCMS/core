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
                setTimeout(() => {
                    this.valueChange.emit(editor.getJSON());
                }, 0);
            }
        });

        editor.on('update', () => {
            this.valueChange.emit(editor.getJSON());
        });

        this.editor.set(editor);
    }
}
