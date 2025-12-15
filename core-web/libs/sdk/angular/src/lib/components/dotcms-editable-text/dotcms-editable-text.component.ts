import { EditorComponent, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

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

import { DotCMSBasicContentlet, DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { getUVEState, sendMessageToUVE } from '@dotcms/uve';
import { __TINYMCE_PATH_ON_DOTCMS__ } from '@dotcms/uve/internal';

import { TINYMCE_CONFIG, DOT_EDITABLE_TEXT_FORMAT, DOT_EDITABLE_TEXT_MODE } from './utils';

import type { Editor as TinyMCEEditor } from 'tinymce';

/**
 * Event object interface from TinyMCE Angular
 * This matches the EventObj interface from @tinymce/tinymce-angular/editor/Events
 * which is not exported from the main package
 */
interface EventObj<T> {
    event: T;
    editor: TinyMCEEditor;
}

/**
 * Dot editable text component.
 * This component is responsible to render a text field that can be edited inline.
 *
 * @export
 * @class DotCMSEditableTextComponent
 * @implements {OnInit}
 * @implements {OnChanges}
 */
@Component({
    selector: 'dotcms-editable-text',
    templateUrl: './dotcms-editable-text.component.html',
    styleUrl: './dotcms-editable-text.component.css',
    imports: [EditorComponent],
    providers: [
        {
            provide: TINYMCE_SCRIPT_SRC,
            useFactory: () => {
                const { dotCMSHost } = getUVEState() || {};

                return `${dotCMSHost || ''}${__TINYMCE_PATH_ON_DOTCMS__}`;
            }
        }
    ]
})
export class DotCMSEditableTextComponent<T extends DotCMSBasicContentlet>
    implements OnInit, OnChanges
{
    @ViewChild(EditorComponent) editorComponent!: EditorComponent;

    /**
     * Represents the mode of the editor which can be `plain`, `minimal`, or `full`
     *
     * @type {DOT_EDITABLE_TEXT_MODE}
     * @memberof DotCMSEditableTextComponent
     */
    @Input() mode: DOT_EDITABLE_TEXT_MODE = 'plain';
    /**
     * Represents the format of the editor which can be `text` or `html`
     *
     * @type {DOT_EDITABLE_TEXT_FORMAT}
     * @memberof DotCMSEditableTextComponent
     */
    @Input() format: DOT_EDITABLE_TEXT_FORMAT = 'text';
    /**
     * Represents the `contentlet` that can be inline edited
     *
     * @type {DotCMSContentlet}
     * @memberof DotCMSEditableTextComponent
     */
    @Input() contentlet!: T;
    /**
     * Represents the field name of the `contentlet` that can be edited
     *
     * @memberof DotCMSEditableTextComponent
     */
    @Input() fieldName!: keyof T;

    /**
     * Represents the content of the `contentlet` that can be edited
     *
     * @protected
     * @memberof DotCMSEditableTextComponent
     */
    protected content = '';
    /**
     * Represents the configuration of the editor
     *
     * @protected
     * @type {EditorComponent['init']}
     * @memberof DotCMSEditableTextComponent
     */
    protected init!: EditorComponent['init'];

    readonly #NotDotCMSHostMessage =
        'The `dotCMSHost` parameter is not defined. Check that the UVE is sending the correct parameters.';

    readonly #sanitizer = inject<DomSanitizer>(DomSanitizer);
    readonly #renderer = inject<Renderer2>(Renderer2);
    readonly #elementRef = inject<ElementRef>(ElementRef);

    /**
     * The TinyMCE editor
     *
     * @readonly
     * @memberof DotCMSEditableTextComponent
     */
    get editor() {
        return this.editorComponent?.editor;
    }

    /**
     * Represents if the component is inside the editor
     *
     * @protected
     * @type {boolean}
     * @memberof DotCMSEditableTextComponent
     */
    protected get isEditMode() {
        const { mode, dotCMSHost } = getUVEState() || {};

        return mode === UVE_MODE.EDIT && dotCMSHost;
    }

    /**
     * Returns the number of pages the contentlet is on
     *
     * @readonly
     * @memberof DotCMSEditableTextComponent
     */
    get onNumberOfPages() {
        return this.contentlet['onNumberOfPages'] || 1;
    }

    /**
     * Handle copy contentlet inline editing success event
     *
     * @param {MessageEvent} { data }
     * @return {*}
     * @memberof DotCMSEditableTextComponent
     */
    @HostListener('window:message', ['$event'])
    onMessage({ data }: MessageEvent) {
        const { name, payload } = data;
        if (name !== __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS) {
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
        const { dotCMSHost } = getUVEState() || {};

        if (!this.isEditMode) {
            this.innerHTMLToElement();

            if (!dotCMSHost) {
                console.warn(this.#NotDotCMSHostMessage);
            }

            return;
        }

        this.init = {
            ...TINYMCE_CONFIG[this.mode],
            base_url: `${dotCMSHost}/ext/tinymcev7`
        };
    }

    ngOnChanges() {
        this.content = (this.contentlet[this.fieldName] as string) || '';
        if (this.editor) {
            this.editor.setContent(this.content, { format: this.format });
        }
    }

    /**
     * Handle mouse down event
     *
     * @param {EventObj<MouseEvent>} { event }
     * @return {*}
     * @memberof DotCMSEditableTextComponent
     */
    onMouseDown({ event }: EventObj<MouseEvent>) {
        if (Number(this.onNumberOfPages) <= 1 || this.editorComponent.editor.hasFocus()) {
            return;
        }

        const { inode, languageId: language } = this.contentlet;

        event.stopPropagation();
        event.preventDefault();

        try {
            sendMessageToUVE({
                action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
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
     * @memberof DotCMSEditableTextComponent
     */
    onFocusOut() {
        const content = this.editor.getContent({ format: this.format });

        if (!this.editor.isDirty() || !this.didContentChange(content)) {
            return;
        }

        const { inode, languageId: langId } = this.contentlet;

        try {
            sendMessageToUVE({
                action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
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
     * @memberof DotCMSEditableTextComponent
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
     * @memberof DotCMSEditableTextComponent
     */
    private didContentChange(editedContent: string) {
        return this.content !== editedContent;
    }
}
