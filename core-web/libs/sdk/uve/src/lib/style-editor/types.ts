export type FieldType = 'input' | 'dropdown' | 'radio' | 'checkboxGroup' | 'switch';

export type InputType = 'text' | 'number';

/**
 * Base field definition that all field types extend
 */
export interface StyleEditorBaseField {
    type: FieldType;
    label: string;
}

/**
 * Input field definition
 */
export interface StyleEditorInputField extends StyleEditorBaseField {
    type: 'input';
    inputType?: InputType;
    placeholder?: string;
    defaultValue?: string | number;
    min?: number;
    max?: number;
    pattern?: string;
}

/**
 * Dropdown field definition
 */
export interface StyleEditorDropdownField extends StyleEditorBaseField {
    type: 'dropdown';
    options: Array<string | { label: string; value: string }>;
    defaultValue?: string;
    placeholder?: string;
}

/**
 * Radio button field definition
 */
export interface StyleEditorRadioField extends StyleEditorBaseField {
    type: 'radio';
    options: Array<
        | string
        | {
              label: string;
              value: string;
              imageURL?: string;
              width?: number;
              height?: number;
          }
    >;
    defaultValue?: string;
}

/**
 * Checkbox group field definition - allows multiple options with multiple values
 */
export interface StyleEditorCheckboxGroupField extends StyleEditorBaseField {
    type: 'checkboxGroup';
    options: Array<string | { label: string; value: string }>;
    defaultValue?: Record<string, boolean>;
}

/**
 * Switch field definition
 */
export interface StyleEditorSwitchField extends StyleEditorBaseField {
    type: 'switch';
    defaultValue?: boolean;
}

/**
 * Union type of all possible field definitions
 */
export type StyleEditorField =
    | StyleEditorInputField
    | StyleEditorDropdownField
    | StyleEditorRadioField
    | StyleEditorCheckboxGroupField
    | StyleEditorSwitchField;

/**
 * Section definition - supports single or multi-column layouts
 */
export interface StyleEditorSection {
    title: string;
    // description?: string;
    /**
     * Number of columns (1-4). Default is 1.
     * When columns > 1, fields must be an array of arrays (one per column)
     */
    columns?: number;
    /**
     * Fields in this section.
     * - Single column: array of fields
     * - Multi-column: array of arrays (one array per column)
     */
    fields: StyleEditorField[] | StyleEditorField[][];
}

/**
 * Complete form definition
 */
export interface StyleEditorForm {
    contentType: string;
    sections: StyleEditorSection[];
}

/** ------- FOR UVE -------- */

/**
 * Normalized field output sent to UVE
 */
export interface StyleEditorFieldSchema {
    type: FieldType;
    label: string;
    config: {
        inputType?: InputType;
        placeholder?: string;
        min?: number;
        max?: number;
        pattern?: string;
        options?: Array<{
            label: string;
            value: string;
            imageURL?: string;
            width?: number;
            height?: number;
        }>;
        defaultValue?: string | number | boolean | Record<string, boolean>;
    };
}

/**
 * Normalized section output sent to UVE
 */
export interface StyleEditorSectionSchema {
    title: string;
    columns: number;
    fields: StyleEditorFieldSchema[][];
}

/**
 * Complete form output sent to UVE
 */
export interface StyleEditorFormSchema {
    contentType: string;
    sections: StyleEditorSectionSchema[];
}
