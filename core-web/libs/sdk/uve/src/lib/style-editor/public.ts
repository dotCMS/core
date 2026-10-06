import type {
    StyleEditorFieldInputType,
    StyleEditorOption,
    StyleEditorRadioOption
} from '@dotcms/types/internal';

import {
    StyleEditorCheckboxGroupField,
    StyleEditorDropdownField,
    StyleEditorInputField,
    StyleEditorInputFieldConfig,
    StyleEditorRadioField
} from './types';

/**
 * Helper functions for creating style editor field definitions.
 * Used by the dotCMS schema builder UI.
 *
 * @experimental This API is experimental and may be subject to change.
 */
export const styleEditorField = {
    /**
     * Creates an input field definition.
     *
     * @experimental This method is experimental and may be subject to change.
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
     */
    checkboxGroup: (
        config: Omit<StyleEditorCheckboxGroupField, 'type'>
    ): StyleEditorCheckboxGroupField => ({
        type: 'checkboxGroup',
        ...config
    })
};
