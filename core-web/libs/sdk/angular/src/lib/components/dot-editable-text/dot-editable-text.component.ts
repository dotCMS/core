import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { EventObj } from '@tinymce/tinymce-angular/editor/Events';

import { Component, inject, Input, OnChanges, OnInit, ViewChild } from '@angular/core';
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
    selector: 'dot-editable-text',
    standalone: true,
    templateUrl: './dot-editable-text.component.html',
    styleUrl: './dot-editable-text.component.css',
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
export class DotEditableTextComponent implements OnInit, OnChanges {
    @ViewChild(EditorComponent) editorComponent!: EditorComponent;

    @Input() inode = '';
    @Input() field = '';
    @Input() content = '';
    @Input() onNumberOfPages = 1;
    @Input() langId = 1;
    @Input() mode: TINYMCE_MODE = 'plain';
    @Input() format: TINYMCE_FORMAT = 'text';

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
        window.addEventListener('message', (event) => {
            if (event.data.name !== 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS') {
                return;
            }

            this.editorComponent.editor.focus();
        });
    }

    ngOnChanges() {
        // eslint-disable-next-line no-console
        console.log('onChanges', this.inode);
        // eslint-disable-next-line no-console
        console.log('onChanges', this.onNumberOfPages);
    }

    get editor() {
        return this.editorComponent.editor;
    }

    onMouseDown(eventObj: EventObj<MouseEvent>) {
        if (!(this.onNumberOfPages > 1) || this.editorComponent.editor.hasFocus()) {
            return;
        }

        const { event } = eventObj; // Prevent focus
        event.stopPropagation();
        event.preventDefault();

        const dataset = {
            inode: this.inode,
            mode: this.mode,
            language: this.langId.toString(),
            fieldName: this.field
        };

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING,
            payload: {
                dataset
            }
        });
    }

    onFocusIn(_event: EventObj<FocusEvent>) {
        this.editor.setDirty(false);
    }

    onFocusOut(_event: EventObj<FocusEvent>) {
        const content = this.editor.getContent({ format: this.format });

        if (!this.editor.isDirty() || !this.didContentChange(content)) {
            return;
        }

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
    }

    private didContentChange(editedContent: string) {
        return this.content !== editedContent;
    }
}
