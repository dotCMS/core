import { normalizeForm } from './internal';
import {
    StyleEditorFormSchema,
    StyleEditorForm,
    StyleEditorInputField,
    StyleEditorDropdownField,
    StyleEditorRadioField,
    StyleEditorCheckboxGroupField,
    StyleEditorSwitchField
} from './types';

/**
 * Helper functions for creating style editor field definitions.
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
 *           default: 16,
 *           min: 8,
 *           max: 72
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
     * @param config - Input field configuration (without the 'type' property)
     * @returns A complete input field definition
     *
     * @example
     * ```typescript
     * styleEditorField.input({
     *   label: 'Font Size',
     *   inputType: 'number',
     *   placeholder: 'Enter font size',
     *   default: 16,
     *   min: 8,
     *   max: 72
     * })
     * ```
     */
    input: (config: Omit<StyleEditorInputField, 'type'>): StyleEditorInputField => ({
        type: 'input',
        inputType: 'text',
        ...config
    }),

    /**
     * Creates a dropdown field definition.
     *
     * @param config - Dropdown field configuration (without the 'type' property)
     * @returns A complete dropdown field definition
     *
     * @example
     * ```typescript
     * styleEditorField.dropdown({
     *   label: 'Font Family',
     *   options: ['Arial', 'Helvetica', 'Times New Roman'],
     *   default: 'Arial',
     *   placeholder: 'Select a font'
     * })
     * ```
     */
    dropdown: (config: Omit<StyleEditorDropdownField, 'type'>): StyleEditorDropdownField => ({
        type: 'dropdown',
        ...config
    }),

    /**
     * Creates a radio button field definition.
     * Supports background images for visual option selection.
     *
     * @param config - Radio field configuration (without the 'type' property)
     * @returns A complete radio field definition
     *
     * @example
     * ```typescript
     * styleEditorField.radio({
     *   label: 'Theme',
     *   options: [
     *     {
     *       label: 'Light',
     *       value: 'light',
     *       backgroundImage: '/images/light-theme.png',
     *       width: 100,
     *       height: 60
     *     },
     *     { label: 'Dark', value: 'dark' },
     *     'Auto' // string options also supported
     *   ],
     *   default: 'light'
     * })
     * ```
     */
    radio: (config: Omit<StyleEditorRadioField, 'type'>): StyleEditorRadioField => ({
        type: 'radio',
        ...config
    }),

    /**
     * Creates a checkbox group field definition.
     * Allows multiple options to be selected simultaneously.
     *
     * @param config - Checkbox group field configuration (without the 'type' property)
     * @returns A complete checkbox group field definition
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
     *   default: {
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
    }),

    /**
     * Creates a switch (toggle) field definition.
     *
     * @param config - Switch field configuration (without the 'type' property)
     * @returns A complete switch field definition
     *
     * @example
     * ```typescript
     * styleEditorField.switch({
     *   label: 'Enable Animation',
     *   default: false
     * })
     * ```
     */
    switch: (config: Omit<StyleEditorSwitchField, 'type'>): StyleEditorSwitchField => ({
        type: 'switch',
        ...config
    })
};

/**
 * Normalizes and validates a style editor form definition.
 * Converts the developer-friendly form structure into the schema format expected by UVE.
 *
 * @param form - The style editor form definition
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
 *           default: 16
 *         })
 *       ]
 *     }
 *   ]
 * });
 * ```
 */
export function defineStyleEditorForm(form: StyleEditorForm): StyleEditorFormSchema {
    return normalizeForm(form);
}
