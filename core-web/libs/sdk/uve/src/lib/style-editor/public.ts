import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import type { StyleEditorFormSchema } from '@dotcms/types';

import {
    StyleEditorCheckboxGroupField,
    StyleEditorDropdownField,
    StyleEditorFieldInputType,
    StyleEditorInputField,
    StyleEditorInputFieldConfig,
    StyleEditorOption,
    StyleEditorRadioField,
    StyleEditorRadioOption
} from './types';

import { getUVEState } from '../core/core.utils';
import { sendMessageToUVE } from '../editor/public';

/**
 * Helper functions for creating style editor field definitions.
 *
 * Provides type-safe factory functions for creating different types of form fields
 * used in the style editor. Each function creates a field definition with the
 * appropriate `type` property automatically set.
 *
 * **Available Field Types:**
 * - `input`: Text or number input fields
 * - `dropdown`: Single-value selection from a dropdown list
 * - `radio`: Single-value selection from radio button options (supports visual options with images)
 * - `checkboxGroup`: Multiple-value selection from checkbox options
 *
 * @experimental This API is experimental and may be subject to change.
 */
export const styleEditorField = {
    /**
     * Creates an input field definition.
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @typeParam T - The input type ('text' or 'number'), inferred from `config.inputType`
     * @param config - Input field configuration
     * @returns A complete input field definition with type 'input'
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
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Dropdown field configuration (without the 'type' property)
     * @returns A complete dropdown field definition with type 'dropdown'
     */
    dropdown: (config: Omit<StyleEditorDropdownField, 'type'>): StyleEditorDropdownField => ({
        type: 'dropdown',
        ...config,
        options: config.options as StyleEditorOption[]
    }),

    /**
     * Creates a radio button field definition.
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Radio field configuration (without the 'type' property)
     * @returns A complete radio field definition with type 'radio'
     */
    radio: (config: Omit<StyleEditorRadioField, 'type'>): StyleEditorRadioField => ({
        type: 'radio',
        ...config,
        options: config.options as StyleEditorRadioOption[]
    }),

    /**
     * Creates a checkbox group field definition.
     *
     * @experimental This method is experimental and may be subject to change.
     *
     * @param config - Checkbox group field configuration (without the 'type' property)
     * @returns A complete checkbox group field definition with type 'checkboxGroup'
     */
    checkboxGroup: (
        config: Omit<StyleEditorCheckboxGroupField, 'type'>
    ): StyleEditorCheckboxGroupField => ({
        type: 'checkboxGroup',
        ...config
    })
};

/**
 * Registers style editor form schemas with the UVE editor.
 *
 * Sends normalized style editor schemas to the UVE (Universal Visual Editor)
 * for registration. Only registers schemas when UVE is in EDIT mode.
 *
 * @experimental This method is experimental and may be subject to change.
 *
 * @param schemas - Array of normalized style editor form schemas to register with UVE
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
