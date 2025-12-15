/**
 * Available field types for the style editor.
 *
 * Each field type represents a different input control that can be used
 * in the style editor form. Currently supported field types:
 * - `input`: Text or number input fields
 * - `dropdown`: Single-value selection from a dropdown list
 * - `radio`: Single-value selection from radio button options
 * - `checkboxGroup`: Multiple-value selection from checkbox options
 * - `switch`: Boolean toggle switch (reserved for future implementation)
 */
export type StyleEditorFieldType = 'input' | 'dropdown' | 'radio' | 'checkboxGroup';

/**
 * Available input types for input fields in the style editor.
 *
 * Determines the type of input control and the expected value type:
 * - `'text'`: Standard text input for string values (e.g., font names, color codes)
 * - `'number'`: Numeric input for number values (e.g., font sizes, dimensions)
 *
 * The input type is used in conjunction with `StyleEditorInputField` to ensure
 * type safety between the input type and the default value.
 */
export type StyleEditorFieldInputType = 'text' | 'number';

/**
 * Helper type that maps input types to their corresponding default value types.
 *
 * This conditional type ensures type safety between `inputType` and `defaultValue`:
 * - When `T` is `'number'`, the mapped type is `number`
 * - When `T` is `'text'`, the mapped type is `string`
 *
 * Used by `StyleEditorInputFieldConfig` to enforce the correct default value type
 * based on the input type.
 */
export type StyleEditorInputValueType<T extends StyleEditorFieldInputType> = T extends 'number'
    ? number
    : string;

/**
 * Configuration type for creating input fields with type-safe default values.
 *
 * This generic type ensures that the `defaultValue` type matches the `inputType`:
 * - When `inputType` is `'number'`, `defaultValue` must be a `number`
 * - When `inputType` is `'text'`, `defaultValue` must be a `string`
 *
 * This type is used by `styleEditorField.input()` to provide compile-time
 * type checking and prevent mismatched input types and default values.
 *
 * @typeParam T - The input type ('text' or 'number')
 *
 * @example
 * ```typescript
 * // Valid: number inputType with number defaultValue
 * const config: StyleEditorInputFieldConfig<'number'> = {
 *   label: 'Font Size',
 *   inputType: 'number',
 *   defaultValue: 16 // ✓ Correct: number
 * };
 *
 * // TypeScript error: string not assignable to number
 * const invalid: StyleEditorInputFieldConfig<'number'> = {
 *   label: 'Font Size',
 *   inputType: 'number',
 *   defaultValue: '16' // ✗ Error: Type 'string' is not assignable to type 'number'
 * };
 * ```
 */
export interface StyleEditorInputFieldConfig<T extends StyleEditorFieldInputType> {
    /** The unique identifier for this field */
    id: string;
    /** The label text displayed to users for this field */
    label: string;
    /** The input type that determines the value type ('text' for strings, 'number' for numbers) */
    inputType: T;
    /** Optional placeholder text shown when the input is empty */
    placeholder?: string;
    /** Optional default value (type is enforced based on inputType: number for 'number', string for 'text') */
    defaultValue?: StyleEditorInputValueType<T>;
}

/**
 * Base option object with label and value properties.
 *
 * Used in dropdown, radio, and checkbox group fields to define
 * selectable options with separate display labels and values.
 *
 * @property label - Display label shown to users
 * @property value - Value returned when this option is selected
 */
export interface StyleEditorOptionObject {
    /** Display label shown to users */
    label: string;
    /** Value returned when this option is selected */
    value: string;
}

/**
 * Extended option object for radio fields with visual properties.
 *
 * Extends the base option with optional image properties for
 * creating visual radio button options (e.g., layout selectors with preview images).
 *
 * @property label - Display label shown to users
 * @property value - Value returned when this option is selected
 * @property imageURL - Optional URL to an image displayed for this option
 * @property width - Optional width of the image in pixels
 * @property height - Optional height of the image in pixels
 */
export interface StyleEditorRadioOptionObject extends StyleEditorOptionObject {
    /** Optional URL to an image displayed for this option */
    imageURL?: string;
    /** Optional width of the image in pixels */
    width?: number;
    /** Optional height of the image in pixels */
    height?: number;
}

/**
 * Option type for dropdown and checkbox group fields.
 *
 * Can be a simple string (used as both label and value)
 * or an object with separate label and value properties.
 *
 * @example
 * ```typescript
 * // String option - 'Arial' is used as both label and value
 * const stringOption: StyleEditorOption = 'Arial';
 *
 * // Object option - separate label and value
 * const objectOption: StyleEditorOption = {
 *   label: 'Times New Roman',
 *   value: 'times'
 * };
 * ```
 */
export type StyleEditorOption = string | StyleEditorOptionObject;

