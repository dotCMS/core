import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';

import { normalizeForm } from './internal';
import {
    StyleEditorFormSchema,
    StyleEditorForm,
    StyleEditorInputField,
    StyleEditorDropdownField,
    StyleEditorRadioField,
    StyleEditorCheckboxGroupField
} from './types';

import { getUVEState } from '../core/core.utils';
import { sendMessageToUVE } from '../editor/public';

/**
 * Helper functions for creating style editor field definitions.
 *
 * Provides type-safe factory functions for creating different types of form fields
 * used in the style editor. Each function creates a field definition with the
 * appropriate type property automatically set.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @example
 * ```typescript
 * const form = defineStyleEditorForm({
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
 *     }
 *   ]
 * });
 * ```
 */
export const styleEditorField = {
    /**
     * Creates an input field definition.
     *
     * Supports both text and number input types. The defaultValue type is
     * automatically inferred based on the inputType:
     * - When inputType is 'number', defaultValue must be a number
     * - When inputType is 'text', defaultValue must be a string
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Input field configuration (without the 'type' property)
     * @param config.label - The label displayed for this input field
     * @param config.inputType - The type of input ('text' or 'number')
     * @param config.placeholder - Optional placeholder text for the input
     * @param config.defaultValue - Optional default value (type depends on inputType)
     * @returns A complete input field definition with type 'input'
     *
     * @example
     * ```typescript
     * // Number input
     * styleEditorField.input({
     *   label: 'Font Size',
     *   inputType: 'number',
     *   placeholder: 'Enter font size',
     *   defaultValue: 16
     * })
     *
     * // Text input
     * styleEditorField.input({
     *   label: 'Font Name',
     *   inputType: 'text',
     *   placeholder: 'Enter font name',
     *   defaultValue: 'Arial'
     * })
     * ```
     */
    input: (config: Omit<StyleEditorInputField, 'type'>): StyleEditorInputField =>
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
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Radio field configuration (without the 'type' property)
     * @param config.label - The label displayed for this radio group
     * @param config.options - Array of options. Can be strings or objects with label, value, and optional imageURL, width, height
     * @param config.defaultValue - Optional default selected value (must match one of the option values)
     * @returns A complete radio field definition with type 'radio'
     *
     * @example
     * ```typescript
     * // Simple string options
     * styleEditorField.radio({
     *   label: 'Alignment',
     *   options: ['Left', 'Center', 'Right'],
     *   defaultValue: 'Left'
     * })
     *
     * // Options with images
     * styleEditorField.radio({
     *   label: 'Theme',
     *   options: [
     *     {
     *       label: 'Light',
     *       value: 'light',
     *       imageURL: 'https://example.com/light-theme.png',
     *       width: 100,
     *       height: 60
     *     },
     *     { label: 'Dark', value: 'dark' },
     *     'Auto' // string options also supported
     *   ],
     *   defaultValue: 'light'
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
 * form definition, validates it, and transforms it into the normalized
 * schema format.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param form - The style editor form definition containing contentType and sections
 * @param form.contentType - The content type identifier for this form
 * @param form.sections - Array of sections, each containing a title and fields
 * @returns The normalized form schema ready to be sent to UVE
 *
 * @example
 * ```typescript
 * const formSchema = defineStyleEditorForm({
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
 *       columns: 2,
 *       fields: [
 *         [
 *           styleEditorField.input({
 *             label: 'Primary Color',
 *             inputType: 'text',
 *             defaultValue: '#000000'
 *           })
 *         ],
 *         [
 *           styleEditorField.input({
 *             label: 'Secondary Color',
 *             inputType: 'text',
 *             defaultValue: '#FFFFFF'
 *           })
 *         ]
 *       ]
 *     }
 *   ]
 * });
 * ```
 */
export function defineStyleEditorForm(form: StyleEditorForm): StyleEditorFormSchema {
    return normalizeForm(form);
}

/**
 * Registers style editor forms with the UVE editor.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param forms - Array of style editor form schemas to register
 * @param options - Optional configuration
 * @param options.force - Force re-registration even if forms already registered
 */
export function registerStyleEditorSchemas(schemas: StyleEditorFormSchema[]): void {
    const { mode } = getUVEState() || {};

    if (!mode || mode !== UVE_MODE.EDIT) {
        return;
    }

    const validatedSchemas = schemas.filter((schema, index) => {
        if (!schema.contentType) {
            console.warn(
                `[registerStyleEditorSchemas] Skipping form with index [${index}] for not having a contentType`
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
