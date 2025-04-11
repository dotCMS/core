import { SelectItem } from 'primeng/api';

/**
 * Enum representing the available editor options for the text area.
 */
export enum AvailableEditorTextArea {
    PlainText = 'Plain Text',
    Monaco = 'Monaco'
}

/**
 * Array of select items representing the available editor options for the text area.
 */
export const TextAreaEditorOptions: SelectItem[] = [
    { label: 'Plain Text', value: AvailableEditorTextArea.PlainText },
    { label: 'Code', value: AvailableEditorTextArea.Monaco }
];
