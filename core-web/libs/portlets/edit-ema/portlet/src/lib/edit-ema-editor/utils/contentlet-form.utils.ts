import { DotCMSClazzes, DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';

/**
 * Type representing the supported field classes for quick edit form
 */
export type QuickEditFieldClass =
    | typeof DotCMSClazzes.TEXT
    | typeof DotCMSClazzes.TEXTAREA
    | typeof DotCMSClazzes.CHECKBOX
    | typeof DotCMSClazzes.MULTI_SELECT
    | typeof DotCMSClazzes.RADIO
    | typeof DotCMSClazzes.SELECT;

/**
 * Supported field types for the quick edit form
 */
export const QUICK_EDIT_SUPPORTED_FIELDS: QuickEditFieldClass[] = [
    DotCMSClazzes.TEXT,
    DotCMSClazzes.TEXTAREA,
    DotCMSClazzes.CHECKBOX,
    DotCMSClazzes.MULTI_SELECT,
    DotCMSClazzes.RADIO,
    DotCMSClazzes.SELECT
];

/**
 * Type representing parsed field option (label/value pair)
 */
export interface FieldOption {
    label: string;
    value: string;
}

/**
 * Type representing a content type field suitable for quick editing
 */
export type QuickEditField = Pick<
    DotCMSContentTypeField,
    'name' | 'variable' | 'regexCheck' | 'dataType' | 'readOnly' | 'required' | 'clazz' | 'values'
>;

/**
 * Parses a field values string into an array of {label, value} objects.
 * The expected format is: "label|value\nlabel|value" with each option on a new line.
 *
 * @example
 * ```typescript
 * // Input: "Option 1|opt1\nOption 2|opt2"
 * // Output: [{label: 'Option 1', value: 'opt1'}, {label: 'Option 2', value: 'opt2'}]
 *
 * parseFieldValues("Red|red\nBlue|blue");
 * // Returns: [{label: 'Red', value: 'red'}, {label: 'Blue', value: 'blue'}]
 *
 * // Handles missing values by using label as value
 * parseFieldValues("Red\nBlue");
 * // Returns: [{label: 'Red', value: 'Red'}, {label: 'Blue', value: 'Blue'}]
 * ```
 *
 * @param values - The values string from the field definition (format: "label|value\n...")
 * @returns Array of {label, value} objects, empty array if input is null/undefined
 */
export function parseFieldValues(values?: string): FieldOption[] {
    if (!values) {
        return [];
    }

    return values
        .split('\n')
        .filter((line) => line.trim())
        .map((line) => {
            const [label, value] = line.split('|').map((s) => s.trim());
            return {
                label: label || value || '',
                value: value || label || ''
            };
        });
}

/**
 * Flattens the content type layout structure and extracts fields that are supported
 * by the quick edit form (text, textarea, checkbox, select, radio, multi-select).
 *
 * @example
 * ```typescript
 * const layout = [
 *   {
 *     columns: [
 *       {
 *         fields: [
 *           { clazz: 'com.dotcms.contenttype.model.field.TextField', name: 'Title', ... },
 *           { clazz: 'com.dotcms.contenttype.model.field.ImageField', name: 'Image', ... }
 *         ]
 *       }
 *     ]
 *   }
 * ];
 *
 * // Only returns the TextField, ImageField is filtered out
 * getQuickEditFields(layout);
 * ```
 *
 * @param layout - The content type layout structure (rows → columns → fields)
 * @returns Array of fields suitable for quick editing with only necessary properties
 */
export function getQuickEditFields(layout: DotCMSContentTypeLayoutRow[]): QuickEditField[] {
    return layout
        .flatMap((row) => row.columns ?? [])
        .flatMap((column) => column.fields)
        .filter((field) => QUICK_EDIT_SUPPORTED_FIELDS.includes(field.clazz as QuickEditFieldClass))
        .map((field) => ({
            name: field.name,
            variable: field.variable,
            regexCheck: field.regexCheck,
            dataType: field.dataType,
            readOnly: field.readOnly,
            required: field.required,
            clazz: field.clazz,
            values: field.values
        }));
}

/**
 * Checks if a field class is supported by the quick edit form.
 *
 * @param clazz - The field class to check
 * @returns True if the field is supported for quick editing
 */
export function isQuickEditSupportedField(clazz: string): clazz is QuickEditFieldClass {
    return QUICK_EDIT_SUPPORTED_FIELDS.includes(clazz as QuickEditFieldClass);
}