/**
 * Option type for radio fields with visual support.
 *
 * Can be a simple string (used as both label and value)
 * or an object with label, value, and optional image properties.
 *
 * @example
 * ```typescript
 * // String option
 * const stringOption: StyleEditorRadioOption = 'Left';
 *
 * // Object option with image
 * const imageOption: StyleEditorRadioOption = {
 *   label: 'Left Layout',
 *   value: 'left',
 *   imageURL: 'https://example.com/left-layout.png',
 *   width: 80,
 *   height: 50
 * };
 * ```
 */
export type StyleEditorRadioOption = string | StyleEditorRadioOptionObject;

/**
 * Default value type for checkbox group fields.
 *
 * A record mapping option values to their boolean checked state.
 * Keys should match the option values, values indicate whether the option is checked.
 *
 * @example
 * ```typescript
 * const defaultState: StyleEditorCheckboxDefaultValue = {
 *   'underline': true,
 *   'overline': false,
 *   'line-through': false
 * };
 * ```
 */
export type StyleEditorCheckboxDefaultValue = Record<string, boolean>;

/**
 * Base field definition that all field types extend.
 *
 * Provides the common properties shared by all field types in the style editor.
 * All specific field types must include these base properties. The `type` property
 * serves as a discriminator that allows TypeScript to narrow union types based on
 * the field type.
 *
 * @property type - The type of field (discriminator for union types, enables type narrowing)
 * @property label - The human-readable label displayed for this field in the UI
 */
export interface StyleEditorBaseField {
    /** The unique identifier for this field */
    id: string;
    /** The type of field, used to discriminate between different field types in union types */
    type: StyleEditorFieldType;
    /** The label text displayed to users for this field */
    label: string;
}

/**
 * Input field definition with type-safe defaultValue based on inputType.
 *
 * This is a discriminated union type that ensures type safety between
 * the `inputType` and `defaultValue` properties. TypeScript will enforce
 * that the `defaultValue` type matches the `inputType`:
 * - When `inputType` is `'number'`, `defaultValue` must be a `number`
 * - When `inputType` is `'text'`, `defaultValue` must be a `string`
 *
 * This type safety prevents runtime errors and ensures that numeric inputs
 * receive numeric values and text inputs receive string values.
 *
 * @example
 * ```typescript
 * // Number input with number defaultValue
 * const numberField: StyleEditorInputField = {
 *   type: 'input',
 *   label: 'Font Size',
 *   inputType: 'number',
 *   defaultValue: 16, // TypeScript ensures this is a number
 *   placeholder: 'Enter font size'
 * };
 *
 * // Text input with string defaultValue
 * const textField: StyleEditorInputField = {
 *   type: 'input',
 *   label: 'Font Name',
 *   inputType: 'text',
 *   defaultValue: 'Arial', // TypeScript ensures this is a string
 *   placeholder: 'Enter font name'
 * };
 *
 * // TypeScript error: type mismatch
 * // const invalid: StyleEditorInputField = {
 * //   type: 'input',
 * //   label: 'Size',
 * //   inputType: 'number',
 * //   defaultValue: '16' // Error: string not assignable to number
 * // };
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
    options: StyleEditorOption[];
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
 * **Layout Options:**
 * - `columns: 1` (default): Single column list layout
 * - `columns: 2`: Two-column grid layout, ideal for visual options with images
 *
 * @property type - Must be 'radio'
 * @property options - Array of selectable options. Can be strings or objects with label, value, and optional image properties
 * @property defaultValue - Optional default selected value (must match one of the option values)
 * @property columns - Optional number of columns for layout (1 or 2). Defaults to 1
 *
 * @example
 * ```typescript
 * // Single column layout (default)
 * const radioField: StyleEditorRadioField = {
 *   type: 'radio',
 *   label: 'Alignment',
 *   options: ['Left', 'Center', 'Right'],
 *   defaultValue: 'left'
 * };
 *
 * // Two-column grid layout with images
 * const layoutField: StyleEditorRadioField = {
 *   type: 'radio',
 *   label: 'Layout',
 *   columns: 2,
 *   options: [
 *     {
 *       label: 'Left',
 *       value: 'left',
 *       imageURL: 'https://example.com/layout-left.png',
 *       width: 80,
 *       height: 50
 *     },
 *     {
 *       label: 'Right',
 *       value: 'right',
 *       imageURL: 'https://example.com/layout-right.png',
 *       width: 80,
 *       height: 50
 *     },
 *     { label: 'Center', value: 'center' },
 *     { label: 'Overlap', value: 'overlap' }
 *   ],
 *   defaultValue: 'right'
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
    options: StyleEditorRadioOption[];
    /** Optional default selected value (must match one of the option values) */
    defaultValue?: string;
    /**
     * Number of columns to display options in.
     * - `1`: Single column list layout (default)
     * - `2`: Two-column grid layout
     */
    columns?: 1 | 2;
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
    options: StyleEditorOption[];
    /**
     * Optional default checked state as a record mapping option values to boolean.
     * Keys should match the option values, values indicate whether the option is checked.
     */
    defaultValue?: StyleEditorCheckboxDefaultValue;
}

