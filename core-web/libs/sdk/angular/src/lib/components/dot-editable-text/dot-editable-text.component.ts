import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { EventObj } from '@tinymce/tinymce-angular/editor/Events';

import {
    Component,
    HostListener,
    inject,
    Input,
    OnChanges,
    OnInit,
    ViewChild
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { TINYMCE_CONFIG, TINYMCE_FORMAT, TINYMCE_MODE } from './utils';

import { DotCMSContentlet } from '../../models';
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

    @Input() mode: TINYMCE_MODE = 'plain';
    @Input() format: TINYMCE_FORMAT = 'text';
    @Input() contentlet!: DotCMSContentlet;
    @Input() fieldName = '';

    protected content = '';
    protected safeContent!: SafeHtml;
    protected init!: EditorComponent['init'];
    protected readonly isInsideEditor = isInsideEditor();

    readonly #client = inject<DotCmsClient>(DOTCMS_CLIENT_TOKEN);
    readonly #sanitizer = inject<DomSanitizer>(DomSanitizer);

    get editor() {
        return this.editorComponent.editor;
    }

    get onNumberOfPages() {
        return this.contentlet['onNumberOfPages'];
    }

    @HostListener('window:message', ['$event'])
    onMessage(event: MessageEvent) {
        if (event.data.name !== 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS') {
            return;
        }

        this.editorComponent.editor.focus();
    }

    ngOnInit() {
        this.init = {
            ...TINYMCE_CONFIG[this.mode],
            base_url: `${this.#client.dotcmsUrl}/html/tinymce`
        };
    }

    ngOnChanges() {
        this.content = this.contentlet[this.fieldName] || '';
        this.safeContent = this.#sanitizer.bypassSecurityTrustHtml(this.content);
    }

    /**
     * Handle mouse down event
     *
     * @param {EventObj<MouseEvent>} { event }
     * @return {*}
     * @memberof DotEditableTextComponent
     */
    onMouseDown({ event }: EventObj<MouseEvent>) {
        if (this.onNumberOfPages <= 1 || this.editorComponent.editor.hasFocus()) {
            return;
        }

        const { inode, languageId: language } = this.contentlet;

        event.stopPropagation();
        event.preventDefault();

        try {
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.COPY_CONTENTLET_INLINE_EDITING,
                payload: {
                    dataset: {
                        inode,
                        language,
                        fieldName: this.fieldName
                    }
                }
            });
        } catch (error) {
            console.error('Failed to post message to editor:', error);
        }
    }
    /**
     * Handle focus out event
     *
     * @return {*}
     * @memberof DotEditableTextComponent
     */
    onFocusOut() {
        const content = this.editor.getContent({ format: this.format });

        if (!this.editor.isDirty() || !this.didContentChange(content)) {
            return;
        }

        const { inode, languageId: langId } = this.contentlet;

        try {
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.UPDATE_CONTENTLET_INLINE_EDITING,
                payload: {
                    content,
                    dataset: {
                        inode,
                        langId,
                        fieldName: this.fieldName
                    }
                }
            });
        } catch (error) {
            console.error('Failed to post message to editor:', error);
        }
    }

    /**
     * Check if the content has changed
     *
     * @private
     * @param {string} editedContent
     * @return {*}
     * @memberof DotEditableTextComponent
     */
    private didContentChange(editedContent: string) {
        return this.content !== editedContent;
    }
}
