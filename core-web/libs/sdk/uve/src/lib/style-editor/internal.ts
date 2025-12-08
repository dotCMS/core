import {
    StyleEditorFieldSchema,
    StyleEditorFormSchema,
    StyleEditorSectionSchema,
    StyleEditorField,
    StyleEditorForm,
    StyleEditorSection,
    StyleEditorFieldInputType
} from './types';

/**
 * Normalizes a field definition into the schema format expected by UVE.
 *
 * Converts developer-friendly field definitions into a normalized structure:
 * - Extracts type-specific configuration properties
 * - Normalizes string options to { label, value } objects for dropdown, radio, and checkboxGroup fields
 * - Preserves additional properties like backgroundImage, width, and height for radio options
 * - Sets default values for optional properties (e.g., inputType defaults to 'text')
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param field - The field definition to normalize
 * @returns The normalized field schema ready to be sent to UVE
 *
 * @example
 * ```typescript
 * normalizeField({
 *   type: 'input',
 *   label: 'Font Size',
 *   inputType: 'number',
 *   default: 16
 * })
 * // Returns: { type: 'input', label: 'Font Size', config: { inputType: 'number', defaultValue: 16 } }
 * ```
 */
function normalizeField(field: StyleEditorField): StyleEditorFieldSchema {
    const base = {
        type: field.type,
        label: field.label
    };

    const config: StyleEditorFieldSchema['config'] = {};

    // Handle type-specific properties
    if (field.type === 'input') {
        config.inputType = field.inputType as unknown as StyleEditorFieldInputType;
        config.placeholder = field.placeholder as unknown as string;
        config.defaultValue = field.defaultValue as unknown as
            | string
            | number
            | boolean
            | Record<string, boolean>;
    }

    if (field.type === 'dropdown' || field.type === 'radio') {
        // Normalize options to consistent format
        config.options = field.options.map((opt) =>
            typeof opt === 'string' ? { label: opt, value: opt } : opt
        );
        config.placeholder = field.type === 'dropdown' ? field.placeholder : undefined;
        config.defaultValue = field.defaultValue;
    }

    if (field.type === 'checkboxGroup') {
        // Normalize options to consistent format
        config.options = field.options.map((opt) =>
            typeof opt === 'string' ? { label: opt, value: opt } : opt
        );
        config.defaultValue = field.defaultValue;
    }

    return { ...base, config };
}

/**
 * Normalizes a section definition into the schema format expected by UVE.
 *
 * Handles both single-column and multi-column layouts:
 * - Single column (columns = 1): Wraps the fields array in a nested array structure
 * - Multi-column (columns > 1): Validates that fields is already a multi-dimensional array
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param section - The section definition to normalize
 * @returns The normalized section schema with fields organized by columns
 * @throws {Error} If columns > 1 but fields is not a multi-dimensional array
 *
 * @example
 * ```typescript
 * // Single column
 * normalizeSection({
 *   title: 'Typography',
 *   fields: [field1, field2]
 * })
 * // Returns: { title: 'Typography', columns: 1, fields: [[field1, field2]] }
 *
 * // Multi-column
 * normalizeSection({
 *   title: 'Layout',
 *   columns: 2,
 *   fields: [[field1], [field2]]
 * })
 * // Returns: { title: 'Layout', columns: 2, fields: [[field1], [field2]] }
 * ```
 */
function normalizeSection(section: StyleEditorSection): StyleEditorSectionSchema {
    const columns = section.columns || 1;

    // Determine if fields is multi-column or single column
    let normalizedFields: StyleEditorFieldSchema[][];

    if (columns === 1) {
        // Single column: wrap in array
        normalizedFields = [(section.fields as StyleEditorField[]).map(normalizeField)];
    } else {
        // Multi-column: fields should already be array of arrays
        if (!Array.isArray(section.fields[0])) {
            throw new Error(
                `Section "${section.title}" has columns=${columns} but fields is not a multi-dimensional array`
            );
        }
        normalizedFields = (section.fields as StyleEditorField[][]).map((column) =>
            column.map(normalizeField)
        );
    }

    return {
        title: section.title,
        columns,
        fields: normalizedFields
    };
}

/**
 * Normalizes a complete form definition into the schema format expected by UVE.
 *
 * This is the main entry point for converting a developer-friendly form definition
 * into the normalized schema structure. It processes all sections and their fields,
 * applying normalization rules to ensure consistency.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param form - The complete form definition to normalize
 * @returns The normalized form schema ready to be sent to UVE
 *
 * @example
 * ```typescript
 * const schema = normalizeForm({
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       fields: [
 *         { type: 'input', label: 'Font Size', inputType: 'number' }
 *       ]
 *     }
 *   ]
 * });
 * ```
 */
export function normalizeForm(form: StyleEditorForm): StyleEditorFormSchema {
    return {
        contentType: form.contentType,
        sections: form.sections.map(normalizeSection)
    };
}