/**
 * Union type of all possible field definitions.
 *
 * Represents any valid field type that can be used in a style editor form.
 * This is a discriminated union type, meaning TypeScript can narrow the type
 * based on the `type` property. Use this type when you need to work with
 * fields of unknown or mixed types.
 *
 * **Supported Field Types:**
 * - `StyleEditorInputField`: Text or number input fields
 * - `StyleEditorDropdownField`: Single-value selection from dropdown
 * - `StyleEditorRadioField`: Single-value selection from radio buttons
 * - `StyleEditorCheckboxGroupField`: Multiple-value selection from checkboxes
 *
 * **Note:** The `switch` field type is reserved for future implementation
 * and is not currently included in this union type.
 *
 * @example
 * ```typescript
 * const fields: StyleEditorField[] = [
 *   { type: 'input', label: 'Font Size', inputType: 'number', defaultValue: 16 },
 *   { type: 'dropdown', label: 'Font Family', options: ['Arial', 'Helvetica'] },
 *   { type: 'radio', label: 'Theme', options: ['Light', 'Dark'] },
 *   { type: 'checkboxGroup', label: 'Styles', options: ['Bold', 'Italic'] }
 * ];
 *
 * // TypeScript can narrow the type based on the discriminator
 * function processField(field: StyleEditorField) {
 *   if (field.type === 'input') {
 *     // field is now narrowed to StyleEditorInputField
 *     console.log(field.inputType); // Type-safe access
 *   }
 * }
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
 * Sections group related fields together with a title. All sections use a
 * single-column layout with a flat array of fields. During normalization,
 * these fields are automatically organized into the multi-dimensional array
 * structure required by UVE.
 *
 * @property title - The section title displayed to users
 * @property fields - Array of field definitions in this section
 *
 * @example
 * ```typescript
 * const section: StyleEditorSection = {
 *   title: 'Typography',
 *   fields: [
 *     { type: 'input', label: 'Font Size', inputType: 'number', defaultValue: 16 },
 *     { type: 'dropdown', label: 'Font Family', options: ['Arial', 'Helvetica'] },
 *     { type: 'radio', label: 'Alignment', options: ['Left', 'Center', 'Right'] }
 *   ]
 * };
 * ```
 */
export interface StyleEditorSection {
    /** The section title displayed to users */
    title: string;
    /** Array of field definitions in this section */
    fields: StyleEditorField[];
}

/**
 * Complete style editor form definition.
 *
 * Represents the full structure of a style editor form, including
 * the content type identifier and all sections with their fields.
 * This is the developer-friendly format used to define forms before
 * they are normalized and sent to UVE.
 *
 * **Form Structure:**
 * - Each form is associated with a specific content type via `contentType`
 * - Forms contain one or more sections, each with a title and array of fields
 * - All sections use a single-column layout (flat array of fields)
 * - During normalization via `defineStyleEditorForm`, sections are automatically
 *   converted to the multi-dimensional array structure required by UVE
 *
 * @property contentType - The content type identifier this form is associated with
 * @property sections - Array of sections, each containing a title and fields array
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
 *         { type: 'dropdown', label: 'Font Family', options: ['Arial', 'Helvetica'] },
 *         { type: 'radio', label: 'Alignment', options: ['Left', 'Center', 'Right'] }
 *       ]
 *     },
 *     {
 *       title: 'Colors',
 *       fields: [
 *         { type: 'input', label: 'Primary Color', inputType: 'text', defaultValue: '#000000' },
 *         { type: 'input', label: 'Secondary Color', inputType: 'text', defaultValue: '#FFFFFF' }
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
 * Configuration object for normalized field schemas.
 *
 * Contains all possible field-specific properties after normalization.
 * This interface is used by `StyleEditorFieldSchema` to define the `config` property.
 *
 * **Note:** All properties are optional since different field types use different subsets:
 * - Input fields use: `inputType`, `placeholder`, `defaultValue`
 * - Dropdown fields use: `options`, `placeholder`, `defaultValue`
 * - Radio fields use: `options`, `columns`, `defaultValue`
 * - Checkbox fields use: `options`, `defaultValue`
 */
