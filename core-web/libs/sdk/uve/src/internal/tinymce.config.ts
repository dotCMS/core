/**
 * TinyMCE default config across all versions
 *
 * @internal
 */
export const __DEFAULT_TINYMCE_CONFIG__ = {
    menubar: false,
    inline: true,
    valid_styles: {
        '*': 'font-size,font-family,color,text-decoration,text-align'
    },
    powerpaste_word_import: 'clean',
    powerpaste_html_import: 'clean',
    suffix: '.min' // Suffix to use when loading resources
};

/**
 * TinyMCE config to use per mode
 *
 * @internal
 */
export const __BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__ = {
    full: {
        plugins: 'link lists autolink charmap',
        toolbar: [
            'styleselect undo redo | bold italic underline | forecolor backcolor | alignleft aligncenter alignright alignfull | numlist bullist outdent indent | hr charmap removeformat | link'
        ],
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
        ]
    },
    plain: {
        plugins: '',
        toolbar: ''
    },
    minimal: {
        plugins: 'link autolink',
        toolbar: 'bold italic underline | link',
        valid_elements: 'strong,em,span[style],a[href]'
    }
};

/**
 * TinyMCE path
 *
 * @internal
 */
export const __TINYMCE_PATH_ON_DOTCMS__ = '/ext/tinymcev7/tinymce.min.js';
