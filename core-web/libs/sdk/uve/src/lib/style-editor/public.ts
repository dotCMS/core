import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';

import { normalizeForm } from './internal';
import {
    StyleEditorFormSchema,
    StyleEditorForm,
    StyleEditorInputField,
    StyleEditorDropdownField,
    StyleEditorRadioField,
    StyleEditorCheckboxGroupField,
    StyleEditorFieldInputType,
    StyleEditorInputFieldConfig
} from './types';

import { getUVEState } from '../core/core.utils';
import { sendMessageToUVE } from '../editor/public';

/**
 * Helper functions for creating style editor field definitions.
 *
 * Provides type-safe factory functions for creating different types of form fields
 * used in the style editor. Each function creates a field definition with the
 * appropriate `type` property automatically set, eliminating the need to manually
 * specify the type discriminator.
 *
 * **Available Field Types:**
 * - `input`: Text or number input fields
 * - `dropdown`: Single-value selection from a dropdown list
 * - `radio`: Single-value selection from radio button options (supports visual options with images)
 * - `checkboxGroup`: Multiple-value selection from checkbox options
 *
 * These factory functions ensure type safety by inferring the correct field type
 * based on the configuration provided, and they automatically set the `type` property
 * to match the factory function used.
 *
 * @experimental This API is experimental and may be subject to change.
 *
 * @example
 * ```typescript
 * const form = defineStyleEditorSchema({
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       fields: [
 *         styleEditorField.input({
 *           label: 'Font Size',
 *           inputType: 'number',
 *           defaultValue: 16
 *         }),
 *         styleEditorField.dropdown({
 *           label: 'Font Family',
 *           options: ['Arial', 'Helvetica'],
 *           defaultValue: 'Arial'
 *         }),
 *         styleEditorField.radio({
 *           label: 'Alignment',
 *           options: ['Left', 'Center', 'Right'],
 *           defaultValue: 'Left'
 *         })
 *       ]
 *     }
 *   ]
 * });
 * ```
 */
