/**
 * Available field types for the style editor.
 *
 * Each field type represents a different input control that can be used
 * in the style editor form.
 */
export type FieldType = 'input' | 'dropdown' | 'radio' | 'checkboxGroup' | 'switch';

/**
 * Available input types for text input fields.
 *
 * - 'text': Standard text input for string values
 * - 'number': Numeric input for number values
 */
export type InputType = 'text' | 'number';

/**
 * Base field definition that all field types extend.
 *
 * Provides the common properties shared by all field types in the style editor.
 * All specific field types must include these base properties.
 *
 * @property type - The type of field (discriminator for union types)
 * @property label - The human-readable label displayed for this field
 */
export interface StyleEditorBaseField {
    /** The type of field, used to discriminate between different field types */
    type: FieldType;
    /** The label text displayed to users for this field */
    label: string;
}

/**
 * Input field definition with type-safe defaultValue based on inputType.
 *
 * This is a discriminated union type that ensures type safety between
 * the inputType and defaultValue properties:
 * - When inputType is 'number', defaultValue must be a number
 * - When inputType is 'text', defaultValue must be a string
 *
 * @example
 * ```typescript
 * // Number input with number defaultValue
 * const numberField: StyleEditorInputField = {
 *   type: 'input',
 *   label: 'Font Size',
 *   inputType: 'number',
 *   defaultValue: 16
 * };
 *
 * // Text input with string defaultValue
 * const textField: StyleEditorInputField = {
 *   type: 'input',
 *   label: 'Font Name',
 *   inputType: 'text',
 *   defaultValue: 'Arial'
 * };
 * ```
 */
export type StyleEditorInputField =
    | (StyleEditorBaseField & {
          /** Discriminator: must be 'input' */
          type: 'input';
          /** Input type for number values */
          inputType: 'number';
          /** Optional placeholder text shown when the input is empty */
          placeholder?: string;
          /** Optional default value (must be a number when inputType is 'number') */
          defaultValue?: number;
      })
    | (StyleEditorBaseField & {
          /** Discriminator: must be 'input' */
          type: 'input';
          /** Input type for text/string values */
          inputType: 'text';
          /** Optional placeholder text shown when the input is empty */
          placeholder?: string;
          /** Optional default value (must be a string when inputType is 'text') */
          defaultValue?: string;
      });

/**
 * Dropdown field definition for single-value selection.
 *
 * Allows users to select a single option from a dropdown list.
 * Options can be provided as simple strings or as objects with
 * separate label and value properties for more flexibility.
 *
 * @property type - Must be 'dropdown'
 * @property options - Array of selectable options. Can be strings or objects with label/value
 * @property defaultValue - Optional default selected value (must match one of the option values)
 * @property placeholder - Optional placeholder text shown when no value is selected
 *
 * @example
 * ```typescript
 * const dropdownField: StyleEditorDropdownField = {
 *   type: 'dropdown',
 *   label: 'Font Family',
 *   options: ['Arial', 'Helvetica', { label: 'Times New Roman', value: 'times' }],
 *   defaultValue: 'Arial',
 *   placeholder: 'Select a font'
 * };
 * ```
 */
export interface StyleEditorDropdownField extends StyleEditorBaseField {
    /** Discriminator: must be 'dropdown' */
    type: 'dropdown';
    /** Array of selectable options. Can be strings or objects with label and value properties */
    options: Array<string | { label: string; value: string }>;
    /** Optional default selected value (must match one of the option values) */
    defaultValue?: string;
    /** Optional placeholder text shown when no value is selected */
    placeholder?: string;
}

/**
 * Radio button field definition for single-value selection with visual options.
 *
 * Allows users to select a single option from a radio button group.
 * Supports visual options with background images for enhanced UI/UX.
 * Options can be provided as simple strings or as objects with label,
 * value, and optional image properties.
 *
 * @property type - Must be 'radio'
 * @property options - Array of selectable options. Can be strings or objects with label, value, and optional image properties
 * @property defaultValue - Optional default selected value (must match one of the option values)
 *
 * @example
 * ```typescript
 * const radioField: StyleEditorRadioField = {
 *   type: 'radio',
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
 *     'Auto' // Simple string options also supported
 *   ],
 *   defaultValue: 'light'
 * };
 * ```
 */
export interface StyleEditorRadioField extends StyleEditorBaseField {
    /** Discriminator: must be 'radio' */
    type: 'radio';
    /**
     * Array of selectable options. Can be:
     * - Simple strings (used as both label and value)
     * - Objects with label, value, and optional imageURL, width, height for visual options
     */
    options: Array<
        | string
        | {
              /** Display label for the option */
              label: string;
              /** Value returned when this option is selected */
              value: string;
              /** Optional URL to an image displayed for this option */
              imageURL?: string;
              /** Optional width of the image in pixels */
              width?: number;
              /** Optional height of the image in pixels */
              height?: number;
          }
    >;
    /** Optional default selected value (must match one of the option values) */
    defaultValue?: string;
}

