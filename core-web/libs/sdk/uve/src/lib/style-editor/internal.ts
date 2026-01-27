import {
    StyleEditorCheckboxOption,
    StyleEditorField,
    StyleEditorFieldInputType,
    StyleEditorFieldSchema,
    StyleEditorForm,
    StyleEditorFormSchema,
    StyleEditorSection,
    StyleEditorSectionSchema
} from './types';

/**
 * Normalizes a field definition into the schema format expected by UVE.
 *
 * Converts developer-friendly field definitions into a normalized structure where
 * all type-specific configuration properties are moved into a `config` object.
 * This transformation ensures consistency in the schema format sent to UVE.
 *
 * **Field Type Handling:**
 * - **Input fields**: Extracts `inputType` and `placeholder` into config
 * - **Dropdown fields**: Normalizes options (strings become `{ label, value }` objects)
 * - **Radio fields**: Normalizes options (preserves image properties like `imageURL`, `width`, `height`),
 *   extracts `columns` into config
 * - **Checkbox group fields**: Normalizes options (converts to format expected by UVE)
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param field - The field definition to normalize. Must be one of: input, dropdown, radio, or checkboxGroup
 * @returns The normalized field schema with type, label, and config properties ready to be sent to UVE
 *
 * @example
 * ```typescript
 * // Input field normalization
 * normalizeField({
 *   type: 'input',
 *   id: 'font-size',
 *   label: 'Font Size',
 *   inputType: 'number',
 *   placeholder: 'Enter size'
 * })
 * // Returns: {
 * //   type: 'input',
 * //   id: 'font-size',
 * //   label: 'Font Size',
 * //   config: { inputType: 'number', placeholder: 'Enter size' }
 * // }
 *
 * // Dropdown field with string options normalization
 * normalizeField({
 *   type: 'dropdown',
 *   label: 'Font Family',
 *   options: ['Arial', 'Helvetica']
 * })
 * // Returns: {
 * //   type: 'dropdown',
 * //   label: 'Font Family',
 * //   config: { options: [{ label: 'Arial', value: 'Arial' }, { label: 'Helvetica', value: 'Helvetica' }] }
 * // }
 * ```
 */
function normalizeField(field: StyleEditorField): StyleEditorFieldSchema {
    const base = {
        type: field.type,
        label: field.label,
        id: field.id
    };

    const config: StyleEditorFieldSchema['config'] = {};

    // Handle type-specific properties
    if (field.type === 'input') {
        config.inputType = field.inputType as unknown as StyleEditorFieldInputType;
        config.placeholder = field.placeholder as unknown as string;
    }

    if (field.type === 'dropdown' || field.type === 'radio') {
        // Normalize options to consistent format
        config.options = field.options.map((opt) =>
            typeof opt === 'string' ? { label: opt, value: opt } : opt
        );

        // Handle radio-specific properties
        if (field.type === 'radio') {
            config.columns = field.columns;
        }
    }

    if (field.type === 'checkboxGroup') {
        // Normalize checkbox options - convert to format expected by UVE
        // Options have label and key - convert key to 'value' for UVE format
        config.options = field.options.map((opt: StyleEditorCheckboxOption) => ({
            label: opt.label,
            value: opt.key // UVE expects 'value' to be the key identifier
        }));
    }

    return { ...base, config };
}

/**
 * Normalizes a section definition into the schema format expected by UVE.
 *
 * Converts a section with a flat array of fields into the normalized schema format
 * where fields are organized as a multi-dimensional array (array of column arrays).
 * Currently, all sections are normalized to a single-column layout structure.
 *
 * **Normalization Process:**
 * 1. Normalizes each field in the section using `normalizeField`
 * 2. Wraps the normalized fields array in an outer array to create the column structure
 * 3. Preserves the section title
 *
 * The output format always uses a multi-dimensional array structure (`fields: StyleEditorFieldSchema[][]`),
 * even for single-column layouts, ensuring consistency in the UVE schema format.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param section - The section definition to normalize, containing a title and array of fields
 * @param section.title - The section title displayed to users
 * @param section.fields - Array of field definitions to normalize
 * @returns The normalized section schema with fields organized as a single-column array structure
 *
 * @example
 * ```typescript
 * normalizeSection({
 *   title: 'Typography',
 *   fields: [
 *     { type: 'input', label: 'Font Size', inputType: 'number' },
 *     { type: 'dropdown', label: 'Font Family', options: ['Arial'] }
 *   ]
 * })
 * // Returns: {
 * //   title: 'Typography',
 * //   fields: [
 * //     [
 * //       { type: 'input', label: 'Font Size', config: { inputType: 'number' } },
 * //       { type: 'dropdown', label: 'Font Family', config: { options: [...] } }
 * //     ]
 * //   ]
 * // }
 * ```
 */
function normalizeSection(section: StyleEditorSection): StyleEditorSectionSchema {
    // Determine if fields is multi-column or single column
    const normalizedFields: StyleEditorFieldSchema[] = section.fields.map(normalizeField);

    return {
        title: section.title,
        fields: normalizedFields
    };
}

/**
 * Normalizes a complete form definition into the schema format expected by UVE.
 *
 * This is the main entry point for converting a developer-friendly form definition
 * into the normalized schema structure that UVE (Universal Visual Editor) can consume.
 * The normalization process transforms the entire form hierarchy:
 *
 * **Normalization Process:**
 * 1. Preserves the `contentType` identifier
 * 2. Processes each section using `normalizeSection`, which:
 *    - Normalizes all fields in the section using `normalizeField`
 *    - Organizes fields into the required multi-dimensional array structure
 * 3. Returns a fully normalized schema with consistent structure across all sections
 *
 * The resulting schema has all field-specific properties moved into `config` objects
 * and all sections using the consistent single-column array structure, regardless
 * of the input format.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param form - The complete form definition to normalize
 * @param form.contentType - The content type identifier this form is associated with
 * @param form.sections - Array of section definitions, each containing a title and fields
 * @returns The normalized form schema ready to be sent to UVE, with all fields and sections normalized
 *
 * @example
 * ```typescript
 * const schema = normalizeForm({
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       fields: [
 *         { type: 'input', id: 'font-size', label: 'Font Size', inputType: 'number' },
 *         { type: 'dropdown', id: 'font-family', label: 'Font Family', options: ['Arial', 'Helvetica'] }
 *       ]
 *     },
 *     {
 *       title: 'Colors',
 *       fields: [
 *         { type: 'input', id: 'primary-color', label: 'Primary Color', inputType: 'text' }
 *       ]
 *     }
 *   ]
 * });
 * // Returns: {
 * //   contentType: 'my-content-type',
 * //   sections: [
 * //     {
 * //       title: 'Typography',
 * //       fields: [
 * //         [
 * //           { type: 'input', id: 'font-size', label: 'Font Size', config: { inputType: 'number' } },
 * //           { type: 'dropdown', id: 'font-family', label: 'Font Family', config: { options: [...] } }
 * //         ]
 * //       ]
 * //     },
 * //     {
 * //       title: 'Colors',
 * //       fields: [
 * //         [
 * //           { type: 'input', id: 'primary-color', label: 'Primary Color', config: { inputType: 'text' } }
 * //         ]
 * //       ]
 * //     }
 * //   ]
 * // }
 * ```
 */
export function normalizeForm(form: StyleEditorForm): StyleEditorFormSchema {
    return {
        contentType: form.contentType,
        sections: form.sections.map(normalizeSection)
    };
}
