import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

import {
    ChangeDetectionStrategy,
    Component,
    inject,
    InjectionToken,
    Input,
    OnInit,
    ViewChild
} from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { DOTCMS_CLIENT_TOKEN } from '../../tokens/client';

@Component({
    selector: 'editable-text',
    standalone: true,
    templateUrl: './editable-text.component.html',
    styleUrl: './editable-text.component.css',
    imports: [EditorComponent, FormsModule, ReactiveFormsModule],
    providers: [
        {
            provide: TINYMCE_SCRIPT_SRC,
            deps: [DOTCMS_CLIENT_TOKEN],
            useFactory: (client: DotCmsClient) => {
                return `${client.dotcmsUrl}/html/tinymce/tinymce.min.js`;
            }
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

    // TODO: WE NEED TO FIX THIS SOMEHOW
    // IF WE DONT DO THIS WE WILL GET A TYPE ERROR ON DEVELOPMENT BECAUSE THE TOKEN USES THE TYPES FROM CORE WEB AND THE INJECT FUNCTION IS USING THE TYPES FROM THE EXAMPLE
    private readonly client = inject<DotCmsClient>(
        DOTCMS_CLIENT_TOKEN as unknown as InjectionToken<DotCmsClient>
    );

    constructor() {
        // eslint-disable-next-line no-console
        console.log('EditableTextComponent constructor');
    }

    protected form!: FormGroup;
    protected isInsideEditor = isInsideEditor();
    protected readonly init: EditorComponent['init'] = {
        base_url: `${this.client.dotcmsUrl}/html/tinymce`, // Root for resources
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
