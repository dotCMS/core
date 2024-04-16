import { ElementRef, Injectable, signal } from '@angular/core';

import { InlineEditingContentletDataset } from '../../edit-ema-editor/components/ema-page-dropzone/types';

declare global {
    interface Window {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        tinymce: any;
    }
}
@Injectable({
    providedIn: 'root'
})
export class InlineEditService {
    private iframeWindow = signal<Window | null>(null);
    private inlineEditingTargetDataset = signal<InlineEditingContentletDataset | null>(null);

    private DEFAULT_TINYMCE_CONFIG = {
        menubar: false,
        inline: true,
        valid_styles: {
            '*': 'font-size,font-family,color,text-decoration,text-align'
        },
        powerpaste_word_import: 'clean',
        powerpaste_html_import: 'clean',
        setup: this.handleInlineEditEvents
    };

    private TINYCME_CONFIG = {
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

    injectInlineEdit(iframe: ElementRef<HTMLIFrameElement>): void {
        const doc = iframe.nativeElement.contentDocument;
        this.iframeWindow.set(iframe.nativeElement.contentWindow);

        if (!doc.querySelector('script[data-inline="true"]')) {
            const script = doc.createElement('script');
            script.dataset.inline = 'true';
            script.src = '/html/js/tinymce/js/tinymce/tinymce.min.js';

            const style = doc.createElement('style');
            style.innerHTML = `
            [data-inode][data-field-name][data-mode] {
                cursor: text;
                border: 1px solid #53c2f9 !important;
                display: block;
            }
        `;

            doc.body.appendChild(script);
            doc.body.appendChild(style);
        }
    }

    handleInlineEdit(dataset: InlineEditingContentletDataset): void {
        // const { target } = this.targetTinyMCE();
        // const { dataset } = target;

        // // if the mode is falsy we do not initialize tinymce.
        // if (!dataset.mode) {
        //     return;
        // }

        // console.log(dataset);

        // this.targetTinyMCE().stopPropagation();
        // this.targetTinyMCE().preventDefault();

        this.inlineEditingTargetDataset.set(dataset);

        if (this.isInMultiplePages(this.inlineEditingTargetDataset())) {
            window.parent.postMessage(
                {
                    action: 'copy-contentlet-inline-editing',
                    payload: {
                        dataset: { ...this.inlineEditingTargetDataset() }
                    }
                },
                '*'
            );

            return;
        }

        this.initEditor();
    }

    initEditor() {
        const dataset = this.inlineEditingTargetDataset();

        const dataSelector = `[data-inode="${dataset.inode}"][data-field-name="${dataset.fieldName}"]`;

        this.iframeWindow()
            .tinymce.init({
                ...this.TINYCME_CONFIG[dataset.mode || 'minimal'],
                selector: dataSelector
            })
            .then(([ed]) => {
                ed?.editorCommands.execCommand('mceFocus');
            });
    }

    replaceContentletONCopy({
        oldInode,
        newInode,
        newIdentifier
    }: {
        oldInode: string;
        newInode: string;
        newIdentifier: string;
    }) {
        const contentlet = this.iframeWindow().document.querySelector(
            `[data-dot-inode='${oldInode}']`
        );
        if (!contentlet) {
            return;
        }

        contentlet.setAttribute('data-dot-inode', newInode);
        contentlet.setAttribute('data-dot-identifier', newIdentifier);
        contentlet.setAttribute('data-dot-on-number-of-pages', '1');

        const editorElement = contentlet.querySelector('[data-inode]');
        editorElement?.setAttribute('data-inode', newInode);
    }

    handleInlineEditEvents(editor) {
        editor.on('blur', (e) => {
            const { target: ed, type: eventType } = e;
            // console.log(e);

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

            // if (eventType === 'blur') {
            //     e.stopImmediatePropagation();
            //     ed.destroy(false);
            // }

            // ed.destroy();

            // window.tinymce.remove(target.id);
            // this.iframeWindow()?.tinymce.remove(target.id);

            const content = ed.getContent();
            const element = ed.target;
            const data = {
                dataset: { ...dataset },
                innerHTML: content,
                element,
                eventType,
                isNotDirty: ed.isNotDirty
            };

            // if (eventType === 'blur' && !ed.isNotDirty) {
            //To save the contentlet
            window.parent.postMessage(
                {
                    action: 'update-contentlet-inline-editing',
                    payload: eventType === 'blur' && !ed.isNotDirty ? data : null
                },
                '*'
            );
            // }

            // if (eventType === 'blur' && ed.isNotDirty) {
            //     window.parent.postMessage(
            //         {
            //             action: 'update-contentlet-inline-editing',
            //             payload: null
            //         },
            //         '*'
            //     );
            // }

            if (eventType === 'blur') {
                e.stopImmediatePropagation();
                ed.destroy(false);
            }
        });
    }

    isInMultiplePages(dataset: InlineEditingContentletDataset) {
        const targetElement = this.iframeWindow().document.querySelector(
            `[data-inode="${dataset.inode}"][data-field-name="${dataset.fieldName}"]`
        );

        const contentlet = targetElement.closest('[data-dot-object="contentlet"]') as HTMLElement;

        return Number(contentlet.dataset.dotOnNumberOfPages || 0) > 1;
    }

    setTargetInlineMCEDataset(dataset: InlineEditingContentletDataset) {
        this.inlineEditingTargetDataset.set(dataset);
    }

    setIframeWindow(iframeWindow: Window) {
        this.iframeWindow.set(iframeWindow);
    }
}
