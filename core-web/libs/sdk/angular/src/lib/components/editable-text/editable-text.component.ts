import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

import { ChangeDetectionStrategy, Component, inject, Input, ViewChild } from '@angular/core';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { DotCMSContentlet } from '../../models';
import { DOTCMS_CLIENT_TOKEN } from '../../tokens/client';

@Component({
    selector: 'editable-text',
    standalone: true,
    templateUrl: './editable-text.component.html',
    styleUrl: './editable-text.component.css',
    imports: [EditorComponent],
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
export class EditableTextComponent {
    @ViewChild(EditorComponent) editorComponent!: EditorComponent;

    @Input() mode = '';
    @Input() contentlet!: DotCMSContentlet;
    @Input() field = '';

    private readonly client = inject<DotCmsClient>(DOTCMS_CLIENT_TOKEN);

    protected isInsideEditor = isInsideEditor();
    protected readonly init: EditorComponent['init'] = {
        base_url: `${this.client.dotcmsUrl}/html/tinymce`, // Root for resources
        suffix: '.min', // Suffix to use when loading resources
        license_key: 'gpl',
        plugins: 'lists link image table code help wordcount',
        menubar: false
    };

    get editor() {
        return this.editorComponent.editor;
    }

    onFocus(_event: unknown) {
        // console.log('onFocus', this.contentlet);
        // postMessageToEditor({
        //     action: CUSTOMER_ACTIONS.EDITABLE_TEXT_FOCUS,
        //     payload: {
        //         inode: this.contentlet.inode,
        //         langId: this.contentlet.languageId,
        //         fieldName: this.field
        //     }
        // });
    }

    onFocusOut(_event: unknown) {
        const content = this.editor.getContent({
            // TODO: We should change the format based on the field type, if is wysiwyg it should be html, if is text should be text
            format: 'text'
        });

        if (this.editor.isDirty()) {
            const dataset = {
                inode: this.contentlet.inode,
                langId: this.contentlet.languageId,
                fieldName: this.field
            };

            postMessageToEditor({
                action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
                payload: {
                    content,
                    dataset
                }
            });
        }
    }
}
