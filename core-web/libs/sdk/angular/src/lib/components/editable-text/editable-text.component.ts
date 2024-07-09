import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

import { Component, inject, Input, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { TINYMCE_CONFIG, TINYMCE_FORMAT, TINYMCE_MODE } from './utils';

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
    ]
})
export class EditableTextComponent implements OnInit {
    @ViewChild(EditorComponent) editorComponent!: EditorComponent;

    @Input() inode = '';
    @Input() field = '';
    @Input() content = '';
    @Input() langId = 1;
    @Input() mode: TINYMCE_MODE = 'minimal';
    @Input() format: TINYMCE_FORMAT = 'html';

    protected init!: EditorComponent['init'];
    protected safeContent!: SafeHtml;
    protected readonly isInsideEditor = isInsideEditor();

    readonly #client = inject<DotCmsClient>(DOTCMS_CLIENT_TOKEN);
    readonly #sanitizer = inject<DomSanitizer>(DomSanitizer);

    ngOnInit() {
        this.init = {
            base_url: `${this.#client.dotcmsUrl}/html/tinymce`, // Root for resources
            ...TINYMCE_CONFIG[this.mode]
        };

        this.safeContent = this.#sanitizer.bypassSecurityTrustHtml(this.content);
    }

    get editor() {
        return this.editorComponent.editor;
    }

    onFocusOut(_event: unknown) {
        if (!this.editor.isDirty()) {
            return;
        }

        const content = this.editor.getContent({ format: this.format });
        const dataset = {
            inode: this.inode,
            langId: this.langId,
            fieldName: this.field
        };

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
            payload: {
                content,
                dataset
            }
        });

        this.editor.setDirty(false); // Reset dirty state
    }
}
