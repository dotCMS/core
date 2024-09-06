import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

import { SelectItem } from 'primeng/api';

import { DEFAULT_MONACO_CONFIG } from '../../models/dot-edit-content-field.constant';

// Available Editors to switch
export enum AvailableEditor {
    TinyMCE = 'TinyMCE',
    Monaco = 'Monaco'
}

// Dropdown values to use in Monaco Editor
export const MonacoLanguageOptions: SelectItem[] = [
    { label: 'Plain Text', value: 'plaintext' },
    { label: 'TypeScript', value: 'typescript' },
    { label: 'HTML', value: 'html' },
    { label: 'Markdown', value: 'markdown' }
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
