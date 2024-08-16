import { IAllProps } from '@tinymce/tinymce-react';

import { DotCMSContentlet } from '../../models';

export type TINYMCE_MODE = 'minimal' | 'full' | 'plain';

export type TINYMCE_FORMAT = 'html' | 'text';

export interface DotEditableTextProps {
    /**
     * Represents the mode of the editor which can be `plain`, `minimal`, or `full`
     *
     * @type {TINYMCE_MODE}
     * @memberof DotEditableTextProps
     */
    mode: TINYMCE_MODE;
    /**
     * Represents the format of the editor which can be `text` or `html`
     *
     * @type {TINYMCE_FORMAT}
     * @memberof DotEditableTextProps
     */
    format: TINYMCE_FORMAT;
    /**
     * Represents the `contentlet` that can be inline edited
     *
     * @type {DotCMSContentlet}
     * @memberof DotEditableTextProps
     */
    contentlet: DotCMSContentlet;
    /**
     * Represents the field name of the `contentlet` that can be edited
     *
     * @memberof DotEditableTextProps
     */
    fieldName: string;
}

const DEFAULT_TINYMCE_CONFIG: IAllProps['init'] = {
    menubar: false,
    inline: true,
    valid_styles: {
        '*': 'font-size,font-family,color,text-decoration,text-align'
    },
    powerpaste_word_import: 'clean',
    powerpaste_html_import: 'clean',
    suffix: '.min'
};

export const TINYMCE_CONFIG: {
    [key in TINYMCE_MODE]: IAllProps['init'];
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
