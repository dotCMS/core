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
    SecurityContext,
    ViewChild
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import {
    CUSTOMER_ACTIONS,
    DotCmsClient,
    isInsideEditor,
    postMessageToEditor
} from '@dotcms/client';

import { TINYMCE_CONFIG, DOT_EDITABLE_TEXT_FORMAT, DOT_EDITABLE_TEXT_MODE } from './utils';

import { DotCMSContentlet } from '../../models';

/**
 * Dot editable text component.
 * This component is responsible to render a text field that can be edited inline.
 *
 * @export
 * @class DotEditableTextComponent
 * @implements {OnInit}
 * @implements {OnChanges}
 */
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
                return `${DotCmsClient.dotcmsUrl}/ext/tinymcev7/tinymce.min.js`;
            }
        }
    ]
})
export class DotEditableTextComponent implements OnInit, OnChanges {
    @ViewChild(EditorComponent) editorComponent!: EditorComponent;

    /**
     * Represents the mode of the editor which can be `plain`, `minimal`, or `full`
     *
     * @type {DOT_EDITABLE_TEXT_MODE}
     * @memberof DotEditableTextComponent
     */
    @Input() mode: DOT_EDITABLE_TEXT_MODE = 'plain';
    /**
     * Represents the format of the editor which can be `text` or `html`
     *
     * @type {DOT_EDITABLE_TEXT_FORMAT}
     * @memberof DotEditableTextComponent
     */
    @Input() format: DOT_EDITABLE_TEXT_FORMAT = 'text';
    /**
     * Represents the `contentlet` that can be inline edited
     *
     * @type {DotCMSContentlet}
     * @memberof DotEditableTextComponent
     */
    @Input() contentlet!: DotCMSContentlet;
    /**
     * Represents the field name of the `contentlet` that can be edited
     *
     * @memberof DotEditableTextComponent
     */
    @Input() fieldName = '';

    /**
     * Represents the content of the `contentlet` that can be edited
     *
     * @protected
     * @memberof DotEditableTextComponent
     */
    protected content = '';
    /**
     * Represents the configuration of the editor
     *
     * @protected
     * @type {EditorComponent['init']}
     * @memberof DotEditableTextComponent
     */
    protected init!: EditorComponent['init'];
    /**
     * Represents if the component is inside the editor
     *
     * @protected
     * @type {boolean}
     * @memberof DotEditableTextComponent
     */
    protected isInsideEditor!: boolean;

    readonly #sanitizer = inject<DomSanitizer>(DomSanitizer);
    readonly #renderer = inject<Renderer2>(Renderer2);
    readonly #elementRef = inject<ElementRef>(ElementRef);

    /**
     * The TinyMCE editor
     *
     * @readonly
     * @memberof DotEditableTextComponent
     */
    get editor() {
        return this.editorComponent?.editor;
    }

    /**
     * Returns the number of pages the contentlet is on
     *
     * @readonly
     * @memberof DotEditableTextComponent
     */
    get onNumberOfPages() {
        return this.contentlet['onNumberOfPages'] || 1;
    }

    /**
     * Handle copy contentlet inline editing success event
     *
     * @param {MessageEvent} { data }
     * @return {*}
     * @memberof DotEditableTextComponent
     */
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
            base_url: `${DotCmsClient.dotcmsUrl}/ext/tinymcev7`
        };
    }

    ngOnChanges() {
        this.content = this.contentlet[this.fieldName] || '';
        if (this.editor) {
            this.editor.setContent(this.content, { format: this.format });
        }
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
        const safeHtml = this.#sanitizer.bypassSecurityTrustHtml(this.content);
        const content = this.#sanitizer.sanitize(SecurityContext.HTML, safeHtml) || '';

        this.#renderer.setProperty(element, 'innerHTML', content);
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