/**
 * Checkbox group field definition for multiple-value selection.
 *
 * Allows users to select multiple options simultaneously. Each option
 * can be independently checked or unchecked. The defaultValue is a
 * record mapping option values to their boolean checked state.
 *
 * @property type - Must be 'checkboxGroup'
 * @property options - Array of selectable options. Can be strings or objects with label/value
 * @property defaultValue - Optional default checked state as a record mapping option values to boolean
 *
 * @example
 * ```typescript
 * const checkboxField: StyleEditorCheckboxGroupField = {
 *   type: 'checkboxGroup',
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
 * };
 * ```
 */
export interface StyleEditorCheckboxGroupField extends StyleEditorBaseField {
    /** Discriminator: must be 'checkboxGroup' */
    type: 'checkboxGroup';
    /** Array of selectable options. Can be strings or objects with label and value properties */
    options: Array<string | { label: string; value: string }>;
    /**
     * Optional default checked state as a record mapping option values to boolean.
     * Keys should match the option values, values indicate whether the option is checked.
     */
    defaultValue?: Record<string, boolean>;
}

/**
 * Union type of all possible field definitions.
 *
 * Represents any valid field type that can be used in a style editor form.
 * Use this type when you need to work with fields of unknown or mixed types.
 *
 * @example
 * ```typescript
 * const fields: StyleEditorField[] = [
 *   { type: 'input', label: 'Font Size', inputType: 'number', defaultValue: 16 },
 *   { type: 'dropdown', label: 'Font Family', options: ['Arial', 'Helvetica'] },
 *   { type: 'radio', label: 'Theme', options: ['Light', 'Dark'] },
 *   { type: 'checkboxGroup', label: 'Styles', options: ['Bold', 'Italic'] }
 * ];
 * ```
 */
export type StyleEditorField =
    | StyleEditorInputField
    | StyleEditorDropdownField
    | StyleEditorRadioField
    | StyleEditorCheckboxGroupField;

/**
 * Section definition for organizing fields in a style editor form.
 *
 * Supports both single-column and multi-column layouts. Sections group
 * related fields together with a title. When using multiple columns,
 * fields are organized as an array of arrays, where each inner array
 * represents the fields in one column.
 *
 * @property title - The section title displayed to users
 * @property columns - Optional number of columns (1-4). Defaults to 1 if not specified
 * @property fields - Fields in this section. Structure depends on column count
 *
 * @example
 * ```typescript
 * // Single column section
 * const singleColumnSection: StyleEditorSection = {
 *   title: 'Typography',
 *   fields: [
 *     { type: 'input', label: 'Font Size', inputType: 'number' },
 *     { type: 'dropdown', label: 'Font Family', options: ['Arial'] }
 *   ]
 * };
 *
 * // Multi-column section
 * const multiColumnSection: StyleEditorSection = {
 *   title: 'Layout',
 *   columns: 2,
 *   fields: [
 *     // Column 1
 *     [
 *       { type: 'input', label: 'Width', inputType: 'number' }
 *     ],
 *     // Column 2
 *     [
 *       { type: 'input', label: 'Height', inputType: 'number' }
 *     ]
 *   ]
 * };
 * ```
 */
export interface StyleEditorSection {
    /** The section title displayed to users */
    title: string;
    /**
     * Number of columns for this section (1-4). Default is 1.
     * When columns > 1, fields must be an array of arrays (one array per column).
     * Each column array contains the fields for that column.
     */
    columns?: number;
    /**
     * Fields in this section.
     * - Single column (columns = 1 or undefined): array of fields
     * - Multi-column (columns > 1): array of arrays, where each inner array represents one column
     */
    fields: StyleEditorField[] | StyleEditorField[][];
}

/**
 * Complete style editor form definition.
 *
 * Represents the full structure of a style editor form, including
 * the content type identifier and all sections with their fields.
 * This is the developer-friendly format used to define forms before
 * they are normalized and sent to UVE.
 *
 * @property contentType - The content type identifier this form is associated with
 * @property sections - Array of sections, each containing a title and fields
 *
 * @example
 * ```typescript
 * const form: StyleEditorForm = {
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       fields: [
 *         { type: 'input', label: 'Font Size', inputType: 'number', defaultValue: 16 },
 *         { type: 'dropdown', label: 'Font Family', options: ['Arial', 'Helvetica'] }
 *       ]
 *     },
 *     {
 *       title: 'Colors',
 *       columns: 2,
 *       fields: [
 *         [{ type: 'input', label: 'Primary Color', inputType: 'text' }],
 *         [{ type: 'input', label: 'Secondary Color', inputType: 'text' }]
 *       ]
 *     }
 *   ]
 * };
 * ```
 */
