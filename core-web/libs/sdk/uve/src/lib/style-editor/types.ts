import type {
    StyleEditorCheckboxOption,
    StyleEditorFieldType,
    StyleEditorOption,
    StyleEditorRadioOption
} from '@dotcms/types/internal';

/**
 * Configuration type for creating input fields.
 *
 * @typeParam T - The input type ('text' or 'number')
 */
export interface StyleEditorInputFieldConfig<T extends 'text' | 'number'> {
    id: string;
    label: string;
    inputType: T;
    placeholder?: string;
}

/**
 * Helper type that extracts the union of all option values from an array of options.
 */
export type StyleEditorOptionValues<T extends readonly StyleEditorOption[]> = T[number] extends {
    value: infer V;
}
    ? V
    : never;

/**
 * Helper type that extracts the union of all radio option values from an array of radio options.
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
 * Checkbox group value type — maps option keys to boolean checked state.
 */
export type StyleEditorCheckboxDefaultValue = Record<string, boolean>;

/**
 * Base field definition shared by all field types.
 */
export interface StyleEditorBaseField {
    id: string;
    type: StyleEditorFieldType;
    label: string;
}

/**
 * Input field definition.
 */
export type StyleEditorInputField =
    | (StyleEditorBaseField & {
          type: 'input';
          inputType: 'number';
          placeholder?: string;
      })
    | (StyleEditorBaseField & {
          type: 'input';
          inputType: 'text';
          placeholder?: string;
      });

/**
 * Dropdown field definition for single-value selection.
 */
export interface StyleEditorDropdownField extends StyleEditorBaseField {
    type: 'dropdown';
    options: readonly StyleEditorOption[];
}

/**
 * Radio button field definition for single-value selection with optional visual options.
 */
export interface StyleEditorRadioField extends StyleEditorBaseField {
    type: 'radio';
    options: readonly StyleEditorRadioOption[];
    columns?: 1 | 2;
}

/**
 * Checkbox group field definition for multiple-value selection.
 */
export interface StyleEditorCheckboxGroupField extends StyleEditorBaseField {
    type: 'checkboxGroup';
    options: StyleEditorCheckboxOption[];
}

/**
 * Union type of all possible field definitions.
 */
export type StyleEditorField =
    | StyleEditorInputField
    | StyleEditorDropdownField
    | StyleEditorRadioField
    | StyleEditorCheckboxGroupField;

/**
 * Section definition for organizing fields in a style editor form.
 */
export interface StyleEditorSection {
    title: string;
    fields: StyleEditorField[];
}

/**
 * Complete style editor form definition (developer-facing input format).
 */
export interface StyleEditorForm {
    contentType: string;
    sections: StyleEditorSection[];
}
