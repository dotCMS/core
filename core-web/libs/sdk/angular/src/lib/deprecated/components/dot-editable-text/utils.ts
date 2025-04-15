import { EditorComponent } from '@tinymce/tinymce-angular';

export type DOT_EDITABLE_TEXT_MODE = 'minimal' | 'full' | 'plain';

export type DOT_EDITABLE_TEXT_FORMAT = 'html' | 'text';

const DEFAULT_TINYMCE_CONFIG: EditorComponent['init'] = {
    menubar: false,
    inline: true,
    valid_styles: {
        '*': 'font-size,font-family,color,text-decoration,text-align'
    },
    powerpaste_word_import: 'clean',
    powerpaste_html_import: 'clean',
    suffix: '.min', // Suffix to use when loading resources
    license_key: 'gpl'
};

export const TINYMCE_CONFIG: {
    [key in DOT_EDITABLE_TEXT_MODE]: EditorComponent['init'];
} = {
    minimal: {
        ...DEFAULT_TINYMCE_CONFIG,
        plugins: 'link autolink',
        toolbar: 'bold italic underline | link',
        valid_elements: 'strong,em,span[style],a[href]'
    },
    full: {
        ...DEFAULT_TINYMCE_CONFIG,
        plugins: 'link lists autolink charmap',
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
            'styleselect undo redo | bold italic underline | forecolor backcolor | alignleft aligncenter alignright alignfull | numlist bullist outdent indent | hr charmap removeformat | link'
        ]
    },
    plain: {
        ...DEFAULT_TINYMCE_CONFIG,
        plugins: '',
        toolbar: ''
    }
};