export interface StyleEditorForm {
    /** The content type identifier this form is associated with */
    contentType: string;
    /** Array of sections, each containing a title and fields */
    sections: StyleEditorSection[];
}

/**
 * ============================================================================
 * UVE Style Editor Schema Types
 * ============================================================================
 *
 * The following types represent the normalized schema format sent to UVE
 * (Universal Visual Editor). These are the output types after processing
 * and normalizing the developer-friendly StyleEditorForm definitions.
 * ============================================================================
 */

/**
 * Normalized field schema sent to UVE.
 *
 * This is the transformed format of field definitions after normalization.
 * All field-specific properties are moved into a `config` object, and
 * the structure is flattened for UVE consumption.
 *
 * @property type - The field type identifier
 * @property label - The field label
 * @property config - Object containing all field-specific configuration
 * @property config.inputType - Optional input type for input fields ('text' or 'number')
 * @property config.placeholder - Optional placeholder text
 * @property config.min - Optional minimum value for number inputs
 * @property config.max - Optional maximum value for number inputs
 * @property config.pattern - Optional regex pattern for validation
 * @property config.options - Optional array of options for dropdown/radio/checkbox fields
 * @property config.defaultValue - Optional default value (type varies by field type)
 */
export interface StyleEditorFieldSchema {
    /** The field type identifier */
    type: FieldType;
    /** The field label */
    label: string;
    /** Object containing all field-specific configuration */
    config: {
        /** Optional input type for input fields ('text' or 'number') */
        inputType?: InputType;
        /** Optional placeholder text shown when the field is empty */
        placeholder?: string;
        /** Optional minimum value constraint for number inputs */
        min?: number;
        /** Optional maximum value constraint for number inputs */
        max?: number;
        /** Optional regex pattern for text input validation */
        pattern?: string;
        /**
         * Optional array of options for dropdown, radio, and checkbox fields.
         * Each option can include label, value, and optional image properties.
         */
        options?: Array<{
            /** Display label for the option */
            label: string;
            /** Value returned when this option is selected */
            value: string;
            /** Optional URL to an image displayed for this option */
            imageURL?: string;
            /** Optional width of the image in pixels */
            width?: number;
            /** Optional height of the image in pixels */
            height?: number;
        }>;
        /**
         * Optional default value. Type depends on field type:
         * - Input fields: string or number
         * - Switch fields: boolean
         * - Checkbox groups: Record<string, boolean>
         */
        defaultValue?: string | number | boolean | Record<string, boolean>;
    };
}

/**
 * Normalized section schema sent to UVE.
 *
 * Represents a section in the normalized UVE format. All sections
 * use a consistent multi-column structure (array of arrays), even
 * for single-column layouts.
 *
 * @property title - The section title
 * @property columns - Number of columns (always present, defaults to 1)
 * @property fields - Array of column arrays, where each inner array contains fields for that column
 *
 * @example
 * ```typescript
 * const sectionSchema: StyleEditorSectionSchema = {
 *   title: 'Typography',
 *   columns: 2,
 *   fields: [
 *     // Column 1 fields
 *     [
 *       { type: 'input', label: 'Font Size', config: { inputType: 'number' } }
 *     ],
 *     // Column 2 fields
 *     [
 *       { type: 'dropdown', label: 'Font Family', config: { options: [...] } }
 *     ]
 *   ]
 * };
 * ```
 */
export interface StyleEditorSectionSchema {
    /** The section title */
    title: string;
    /** Number of columns in this section (always present, defaults to 1) */
    columns: number;
    /** Array of column arrays, where each inner array contains the fields for that column */
    fields: StyleEditorFieldSchema[][];
}

/**
 * Complete normalized form schema sent to UVE.
 *
 * This is the final output format after normalizing a StyleEditorForm.
 * The form structure is transformed into a consistent schema format
 * that UVE can consume. All sections use the multi-column structure,
 * and all field properties are normalized into the config object.
 *
 * @property contentType - The content type identifier
 * @property sections - Array of normalized section schemas
 *
 * @example
 * ```typescript
 * const formSchema: StyleEditorFormSchema = {
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       columns: 1,
 *       fields: [
 *         [
 *           { type: 'input', label: 'Font Size', config: { inputType: 'number', defaultValue: 16 } },
 *           { type: 'dropdown', label: 'Font Family', config: { options: [...] } }
 *         ]
 *       ]
 *     }
 *   ]
 * };
 * ```
 */
export interface StyleEditorFormSchema {
    /** The content type identifier this form is associated with */
    contentType: string;
    /** Array of normalized section schemas */
    sections: StyleEditorSectionSchema[];
}
