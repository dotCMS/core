/**
 * Available field types for the style editor.
 */
export type StyleEditorFieldType = 'input' | 'dropdown' | 'radio' | 'checkboxGroup';

/**
 * Available input types for input fields in the style editor.
 */
export type StyleEditorFieldInputType = 'text' | 'number';

/**
 * Base option object with label and value properties.
 */
export interface StyleEditorOptionObject {
    label: string;
    value: string;
}

/**
 * Extended option object for radio fields with optional image support.
 */
export interface StyleEditorRadioOptionObject extends StyleEditorOptionObject {
    imageURL?: string;
}

/**
 * Option type for dropdown fields.
 */
export type StyleEditorOption = StyleEditorOptionObject;

/**
 * Option type for radio fields (supports optional image).
 */
export type StyleEditorRadioOption = StyleEditorRadioOptionObject;

/**
 * Checkbox option with a key identifier instead of value.
 */
export interface StyleEditorCheckboxOption {
    label: string;
    key: string;
}

/**
 * Configuration object for normalized field schemas sent to UVE.
 */
export interface StyleEditorFieldSchemaConfig {
    inputType?: StyleEditorFieldInputType;
    placeholder?: string;
    options?: StyleEditorRadioOptionObject[];
    columns?: 1 | 2;
}

/**
 * Normalized field schema sent to UVE.
 */
export interface StyleEditorFieldSchema {
    id: string;
    type: StyleEditorFieldType;
    label: string;
    config: StyleEditorFieldSchemaConfig;
}

/**
 * Normalized section schema sent to UVE.
 */
export interface StyleEditorSectionSchema {
    title: string;
    fields: StyleEditorFieldSchema[];
}

/**
 * Complete normalized form schema sent to UVE.
 * This is the output format after processing a style editor form definition.
 */
export interface StyleEditorFormSchema {
    contentType: string;
    sections: StyleEditorSectionSchema[];
}
