import { StyleEditorFieldType } from '@dotcms/types/internal';

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

export function createField(label = ''): BuilderField {
    return {
        uid: crypto.randomUUID(),
        type: 'input',
        label,
        identifier: label ? toLabelIdentifier(label) : '',
        inputType: 'text',
        placeholder: '',
        columns: 1,
        options: [
            { label: '', value: '' },
            { label: '', value: '' }
        ]
    };
}

export function createSection(title = 'New Section', fieldLabel = ''): BuilderSection {
    return {
        uid: crypto.randomUUID(),
        title,
        fields: [createField(fieldLabel)]
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

/**
 * Returns a Set of identifiers that appear more than once across all sections.
 * Used by the builder to enforce globally unique field identifiers.
 */
export function getDuplicateIdentifiers(sections: BuilderSection[]): Set<string> {
    const seen = new Set<string>();
    const duplicates = new Set<string>();
    for (const section of sections) {
        for (const field of section.fields) {
            const id = field.identifier.trim();
            if (!id) continue;
            if (seen.has(id)) {
                duplicates.add(id);
            } else {
                seen.add(id);
            }
        }
    }

    return duplicates;
}

/**
 * Returns true if a BuilderField has any validation error.
 * Used by the builder (form-level validity) and section (header error indicator).
 */
export function fieldHasErrors(field: BuilderField): boolean {
    if (!field.label.trim() || !field.identifier.trim()) {
        return true;
    }
    if (field.type === 'input') {
        return false;
    }

    if (field.options.length === 0) {
        return true;
    }

    return field.options.some((opt) => {
        if (!opt.label?.trim()) {
            return true;
        }

        if (field.type === 'checkboxGroup') {
            return !opt.key?.trim();
        }

        if (field.type === 'radio' && opt.imageURL !== undefined && !opt.imageURL?.trim())
            return true;

        return false;
    });
}

export const FIELD_TYPE_OPTIONS: { labelKey: string; value: StyleEditorFieldType }[] = [
    { labelKey: 'style.editor.form.builder.field.type.short.text', value: 'input' },
    { labelKey: 'style.editor.form.builder.field.type.dropdown', value: 'dropdown' },
    { labelKey: 'style.editor.form.builder.field.type.radio', value: 'radio' },
    { labelKey: 'style.editor.form.builder.field.type.checkbox.group', value: 'checkboxGroup' }
];
