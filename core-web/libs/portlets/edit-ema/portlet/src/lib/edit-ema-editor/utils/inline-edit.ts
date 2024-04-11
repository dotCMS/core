declare global {
    interface Window {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        tinymce: any;
    }
}

const DEFAULT_TINYMCE_CONFIG = {
    menubar: false,
    inline: true,
    valid_styles: {
        '*': 'font-size,font-family,color,text-decoration,text-align'
    },
    powerpaste_word_import: 'clean',
    powerpaste_html_import: 'clean',
    setup: handleInlineEditEvents
};

const TINYCME_CONFIG = {
    minimal: {
        plugins: ['link', 'autolink'],
        toolbar: 'bold italic underline | link',
        valid_elements: 'strong,em,span[style],a[href]',
        content_css: ['//fonts.googleapis.com/css?family=Lato:300,300i,400,400i'],
        ...DEFAULT_TINYMCE_CONFIG
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
        ...DEFAULT_TINYMCE_CONFIG
    }
};

export function injectInlineEdit(doc: Document): void {
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

export function handleInlineEdit(e: MouseEvent): void {
    const target = e.target as HTMLElement;
    const { dataset } = target;

    // if the mode is falsy we do not initialize tinymce.
    if (!dataset.mode) {
        return;
    }

    e.stopPropagation();
    e.preventDefault();

    // if(isInMultiplePages(target)) {
    //     window.contentletEvents.next({
    //         name: "showCopyModal",
    //         data: showCopyModalData(target)
    //     });

    //     return;
    // };

    initEditor(target);
}

export function handleInternalNav(e: MouseEvent) {
    const href =
        (e.target as HTMLAnchorElement).href ||
        ((e.target as HTMLElement).closest('a') as HTMLAnchorElement)?.href;

    if (href) {
        e.preventDefault();
        const url = new URL(href);

        // Check if the URL is not external
        if (url.hostname === window.location.hostname) {
            this.updateQueryParams({
                url: url.pathname
            });

            return;
        }

        // Open external links in a new tab
        this.window.open(href, '_blank');
    }
}

function initEditor(editorElement: HTMLElement) {
    const { dataset } = editorElement;
    const dataSelector = `[data-inode="${dataset.inode}"][data-field-name="${dataset.fieldName}"]`;
    const iframeWindow = editorElement.ownerDocument.defaultView;

    iframeWindow.tinymce
        .init({
            ...TINYCME_CONFIG[dataset.mode || 'minimal'],
            selector: dataSelector
        })
        .then(([ed]) => {
            ed?.editorCommands.execCommand('mceFocus');
        });
}

function handleInlineEditEvents(editor) {
    editor.on('focus blur', (e) => {
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

        if (eventType === 'blur') {
            e.stopImmediatePropagation();
            ed.destroy(false);
        }

        // TODO: Implement the save in the new editor
        // window.contentletEvents.next({
        //     name: "inlineEdit",
        //     data,
        // });
    });
}