export const styleEditorField = {
    /**
     * Creates an input field definition with type-safe default values.
     *
     * Supports both text and number input types. The `defaultValue` type is
     * enforced based on the `inputType` using TypeScript generics:
     * - When `inputType` is `'number'`, `defaultValue` must be a `number`
     * - When `inputType` is `'text'`, `defaultValue` must be a `string`
     *
     * This provides compile-time type checking to prevent mismatched types,
     * such as passing a string when a number is expected.
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @typeParam T - The input type ('text' or 'number'), inferred from `config.inputType`
     * @param config - Input field configuration
     * @param config.label - The label displayed for this input field
     * @param config.inputType - The type of input ('text' or 'number')
     * @param config.placeholder - Optional placeholder text for the input
     * @param config.defaultValue - Optional default value (type enforced based on inputType)
     * @returns A complete input field definition with type 'input'
     *
     * @example
     * ```typescript
     * // Number input - defaultValue must be a number
     * styleEditorField.input({
     *   label: 'Font Size',
     *   inputType: 'number',
     *   placeholder: 'Enter font size',
     *   defaultValue: 16 // ✓ Correct: number
     * })
     *
     * // Text input - defaultValue must be a string
     * styleEditorField.input({
     *   label: 'Font Name',
     *   inputType: 'text',
     *   placeholder: 'Enter font name',
     *   defaultValue: 'Arial' // ✓ Correct: string
     * })
     *
     * // TypeScript error - type mismatch
     * styleEditorField.input({
     *   label: 'Font Size',
     *   inputType: 'number',
     *   defaultValue: '16' // ✗ Error: Type 'string' is not assignable to type 'number'
     * })
     * ```
     */
    input: <T extends StyleEditorFieldInputType>(
        config: StyleEditorInputFieldConfig<T>
    ): StyleEditorInputField =>
        ({
            type: 'input',
            ...config
        }) as StyleEditorInputField,

    /**
     * Creates a dropdown field definition.
     *
     * Allows users to select a single value from a list of options.
     * Options can be provided as simple strings or as objects with label and value.
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Dropdown field configuration (without the 'type' property)
     * @param config.label - The label displayed for this dropdown field
     * @param config.options - Array of options. Can be strings or objects with label and value
     * @param config.defaultValue - Optional default selected value (must match one of the option values)
     * @param config.placeholder - Optional placeholder text shown when no value is selected
     * @returns A complete dropdown field definition with type 'dropdown'
     *
     * @example
     * ```typescript
     * // Simple string options
     * styleEditorField.dropdown({
     *   label: 'Font Family',
     *   options: ['Arial', 'Helvetica', 'Times New Roman'],
     *   defaultValue: 'Arial',
     *   placeholder: 'Select a font'
     * })
     *
     * // Object options with custom labels
     * styleEditorField.dropdown({
     *   label: 'Theme',
     *   options: [
     *     { label: 'Light Theme', value: 'light' },
     *     { label: 'Dark Theme', value: 'dark' }
     *   ],
     *   defaultValue: 'light'
     * })
     * ```
     */
    dropdown: (config: Omit<StyleEditorDropdownField, 'type'>): StyleEditorDropdownField => ({
        type: 'dropdown',
        ...config
    }),

    /**
     * Creates a radio button field definition.
     *
     * Allows users to select a single option from a list. Supports visual
     * options with background images for enhanced UI. Options can be provided
     * as simple strings or as objects with label, value, and optional image properties.
     *
     * **Layout Options:**
     * - `columns: 1` (default): Single column list layout
     * - `columns: 2`: Two-column grid layout, ideal for visual options with images
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Radio field configuration (without the 'type' property)
     * @param config.label - The label displayed for this radio group
     * @param config.options - Array of options. Can be strings or objects with label, value, and optional imageURL
     * @param config.defaultValue - Optional default selected value (must match one of the option values)
     * @param config.columns - Optional number of columns (1 or 2). Defaults to 1 (single column)
     * @returns A complete radio field definition with type 'radio'
     *
     * @example
     * ```typescript
     * // Simple string options (single column)
     * styleEditorField.radio({
     *   label: 'Alignment',
     *   options: ['Left', 'Center', 'Right'],
     *   defaultValue: 'Left'
     * })
     *
     * // Two-column grid layout with images
     * styleEditorField.radio({
     *   label: 'Layout',
     *   columns: 2,
     *   options: [
     *     {
     *       label: 'Left',
     *       value: 'left',
     *       imageURL: 'https://example.com/layout-left.png',
     *     },
     *     {
     *       label: 'Right',
     *       value: 'right',
     *       imageURL: 'https://example.com/layout-right.png',
     *     },
     *     { label: 'Center', value: 'center' },
     *     { label: 'Overlap', value: 'overlap' }
     *   ],
     *   defaultValue: 'right'
     * })
     * ```
     */
    radio: (config: Omit<StyleEditorRadioField, 'type'>): StyleEditorRadioField => ({
        type: 'radio',
        ...config
    }),

    /**
     * Creates a checkbox group field definition.
     *
     * Allows users to select multiple options simultaneously. Each option
     * can be independently checked or unchecked. The defaultValue is a
     * record mapping option values to their boolean checked state.
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Checkbox group field configuration (without the 'type' property)
     * @param config.label - The label displayed for this checkbox group
     * @param config.options - Array of options. Can be strings or objects with label and value
     * @param config.defaultValue - Optional default checked state as a record of option values to boolean
     * @returns A complete checkbox group field definition with type 'checkboxGroup'
     *
     * @example
     * ```typescript
     * styleEditorField.checkboxGroup({
     *   label: 'Text Decoration',
     *   options: [
     *     { label: 'Underline', value: 'underline' },
     *     { label: 'Overline', value: 'overline' },
     *     { label: 'Line Through', value: 'line-through' }
     *   ],
     *   defaultValue: {
     *     'underline': true,
     *     'overline': false,
     *     'line-through': false
     *   }
     * })
     * ```
     */
    checkboxGroup: (
        config: Omit<StyleEditorCheckboxGroupField, 'type'>
    ): StyleEditorCheckboxGroupField => ({
        type: 'checkboxGroup',
        ...config
    })
};

