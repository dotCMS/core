import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

import { ChangeDetectionStrategy, Component, Input, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CUSTOMER_ACTIONS, isInsideEditor, postMessageToEditor } from '@dotcms/client';

@Component({
    selector: 'editable-text',
    standalone: true,
    templateUrl: './editable-text.component.html',
    styleUrl: './editable-text.component.css',
    imports: [EditorComponent, FormsModule, ReactiveFormsModule],
    providers: [
        {
            provide: TINYMCE_SCRIPT_SRC,
            useValue: 'http://localhost:8080/html/tinymce/tinymce.min.js'
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditableTextComponent implements OnInit {
    @ViewChild(EditorComponent) editorComponent!: EditorComponent;

    @Input() mode = '';
    @Input() inode = '';
    @Input() field = '';
    @Input() langId = '';
    @Input() content = '';

    constructor() {
        // eslint-disable-next-line no-console
        console.log('EditableTextComponent constructor');
    }

    protected form!: FormGroup;
    protected isInsideEditor = isInsideEditor();
    protected readonly init: EditorComponent['init'] = {
        base_url: 'http://localhost:8080/html/tinymce', // Root for resources
        suffix: '.min', // Suffix to use when loading resources
        license_key: 'gpl',
        plugins: 'lists link image table code help wordcount',
        menubar: false
    };

    ngOnInit() {
        this.form = new FormGroup({
            content: new FormControl(this.content)
        });
    }

    onFocusOut(_event: unknown) {
        // eslint-disable-next-line no-console
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
            // Todo: Changes this is a more practical way to send the date to the editor
            payload: {
                innerHTML: this.form.get('content')?.value,
                dataset: {
                    inode: this.inode,
                    langId: this.langId,
                    fieldName: this.field
                }
            }
        });
    }
}
