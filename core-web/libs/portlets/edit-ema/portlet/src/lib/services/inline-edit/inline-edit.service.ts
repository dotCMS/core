import { ElementRef, Injectable, signal } from '@angular/core';

import { DotCMSUVEAction } from '@dotcms/types';

import { InlineEditingContentletDataset } from '../../edit-ema-editor/components/ema-page-dropzone/types';

declare global {
    interface Window {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        tinymce: any;
    }
}

// And added the way we add the `sdk-editor-vtl.js` file.
export const INLINE_CONTENT_STYLES = `
    [data-inode][data-field-name][data-mode] {
        cursor: text;
        border: 1px solid #53c2f9 !important;
        display: block;
    }
            
    [data-inode][data-field-name].dotcms__inline-edit-field {
        cursor: text;
        border: 1px solid #53c2f9 !important;
        display: block;
    }

    [data-inode][data-field-name][data-block-editor-content].dotcms__inline-edit-field {
        cursor: pointer;
    }    
`;

@Injectable({
    providedIn: 'root'
})
export class InlineEditService {
    $iframeWindow = signal<Window | null>(null);
    $isInlineEditingEnable = signal(true);
    private $inlineEditingTargetDataset = signal<InlineEditingContentletDataset | null>(null);

    private readonly DEFAULT_TINYMCE_CONFIG = {
        menubar: false,
        inline: true,
        valid_styles: {
            '*': 'font-size,font-family,color,text-decoration,text-align'
        },
        powerpaste_word_import: 'clean',
        powerpaste_html_import: 'clean',
        setup: this.#handleInlineEditEvents
    };

    private readonly TINYCME_CONFIG = {
        minimal: {
            plugins: ['link', 'autolink'],
            toolbar: 'bold italic underline | link',
            valid_elements: 'strong,em,span[style],a[href]',
            content_css: ['//fonts.googleapis.com/css?family=Lato:300,300i,400,400i'],
            ...this.DEFAULT_TINYMCE_CONFIG
        },
        full: {
            plugins: ['link', 'lists', 'autolink', 'hr', 'charmap'],
            style_formats: [
                { title: 'Paragraph', format: 'p' },
                { title: 'Header 1', format: 'h1' },
                { title: 'Header 2', format: 'h2' },
                { title: 'Header 3', format: 'h3' },
                { title: 'Header 4', format: 'h4' },
                { title: 'Header 5', format: 'h5' },
                { title: 'Header 6', format: 'h6' },
                { title: 'Pre', format: 'pre' },
                { title: 'Code', format: 'code' }
            ],
            toolbar: [
                'styleselect | undo redo | bold italic underline | forecolor backcolor | alignleft aligncenter alignright alignfull | numlist bullist outdent indent | hr charmap removeformat | link'
            ],
            ...this.DEFAULT_TINYMCE_CONFIG
        }
    };

    /**
     * Injects the inline edit functionality into the specified iframe.
     *
     * @param {ElementRef<HTMLIFrameElement>} iframe - The ElementRef of the HTMLIFrameElement to inject the inline edit into.
     * @memberof InlineEditService
     */
    injectInlineEdit(iframe: ElementRef<HTMLIFrameElement>): void {
        const doc = iframe.nativeElement.contentDocument;
        this.$iframeWindow.set(iframe.nativeElement.contentWindow);
        this.$isInlineEditingEnable.set(true);

        if (doc.querySelector('script[data-inline="true"]')) {
            return;
        }

        this.#addStyles(doc);
        this.#addScript(doc, '/html/js/tinymce/js/tinymce/tinymce.min.js');
    }

    removeInlineEdit(iframe: ElementRef<HTMLIFrameElement>) {
        const doc = iframe.nativeElement.contentDocument;

        doc.querySelectorAll('script[data-inline="true"]').forEach((script) => script.remove());
        doc.querySelectorAll('style').forEach((style) => {
            if (style.textContent.includes('[data-inode][data-field-name][data-mode]')) {
                style.remove();
            }
        });

        this.$isInlineEditingEnable.set(false);
    }

