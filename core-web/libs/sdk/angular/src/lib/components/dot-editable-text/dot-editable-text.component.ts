import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { EventObj } from '@tinymce/tinymce-angular/editor/Events';

import {
    Component,
    ElementRef,
    HostListener,
    inject,
    Input,
    OnChanges,
    OnInit,
    Renderer2,
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

@Component({
    selector: 'dot-editable-text',
    standalone: true,
    templateUrl: './dot-editable-text.component.html',
    styleUrl: './dot-editable-text.component.css',
    imports: [EditorComponent],
    providers: [
        {
            provide: TINYMCE_SCRIPT_SRC,
            useFactory: () => {
                return `${DotCmsClient.dotcmsUrl}/html/js/tinymce-8.0/tinymce.min.js`;
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
    protected isInsideEditor!: boolean;

    readonly #sanitizer = inject<DomSanitizer>(DomSanitizer);
    readonly #renderer = inject<Renderer2>(Renderer2);
    readonly #elementRef = inject<ElementRef>(ElementRef);

    get editor() {
        return this.editorComponent.editor;
    }

    get onNumberOfPages() {
        return this.contentlet['onNumberOfPages'];
    }

    @HostListener('window:message', ['$event'])
    onMessage({ data }: MessageEvent) {
        const { name, payload } = data;
        if (name !== 'COPY_CONTENTLET_INLINE_EDITING_SUCCESS') {
            return;
        }

        const { oldInode, inode } = payload;
        const currentInode = this.contentlet.inode;

        if (currentInode === oldInode || currentInode === inode) {
            this.editorComponent.editor.focus();

            return;
        }
    }

    ngOnInit() {
        this.isInsideEditor = isInsideEditor();

        if (!this.isInsideEditor) {
            this.innerHTMLToElement();

            return;
        }

        this.init = {
            ...TINYMCE_CONFIG[this.mode],
            base_url: `${DotCmsClient.dotcmsUrl}/html/js/tinymce-8.0`
        };
    }

    ngOnChanges() {
        this.content = this.contentlet[this.fieldName] || '';
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
     * inner HTML to element
     *
     * @private
     * @param {string} editedContent
     * @return {*}
     * @memberof DotEditableTextComponent
     */
    private innerHTMLToElement() {
        const element = this.#elementRef.nativeElement;
        this.safeContent = this.#sanitizer.bypassSecurityTrustHtml(this.content);
        this.#renderer.setProperty(element, 'innerHTML', this.safeContent);
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
