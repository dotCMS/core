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

@Component({
    selector: 'dot-block-editor',
    standalone: true,
    imports: [],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: '<div>Block Editor Mock</div>'
})
export class BlockEditorMockComponent implements OnInit {
    @Input() value: JSONContent;
    editor: Editor;
    @Output() valueChange = new EventEmitter<JSONContent>();

    ngOnInit() {
        this.editor = new Editor({
            extensions: [StarterKit]
        });

        this.editor.on('create', () => {
            this.editor.commands.setContent(this.value, true);
        });

        this.editor.on('update', () => {
            this.valueChange.emit();
        });
    }
}
