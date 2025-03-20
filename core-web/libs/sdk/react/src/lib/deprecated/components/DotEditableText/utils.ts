import { IAllProps } from '@tinymce/tinymce-react';

import { DotCMSContentlet } from '../../models';

export type DOT_EDITABLE_TEXT_FORMAT = 'html' | 'text';

export type DOT_EDITABLE_TEXT_MODE = 'minimal' | 'full' | 'plain';

export interface DotEditableTextProps {
    /**
     * Represents the field name of the `contentlet` that can be edited
     *
     * @memberof DotEditableTextProps
     */
    fieldName: string;
    /**
     * Represents the format of the editor which can be `text` or `html`
     *
     * @type {DOT_EDITABLE_TEXT_FORMAT}
     * @memberof DotEditableTextProps
     */
    format?: DOT_EDITABLE_TEXT_FORMAT;
    /**
     * Represents the mode of the editor which can be `plain`, `minimal`, or `full`
     *
     * @type {DOT_EDITABLE_TEXT_MODE}
     * @memberof DotEditableTextProps
     */
    mode?: DOT_EDITABLE_TEXT_MODE;
    /**
     * Represents the `contentlet` that can be inline edited
     *
     * @type {DotCMSContentlet}
     * @memberof DotEditableTextProps
     */
    contentlet: DotCMSContentlet;
}

const DEFAULT_TINYMCE_CONFIG: IAllProps['init'] = {
    inline: true,
    menubar: false,
    powerpaste_html_import: 'clean',
    powerpaste_word_import: 'clean',
    suffix: '.min',
    valid_styles: {
        '*': 'font-size,font-family,color,text-decoration,text-align'
    }
};

export const TINYMCE_CONFIG: {
    [key in DOT_EDITABLE_TEXT_MODE]: IAllProps['init'];
} = {
    full: {
        ...DEFAULT_TINYMCE_CONFIG,
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
        ...DEFAULT_TINYMCE_CONFIG,
        plugins: '',
        toolbar: ''
    },
    minimal: {
        ...DEFAULT_TINYMCE_CONFIG,
        plugins: 'link autolink',
        toolbar: 'bold italic underline | link',
        valid_elements: 'strong,em,span[style],a[href]'
    }
};
