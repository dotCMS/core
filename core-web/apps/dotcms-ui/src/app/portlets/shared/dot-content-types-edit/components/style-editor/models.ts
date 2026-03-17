import { StyleEditorFieldType } from '@dotcms/uve';

export interface BuilderOption {
    label: string;
    /** Option identifier for dropdown and radio fields. */
    value?: string;
    /** Option identifier for checkboxGroup fields — the key used to retrieve the boolean value. */
    key?: string;
    /** Optional image URL shown alongside the option label (radio only). */
    imageURL?: string;
}

export interface BuilderField {
    uid: string;
    type: StyleEditorFieldType;
    label: string;
    identifier: string;
    inputType: 'text' | 'number';
    placeholder: string;
    columns: 1 | 2;
    options: BuilderOption[];
}

export interface BuilderSection {
    uid: string;
    title: string;
    fields: BuilderField[];
}

export function createField(): BuilderField {
    return {
        uid: crypto.randomUUID(),
        type: 'input',
        label: 'New Field',
        identifier: 'newField',
        inputType: 'text',
        placeholder: '',
        columns: 1,
        options: [
            { label: '', value: '' },
            { label: '', value: '' }
        ]
    };
}

export function createSection(): BuilderSection {
    return {
        uid: crypto.randomUUID(),
        title: 'New Section',
        fields: [createField()]
    };
}

export function toLabelIdentifier(label: string): string {
    const camel = label
        .trim()
        .replace(/[^a-zA-Z0-9 ]/g, '')
        .replace(/\s+(.)/g, (_, c: string) => c.toUpperCase())
        .replace(/^(.)/, (c: string) => c.toLowerCase());

    return camel || 'field';
}

export const FIELD_TYPE_OPTIONS: { label: string; value: StyleEditorFieldType }[] = [
    { label: 'Short Text', value: 'input' },
    { label: 'Dropdown', value: 'dropdown' },
    { label: 'Radio Buttons', value: 'radio' },
    { label: 'Checkbox Group', value: 'checkboxGroup' }
];