export interface StyleEditorFieldSchemaConfig {
    /** Optional input type for input fields ('text' or 'number') */
    inputType?: StyleEditorFieldInputType;
    /** Optional placeholder text shown when the field is empty */
    placeholder?: string;
    /**
     * Optional array of normalized options for dropdown, radio, and checkbox fields.
     * In the normalized schema, options are always in object form (strings are converted).
     * Uses `StyleEditorRadioOptionObject` as the superset that supports all option properties.
     */
    options?: StyleEditorRadioOptionObject[];
    /**
     * Optional default value. Type depends on field type:
     * - Input fields: string or number
     * - Switch fields: boolean
     * - Checkbox groups: StyleEditorCheckboxDefaultValue
     */
    defaultValue?: string | number | boolean | StyleEditorCheckboxDefaultValue;
    /**
     * Number of columns to display options in (for radio fields).
     * - `1`: Single column list layout (default)
     * - `2`: Two-column grid layout
     */
    columns?: 1 | 2;
}

/**
 * Normalized field schema sent to UVE.
 *
 * This is the transformed format of field definitions after normalization.
 * All field-specific properties are moved into a `config` object, ensuring
 * a consistent structure that UVE can consume.
 *
 * **Normalization Process:**
 * - Type-specific properties (like `inputType`, `options`, `placeholder`) are moved into `config`
 * - String options are normalized to `{ label, value }` objects
 * - Radio field image properties (`imageURL`, `width`, `height`) are preserved in option objects
 * - The `type` and `label` remain at the top level for easy access
 *
 * @property type - The field type identifier (discriminator for field types)
 * @property label - The human-readable label displayed for this field
 * @property config - Object containing all field-specific configuration properties
 */
export interface StyleEditorFieldSchema {
    /** The unique identifier for this field */
    id: string;
    /** The field type identifier */
    type: StyleEditorFieldType;
    /** The field label */
    label: string;
    /** Object containing all field-specific configuration */
    config: StyleEditorFieldSchemaConfig;
}

/**
 * Normalized section schema sent to UVE.
 *
 * Represents a section in the normalized UVE format. All sections
 * use a consistent multi-dimensional array structure (array of arrays),
 * even for single-column layouts. This ensures a uniform schema format
 * that UVE can consume.
 *
 * **Structure:**
 * - The `fields` property is always a two-dimensional array
 * - Each inner array represents a column (currently all sections use a single column)
 * - Each field in the inner arrays is a normalized `StyleEditorFieldSchema` with
 *   all properties moved into the `config` object
 *
 * @property title - The section title displayed to users
 * @property fields - Two-dimensional array where each inner array contains normalized field schemas for a column
 *
 * @example
 * ```typescript
 * // Single-column section (normalized format)
 * const sectionSchema: StyleEditorSectionSchema = {
 *   title: 'Typography',
 *   fields: [
 *     // Single column containing all fields
 *     [
 *       { type: 'input', label: 'Font Size', config: { inputType: 'number', defaultValue: 16 } },
 *       { type: 'dropdown', label: 'Font Family', config: { options: [...] } }
 *     ]
 *   ]
 * };
 * ```
 */
export interface StyleEditorSectionSchema {
    /** The section title displayed to users */
    title: string;
    /** Two-dimensional array where each inner array contains normalized field schemas for a column */
    fields: StyleEditorFieldSchema[];
}

/**
 * Complete normalized form schema sent to UVE.
 *
 * This is the final output format after normalizing a `StyleEditorForm` using
 * `defineStyleEditorForm`. The form structure is transformed into a consistent
 * schema format that UVE (Universal Visual Editor) can consume.
 *
 * **Normalization Characteristics:**
 * - All sections use the multi-dimensional array structure (`fields: StyleEditorFieldSchema[][]`)
 * - All field-specific properties are moved into `config` objects
 * - String options are normalized to `{ label, value }` objects
 * - The `contentType` identifier is preserved for association with content types
 *
 * This schema format is ready to be sent to UVE via `registerStyleEditorSchemas`.
 *
 * @property contentType - The content type identifier this form is associated with
 * @property sections - Array of normalized section schemas, each with fields organized as a multi-dimensional array
 *
 * @example
 * ```typescript
 * const formSchema: StyleEditorFormSchema = {
 *   contentType: 'my-content-type',
 *   sections: [
 *     {
 *       title: 'Typography',
 *       fields: [
 *         // Single column containing all fields
 *         [
 *           { type: 'input', label: 'Font Size', config: { inputType: 'number', defaultValue: 16 } },
 *           { type: 'dropdown', label: 'Font Family', config: { options: [{ label: 'Arial', value: 'Arial' }] } }
 *         ]
 *       ]
 *     },
 *     {
 *       title: 'Colors',
 *       fields: [
 *         [
 *           { type: 'input', label: 'Primary Color', config: { inputType: 'text', defaultValue: '#000000' } }
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
