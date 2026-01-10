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
 */
export type StyleEditorFieldInputType = 'text' | 'number';

/**
 * Configuration type for creating input fields.
 *
 * This type is used by `styleEditorField.input()` to provide compile-time
 * type checking for input field definitions.
 *
 * @typeParam T - The input type ('text' or 'number')
 */
export interface StyleEditorInputFieldConfig<T extends StyleEditorFieldInputType> {
    /** The unique identifier for this field */
    id: string;
    /** The label text displayed to users for this field */
    label: string;
    /** The input type ('text' for strings, 'number' for numbers) */
    inputType: T;
    /** Optional placeholder text shown when the input is empty */
    placeholder?: string;
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
 */
export interface StyleEditorRadioOptionObject extends StyleEditorOptionObject {
    /** Optional URL to an image displayed for this option */
    imageURL?: string;
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
export type StyleEditorOption = StyleEditorOptionObject;

/**
 * Helper type that extracts the union of all option values from an array of options.
 *
 * This type extracts the `value` property from each option object and creates
 * a union type of all possible values.
 *
 * **Note:** For full type safety and autocomplete, use `as const` when defining options:
 * ```typescript
 * const options = [
 *   { label: 'The one', value: 'one' },
 *   { label: 'The two', value: 'two' }
 * ] as const;
 * ```
 *
 * @typeParam T - Array of option objects (should be `as const` for best results)
 *
 * @example
 * ```typescript
 * const options = [
 *   { label: 'The one', value: 'one' },
 *   { label: 'The two', value: 'two' }
 * ] as const;
 *
 * type OptionValues = StyleEditorOptionValues<typeof options>;
 * // Result: 'one' | 'two'
 * ```
 */
export type StyleEditorOptionValues<T extends readonly StyleEditorOption[]> = T[number] extends {
    value: infer V;
}
    ? V
    : never;

/**
 * Helper type that extracts the union of all radio option values from an array of radio options.
 *
 * Similar to `StyleEditorOptionValues`, but handles radio options which can be
 * strings or objects. Extracts the `value` property from option objects, or uses
 * the string itself if the option is a string.
 *
 * **Note:** For full type safety and autocomplete, use `as const` when defining options:
 * ```typescript
 * const options = [
 *   { label: 'The one', value: 'one' },
 *   { label: 'The two', value: 'two' }
 * ] as const;
 * ```
 *
 * @typeParam T - Array of radio option objects or strings (should be `as const` for best results)
 *
 * @example
 * ```typescript
 * const options = [
 *   { label: 'The one', value: 'one' },
 *   { label: 'The two', value: 'two' }
 * ] as const;
 *
 * type RadioOptionValues = StyleEditorRadioOptionValues<typeof options>;
 * // Result: 'one' | 'two'
 * ```
 */
export type StyleEditorRadioOptionValues<T extends readonly StyleEditorRadioOption[]> =
    T[number] extends infer U
        ? U extends string
            ? U
            : U extends { value: infer V }
              ? V
              : never
        : never;

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
 *   imageURL: 'https://example.com/left-layout.png'
 * };
 * ```
 */
export type StyleEditorRadioOption = string | StyleEditorRadioOptionObject;

/**
 * Checkbox option object with label and key identifier.
 *
 * Unlike dropdown and radio options where `value` represents the actual value,
 * checkbox options use `key` as the identifier. The checked state is managed
 * by the form system and is not stored in the option definition.
 *
 * @property label - Display label shown to users
 * @property key - Unique identifier/key used for this checkbox option
 *
 * @example
 * ```typescript
 * const checkboxOption: StyleEditorCheckboxOption = {
 *   label: 'Underline',
 *   key: 'underline'
 * };
 * ```
 */
export interface StyleEditorCheckboxOption {
    /** Display label shown to users */
    label: string;
    /** Unique identifier/key used for this checkbox option */
    key: string;
}

/**
 * Checkbox group value type.
 *
 * A record mapping option keys to their boolean checked state.
 * Keys match the `key` property from checkbox options, values indicate whether the option is checked.
 *
 * @example
 * ```typescript
 * const checkboxState: StyleEditorCheckboxDefaultValue = {
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
 * Input field definition.
 *
 * Supports both text and number input types for different value types.
 *
 * @example
 * ```typescript
 * // Number input
 * const numberField: StyleEditorInputField = {
 *   type: 'input',
 *   id: 'font-size',
 *   label: 'Font Size',
 *   inputType: 'number',
 *   placeholder: 'Enter font size'
 * };
 *
 * // Text input
 * const textField: StyleEditorInputField = {
 *   type: 'input',
 *   id: 'font-name',
 *   label: 'Font Name',
 *   inputType: 'text',
 *   placeholder: 'Enter font name'
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
      })
    | (StyleEditorBaseField & {
          /** Discriminator: must be 'input' */
          type: 'input';
          /** Input type for text/string values */
          inputType: 'text';
          /** Optional placeholder text shown when the input is empty */
          placeholder?: string;
      });

/**
 * Dropdown field definition for single-value selection.
 *
 * Allows users to select a single option from a dropdown list.
 * Options can be provided as simple strings or as objects with
 * separate label and value properties for more flexibility.
 *
 * **Best Practice:** Use `as const` when defining options for better type safety:
 * ```typescript
 * const OPTIONS = [
 *   { label: '18', value: '18px' },
 *   { label: '24', value: '24px' }
 * ] as const;
 *
 * styleEditorField.dropdown({
 *   id: 'size',
 *   label: 'Size',
 *   options: OPTIONS
 * });
 * ```
 *
 * @property type - Must be 'dropdown'
 * @property options - Array of selectable options. Can be strings or objects with label/value. Use `as const` for best type safety.
 *
 * @example
 * ```typescript
 * const dropdownField: StyleEditorDropdownField = {
 *   type: 'dropdown',
 *   id: 'font-family',
 *   label: 'Font Family',
 *   options: ['Arial', 'Helvetica', { label: 'Times New Roman', value: 'times' }]
 * };
 * ```
 */
export interface StyleEditorDropdownField extends StyleEditorBaseField {
    /** Discriminator: must be 'dropdown' */
    type: 'dropdown';
    /** Array of selectable options. Can be strings or objects with label and value properties. Accepts readonly arrays (use `as const` for best type safety). */
    options: readonly StyleEditorOption[];
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
 * **Best Practice:** Use `as const` when defining options for better type safety:
 * ```typescript
 * const RADIO_OPTIONS = [
 *   { label: 'Left', value: 'left' },
 *   { label: 'Right', value: 'right' }
 * ] as const;
 *
 * styleEditorField.radio({
 *   id: 'layout',
 *   label: 'Layout',
 *   options: RADIO_OPTIONS
 * });
 * ```
 *
 * @property type - Must be 'radio'
 * @property options - Array of selectable options. Can be strings or objects with label, value, and optional image properties. Use `as const` for best type safety.
 * @property columns - Optional number of columns for layout (1 or 2). Defaults to 1
 *
 * @example
 * ```typescript
 * // Single column layout (default)
 * const alignmentField: StyleEditorRadioField = {
 *   type: 'radio',
 *   id: 'alignment',
 *   label: 'Alignment',
 *   options: ['Left', 'Center', 'Right']
 * };
 *
 * // Two-column grid layout with images
 * const layoutField: StyleEditorRadioField = {
 *   type: 'radio',
 *   id: 'layout',
 *   label: 'Layout',
 *   columns: 2,
 *   options: [
 *     {
 *       label: 'Left',
 *       value: 'left',
 *       imageURL: 'https://example.com/layout-left.png'
 *     },
 *     {
 *       label: 'Right',
 *       value: 'right',
 *       imageURL: 'https://example.com/layout-right.png'
 *     },
 *     { label: 'Center', value: 'center' },
 *     { label: 'Overlap', value: 'overlap' }
 *   ]
 * };
 * ```
 */
export interface StyleEditorRadioField extends StyleEditorBaseField {
    /** Discriminator: must be 'radio' */
    type: 'radio';
    /**
     * Array of selectable options. Can be:
     * - Simple strings (used as both label and value)
     * - Objects with label, value, and optional imageURL for visual options
     * Accepts readonly arrays (use `as const` for best type safety).
     */
    options: readonly StyleEditorRadioOption[];
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
 * can be independently checked or unchecked. The checked state is managed
 * by the form system and is not stored in the option definition.
 *
 * **Key Differences from Other Field Types:**
 * - Uses `key` instead of `value` for the identifier (to avoid confusion)
 * - Checked state is managed by the form system, not stored in the option definition
 *
 * @property type - Must be 'checkboxGroup'
 * @property options - Array of checkbox options with label and key
 *
 * @example
 * ```typescript
 * const checkboxField: StyleEditorCheckboxGroupField = {
 *   type: 'checkboxGroup',
 *   id: 'text-decoration',
 *   label: 'Text Decoration',
 *   options: [
 *     { label: 'Underline', key: 'underline' },
 *     { label: 'Overline', key: 'overline' },
 *     { label: 'Line Through', key: 'line-through' }
 *   ]
 * };
 * ```
 */
export interface StyleEditorCheckboxGroupField extends StyleEditorBaseField {
    /** Discriminator: must be 'checkboxGroup' */
    type: 'checkboxGroup';
    /**
     * Array of checkbox options. Each option contains:
     * - `label`: Display text shown to users
     * - `key`: Unique identifier for this checkbox option
     */
    options: StyleEditorCheckboxOption[];
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
 *   { type: 'input', id: 'font-size', label: 'Font Size', inputType: 'number' },
 *   { type: 'dropdown', id: 'font-family', label: 'Font Family', options: ['Arial', 'Helvetica'] },
 *   { type: 'radio', id: 'theme', label: 'Theme', options: ['Light', 'Dark'] },
 *   {
 *     type: 'checkboxGroup',
 *     id: 'styles',
 *     label: 'Styles',
 *     options: [
 *       { label: 'Bold', key: 'bold', value: true },
 *       { label: 'Italic', key: 'italic', value: false }
 *     ]
 *   }
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
 *     { type: 'input', id: 'font-size', label: 'Font Size', inputType: 'number' },
 *     { type: 'dropdown', id: 'font-family', label: 'Font Family', options: ['Arial', 'Helvetica'] },
 *     { type: 'radio', id: 'alignment', label: 'Alignment', options: ['Left', 'Center', 'Right'] }
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
 *         { type: 'input', id: 'font-size', label: 'Font Size', inputType: 'number' },
 *         { type: 'dropdown', id: 'font-family', label: 'Font Family', options: ['Arial', 'Helvetica'] },
 *         { type: 'radio', id: 'alignment', label: 'Alignment', options: ['Left', 'Center', 'Right'] }
 *       ]
 *     },
 *     {
 *       title: 'Colors',
 *       fields: [
 *         { type: 'input', id: 'primary-color', label: 'Primary Color', inputType: 'text' },
 *         { type: 'input', id: 'secondary-color', label: 'Secondary Color', inputType: 'text' }
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
 * - Input fields use: `inputType`, `placeholder`
 * - Dropdown fields use: `options`
 * - Radio fields use: `options`, `columns`
 * - Checkbox fields use: `options`
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
 * - Radio field image properties (`imageURL`) are preserved in option objects
 * - The `type`, `id`, and `label` remain at the top level for easy access
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
 *       { type: 'input', id: 'font-size', label: 'Font Size', config: { inputType: 'number' } },
 *       { type: 'dropdown', id: 'font-family', label: 'Font Family', config: { options: [...] } }
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
 *           { type: 'input', id: 'font-size', label: 'Font Size', config: { inputType: 'number' } },
 *           { type: 'dropdown', id: 'font-family', label: 'Font Family', config: { options: [{ label: 'Arial', value: 'Arial' }] } }
 *         ]
 *       ]
 *     },
 *     {
 *       title: 'Colors',
 *       fields: [
 *         [
 *           { type: 'input', id: 'primary-color', label: 'Primary Color', config: { inputType: 'text' } }
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
