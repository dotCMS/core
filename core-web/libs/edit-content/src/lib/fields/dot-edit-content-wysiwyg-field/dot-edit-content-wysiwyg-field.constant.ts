import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

import { SelectItem } from 'primeng/api';

import { DEFAULT_MONACO_CONFIG } from '../../models/dot-edit-content-field.constant';

// Available Editors to switch
export enum AvailableEditor {
    TinyMCE = 'TinyMCE',
    Monaco = 'Monaco'
}

export enum AvailableLanguageMonaco {
    PlainText = 'plaintext',
    Javascript = 'javascript',
    Markdown = 'markdown',
    Html = 'html'
}

// Dropdown values to use in Monaco Editor
export const MonacoLanguageOptions: SelectItem[] = [
    { label: 'Html', value: AvailableLanguageMonaco.Html },
    { label: 'Javascript', value: AvailableLanguageMonaco.Javascript },
    { label: 'Markdown', value: AvailableLanguageMonaco.Markdown },
    { label: 'Plain Text', value: AvailableLanguageMonaco.PlainText }
];

// Dropdown values to select Editors
export const EditorOptions: SelectItem[] = [
    { label: 'WYSIWYG', value: AvailableEditor.TinyMCE },
    { label: 'Code', value: AvailableEditor.Monaco }
];

export const DEFAULT_EDITOR = AvailableEditor.TinyMCE;

export const DEFAULT_MONACO_LANGUAGE = 'html';

export const DEFAULT_WYSIWYG_FIELD_MONACO_CONFIG: MonacoEditorConstructionOptions = {
    ...DEFAULT_MONACO_CONFIG,
    language: DEFAULT_MONACO_LANGUAGE,
    automaticLayout: true,
    theme: 'vs'
};

export const DEFAULT_TINYMCE_CONFIG = {
    menubar: false,
    image_caption: true,
    image_advtab: true,
    contextmenu: 'align link image',
    toolbar1:
        'undo redo | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent dotAddImage hr',
    plugins:
        'advlist autolink lists link image charmap preview anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table directionality emoticons template',
    theme: 'silver'
};

export const MdSyntax = ['# ', '## ', '### ', '- ', '* ', '1. ', '```', '>['];

export const HtmlTags = ['<div', '<p>', '<span', '<a ', '<img', '<ul', '<li', '<table'];

export const JsKeywords = [
    'function',
    'const ',
    'let ',
    'var ',
    '=>',
    'class ',
    'interface ',
    'import '
];