/**
 * Normalizes and validates a style editor form definition.
 *
 * Converts the developer-friendly form structure into the schema format
 * expected by UVE (Universal Visual Editor). This function processes the
 * form definition and transforms it into the normalized schema format where:
 *
 * - All field-specific properties are moved into `config` objects
 * - String options are normalized to `{ label, value }` objects
 * - Sections are organized into the multi-dimensional array structure required by UVE
 *
 * The normalization process ensures consistency and type safety in the schema
 * format sent to UVE. After normalization, use `registerStyleEditorSchemas`
 * to register the schema with the UVE editor.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param form - The style editor form definition containing contentType and sections
 * @param form.contentType - The content type identifier for this form
 * @param form.sections - Array of sections, each containing a title and fields array
 * @returns The normalized form schema ready to be sent to UVE
 *
 * @example
 * ```typescript
 * const formSchema = defineStyleEditorSchema({
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       fields: [
 *         styleEditorField.input({
 *           label: 'Font Size',
 *           inputType: 'number',
 *           defaultValue: 16
 *         }),
 *         styleEditorField.dropdown({
 *           label: 'Font Family',
 *           options: ['Arial', 'Helvetica'],
 *           defaultValue: 'Arial'
 *         })
 *       ]
 *     },
 *     {
 *       title: 'Colors',
 *       fields: [
 *         styleEditorField.input({
 *           label: 'Primary Color',
 *           inputType: 'text',
 *           defaultValue: '#000000'
 *         }),
 *         styleEditorField.input({
 *           label: 'Secondary Color',
 *           inputType: 'text',
 *           defaultValue: '#FFFFFF'
 *         })
 *       ]
 *     }
 *   ]
 * });
 *
 * // Register the schema with UVE
 * registerStyleEditorSchemas([formSchema]);
 * ```
 */
export function defineStyleEditorSchema(form: StyleEditorForm): StyleEditorFormSchema {
    return normalizeForm(form);
}

/**
 * Registers style editor form schemas with the UVE editor.
 *
 * Sends normalized style editor schemas to the UVE (Universal Visual Editor)
 * for registration. The schemas must be normalized using `defineStyleEditorSchema`
 * before being passed to this function.
 *
 * **Behavior:**
 * - Only registers schemas when UVE is in EDIT mode
 * - Validates that each schema has a `contentType` property
 * - Skips schemas without `contentType` and logs a warning
 * - Sends the validated schemas to UVE via the `REGISTER_STYLE_SCHEMAS` action
 *
 * **Note:** This function will silently return early if UVE is not in EDIT mode,
 * so it's safe to call even when the editor is not active.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param schemas - Array of normalized style editor form schemas to register with UVE
 * @returns void - This function does not return a value
 *
 * @example
 * ```typescript
 * // Create and normalize a form schema
 * const formSchema = defineStyleEditorSchema({
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       fields: [
 *         styleEditorField.input({
 *           label: 'Font Size',
 *           inputType: 'number',
 *           defaultValue: 16
 *         })
 *       ]
 *     }
 *   ]
 * });
 *
 * // Register the schema with UVE
 * registerStyleEditorSchemas([formSchema]);
 *
 * // Register multiple schemas at once
 * const schema1 = defineStyleEditorSchema({ ... });
 * const schema2 = defineStyleEditorSchema({ ... });
 * registerStyleEditorSchemas([schema1, schema2]);
 * ```
 */
export function registerStyleEditorSchemas(schemas: StyleEditorFormSchema[]): void {
    const { mode } = getUVEState() || {};

    if (!mode || mode !== UVE_MODE.EDIT) {
        return;
    }

    const validatedSchemas = schemas.filter((schema, index) => {
        if (!schema.contentType) {
            console.warn(
                `[registerStyleEditorSchemas] Skipping schema with index [${index}] for not having a contentType`
            );
            return false;
        }
        return true;
    });

    sendMessageToUVE({
        action: DotCMSUVEAction.REGISTER_STYLE_SCHEMAS,
        payload: {
            schemas: validatedSchemas
        }
    });
}