    /**
     * Handles the inline editing of a contentlet dataset.
     * If the dataset is part of multiple pages, it sends a message to the parent window to copy the contentlet for inline editing.
     * Otherwise, it initializes the editor.
     *
     * @param dataset - The contentlet dataset to be edited inline.
     */
    handleInlineEdit(dataset: InlineEditingContentletDataset): void {
        if (!this.$isInlineEditingEnable()) {
            return;
        }

        this.$inlineEditingTargetDataset.set(dataset);

        if (this.isInMultiplePages(this.$inlineEditingTargetDataset())) {
            window.parent.postMessage(
                {
                    action: 'copy-contentlet-inline-editing',
                    payload: {
                        dataset: { ...this.$inlineEditingTargetDataset() }
                    }
                },
                '*'
            );

            return;
        }

        window.parent.postMessage(
            {
                action: DotCMSUVEAction.INIT_INLINE_EDITING,
                payload: {
                    type: 'WYSIWYG'
                }
            },
            '*'
        );
    }

    /**
     * Initializes the editor for inline editing.
     */
    initEditor() {
        if (!this.$isInlineEditingEnable()) {
            return;
        }

        const dataset = this.$inlineEditingTargetDataset();

        const dataSelector = `[data-inode="${dataset.inode}"][data-field-name="${dataset.fieldName}"]`;

        this.$iframeWindow()
            .tinymce.init({
                ...this.TINYCME_CONFIG[dataset.mode || 'minimal'],
                selector: dataSelector
            })
            .then(([ed]) => {
                ed?.editorCommands.execCommand('mceFocus');
            });
    }

    /**
     * Sets the target inline contentlet dataset.
     *
     * @param {InlineEditingContentletDataset} dataset
     * @memberof InlineEditService
     */
    setTargetInlineMCEDataset(dataset: InlineEditingContentletDataset) {
        this.$inlineEditingTargetDataset.set(dataset);
    }

    /**
     * Sets the iframe window.
     *
     * @param {Window} iframeWindow
     * @memberof InlineEditService
     */
    setIframeWindow(iframeWindow: Window) {
        this.$iframeWindow.set(iframeWindow);
    }

    /**
     * Adds the inline content styles to the document.
     *
     * @private
     * @param {Document} doc
     * @memberof InlineEditService
     */
    #addStyles(doc: Document) {
        const style = doc.createElement('style');
        style.innerHTML = INLINE_CONTENT_STYLES;
        doc.body?.appendChild(style);
    }

    #addScript(doc: Document, src: string) {
        const script = doc.createElement('script');
        script.dataset.inline = 'true';
        script.src = src;
        doc.body?.appendChild(script);
    }

    /**
     * Handles inline edit events for the editor.
     *
     * @param {Editor} editor - The editor instance.
     */
    #handleInlineEditEvents(editor) {
        editor.on('blur', (e) => {
            const { target: ed, type: eventType } = e;
            const dataset = ed.targetElm.dataset;
            const container = ed.bodyElement.closest('[data-dot-object="container"]');

            // For full editor we are adding pointer-events: none to all it children,
            // this is the way we can capture the click to init in the editor itself, after the editor
            // is initialized and clicked we set the pointer-events: auto so users can use the editor as intended.
            if (eventType === 'focus' && dataset.mode) {
                container.classList.add('inline-editing');
                ed.bodyElement.classList.add('active');
            }

            if (eventType === 'blur' && ed.bodyElement.classList.contains('active')) {
                container.classList.remove('inline-editing');
                ed.bodyElement.classList.remove('active');
            }

            const content = ed.getContent(); // TODO: We should set the format here based on the field type. Wysiwyg should be html and text should be text
            const element = ed.target;
            const data = {
                content,
                element,
                eventType,
                dataset: { ...dataset },
                isNotDirty: ed.isNotDirty
            };

            window.parent.postMessage(
                {
                    action: 'update-contentlet-inline-editing',
                    payload: eventType === 'blur' && !ed.isNotDirty ? data : null
                },
                '*'
            );

            if (eventType === 'blur') {
                e.stopImmediatePropagation();
                ed.destroy(false);
            }
        });

        editor.on('cut copy paste', () => {
            editor.setDirty(true);
        });
    }

    /**
     * Checks if the given contentlet dataset is present in multiple pages.
     *
     * @param dataset - The dataset containing the contentlet information.
     * @returns A boolean indicating whether the contentlet is present in multiple pages.
     */
    private isInMultiplePages(dataset: InlineEditingContentletDataset) {
        const targetElement = this.$iframeWindow().document.querySelector(
            `[data-inode="${dataset.inode}"][data-field-name="${dataset.fieldName}"]`
        );

        const contentlet = targetElement.closest('[data-dot-object="contentlet"]') as HTMLElement;

        return Number(contentlet.dataset.dotOnNumberOfPages || 0) > 1;
    }
}
