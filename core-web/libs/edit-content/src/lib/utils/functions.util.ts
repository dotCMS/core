import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutTab,
    DotLanguage,
    UI_STORAGE_KEY
} from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { CustomFieldConfig } from '../models/dot-edit-content-custom-field.interface';
import {
    CALENDAR_FIELD_TYPES,
    CALENDAR_FIELD_TYPES_WITH_TIME,
    DEFAULT_CUSTOM_FIELD_CONFIG,
    FLATTENED_FIELD_TYPES,
    TAB_FIELD_CLAZZ,
    UNCASTED_FIELD_TYPES
} from '../models/dot-edit-content-field.constant';
import {
    DotEditContentFieldSingleSelectableDataType,
    FIELD_TYPES
} from '../models/dot-edit-content-field.enum';
import { DotEditContentFieldSingleSelectableDataTypes } from '../models/dot-edit-content-field.type';
import { NON_FORM_CONTROL_FIELD_TYPES } from '../models/dot-edit-content-form.enum';
import { Tab } from '../models/dot-edit-content-form.interface';
import { UIState } from '../models/dot-edit-content.model';

// This function is used to cast the value to a correct type for the Angular Form if the field is a single selectable field
export const castSingleSelectableValue = (
    value: unknown,
    type: string
): DotEditContentFieldSingleSelectableDataTypes | null => {
    // Early return for null/undefined/empty values
    if (value === null || value === undefined || value === '') {
        return null;
    }

    switch (type) {
        case DotEditContentFieldSingleSelectableDataType.BOOL: {
            // For boolean type, handle both boolean and string values
            return typeof value === 'boolean'
                ? value
                : String(value).toLowerCase().trim() === 'true';
        }

        case DotEditContentFieldSingleSelectableDataType.INTEGER:

        // fallthrough
        case DotEditContentFieldSingleSelectableDataType.FLOAT: {
            const num = Number(value);

            return isNaN(num) ? null : num;
        }

        default: {
            return String(value);
        }
    }
};

/**
 * Parses field options for single selectable fields (Checkbox, Radio, Select).
 *
 * The function handles the following formats:
 *
 * 1. Multi-line pipe format (standard format):
 *    ```
 *    label1|value1
 *    label2|value2
 *    ```
 *    Each line represents a separate option with label and value separated by pipe.
 *
 * 2. Special case for checkboxes:
 *    ```
 *    |true
 *    ```
 *    Creates a checkbox without label, using the value after the pipe.
 *
 * 3. Simple value format:
 *    ```
 *    value1,value2,value3
 *    ```
 *    When no pipes are present, each comma-separated value is used as both label and value.
 *
 * Note: If the input contains line breaks, it will be treated as a single option,
 * preserving the line breaks as part of the option text.
 *
 * @param options - The string containing the options to parse
 * @param dataType - The data type of the field
 * @returns Array of parsed options with label and value
 */
export const getSingleSelectableFieldOptions = (
    options: string,
    dataType: string
): { label: string; value: DotEditContentFieldSingleSelectableDataTypes }[] => {
    if (!options?.trim()) return [];

    const LINE_BREAKS_REGEX = /\r\n|\n|\r/;
    const PIPE_REGEX = /\|/;
    const hasLineBreaks = LINE_BREAKS_REGEX.test(options);
    const hasPipes = PIPE_REGEX.test(options);

    let items: string[] = [];
    let isPipeFormat = false;

    if (hasPipes && hasLineBreaks) {
        // Multi-line pipe format (standard dotCMS format)
        items = options.split(LINE_BREAKS_REGEX).filter((line) => line.trim());
        isPipeFormat = true;
    } else if (hasPipes && !hasLineBreaks && options.trim().startsWith('|')) {
        // Special case: "|true" (checkbox without label)
        items = [options.trim()];
        isPipeFormat = true;
    } else {
        // Simple comma format or single-line with pipes treated as comma format
        items = options
            .split(',')
            .map((v) => v.trim())
            .filter(Boolean);
    }

    // Handle nested line breaks in single items
    if (items.length === 1 && LINE_BREAKS_REGEX.test(items[0])) {
        items = items[0].split(LINE_BREAKS_REGEX).filter((line) => line.trim());
    }

    return items
        .map((item) => {
            let label: string;
            let value: string;

            if (isPipeFormat) {
                const parts = item.split('|');
                // Si hay pipe, el label es la primera parte y el value es la segunda
                // Si no hay segunda parte, el value es igual al label
                label = (parts[0] || '').trim();
                value = parts[1]?.trim() || label;
            } else {
                // Si no hay pipe, tanto label como value son el mismo valor
                label = item;
                value = item;
            }

            if (!value) return null;

            const castedValue = castSingleSelectableValue(value, dataType);

            return castedValue !== null ? { label, value: castedValue } : null;
        })
        .filter(
            (
                item
            ): item is { label: string; value: DotEditContentFieldSingleSelectableDataTypes } =>
                item !== null
        );
};

/**
 * This function is used to cast the value to a correct type for the Angular Form
 *
 * @param value
 * @param field
 * @returns
 */
export const getFinalCastedValue = (
    value: object | string | number | undefined,
    field: DotCMSContentTypeField
) => {
    if (CALENDAR_FIELD_TYPES_WITH_TIME.includes(field.fieldType as FIELD_TYPES)) {
        return value;
    }

    if (FLATTENED_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)) {
        return (value as string)?.split(',').map((value) => value.trim());
    }

    if (value === undefined || UNCASTED_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)) {
        return value;
    }

    if (field.fieldType === FIELD_TYPES.JSON) {
        return JSON.stringify(value, null, 2); // This is a workaround to avoid the Monaco Editor to show the value as a string and keep the formatting
    }

    return castSingleSelectableValue(value, field.dataType);
};

export const transformLayoutToTabs = (
    firstTabTitle: string,
    layout: DotCMSContentTypeLayoutRow[]
): DotCMSContentTypeLayoutTab[] => {
    // Reduce the layout into tabs
    const tabs = layout.reduce((acc, row) => {
        const { clazz, name } = row.divider || {};
        const lastTabIndex = acc.length - 1;

        // If the class indicates a tab field, create a new tab
        if (clazz === TAB_FIELD_CLAZZ) {
            acc.push({
                title: name,
                layout: []
            });
        } else if (lastTabIndex >= 0) {
            // Otherwise, add the row to the layout of the last tab
            acc[lastTabIndex].layout.push(row);
        } else {
            // If there's no tab yet, create the initial tab
            acc.push({
                title: firstTabTitle,
                layout: [row]
            });
        }

        return acc;
    }, [] as DotCMSContentTypeLayoutTab[]);

    return tabs;
};

/**
 * Checks if a given value is a valid JSON string
 *
 * @param {string} value - The value to be checked
 * @returns {boolean} - True if the value is a valid JSON string, false otherwise
 */
export const isValidJson = (value: string): boolean => {
    try {
        const json = JSON.parse(value);

        return json !== null && typeof json === 'object' && !Array.isArray(json);
    } catch {
        console.warn(`${value} is not a valid JSON`);

        return false;
    }
};

/**
 * Parses an array of `DotCMSContentTypeFieldVariable` objects and returns a new object
 * with key-value pairs.
 *
 * @template T - The type of the resulting object.
 * @param {DotCMSContentTypeFieldVariable[]} fieldVariables - The array of field variables to be parsed.
 * @return {T} - The parsed object with key-value pairs.
 */
export const getFieldVariablesParsed = <T extends Record<string, string | boolean>>(
    fieldVariables: DotCMSContentTypeFieldVariable[]
): T => {
    if (!fieldVariables) {
        return {} as T;
    }

    const result = {};
    fieldVariables.forEach(({ key, value }) => {
        // If the value is a boolean string, convert it to a boolean
        if (value === 'true' || value === 'false') {
            (result as Record<string, string | boolean>)[key] = value === 'true';

            return;
        }

        (result as Record<string, string | boolean>)[key] = value;
    });

    return result as T;
};

/**
 * Converts a JSON string into a JavaScript object.
 *
 * @param {string} value - The JSON string to be converted.
 * @return {Object} - The converted JavaScript object. If the input string is
 *       not valid JSON, an empty object will be returned.
 */
export const stringToJson = (value: string) => {
    if (!value) {
        return {};
    }

    return isValidJson(value) ? JSON.parse(value) : {};
};

/**
 * Converts a JSON string into a JavaScript object.
 * Create all paths based in a Path
 *
 * @param {string} path - the path
 * @return {string[]} - An arrray with all posibles pats
 *
 * @usageNotes
 *
 * ### Example
 *
 * ```ts
 * const path = 'demo.com/level1/level2';
 * const paths = createPaths(path);
 * console.log(paths); // ['demo.com/', 'demo.com/level1/', 'demo.com/level1/level2/']
 * ```
 */
export const createPaths = (path: string): string[] => {
    const split = path.split('/').filter((item) => item !== '');

    return split.reduce((array, item, index) => {
        const prev = array[index - 1];
        let path = `${item}/`;
        if (prev) {
            path = `${prev}${path}`;
        }

        array.push(path);

        return array;
    }, [] as string[]);
};

/**
 * Checks if a given content type field is of a filtered type.
 *
 * This function determines whether the provided DotCMSContentTypeField's fieldType
 * is included in the FILTERED_TYPES enum. It's used to identify fields that require
 * special handling or filtering in the content management system.
 *
 * @param {DotCMSContentTypeField} field - The content type field to check.
 * @returns {boolean} True if the field's type is in FILTERED_TYPES, false otherwise.
 */

export const isFilteredType = (field: DotCMSContentTypeField): boolean => {
    return Object.values(NON_FORM_CONTROL_FIELD_TYPES).includes(
        field.fieldType as NON_FORM_CONTROL_FIELD_TYPES
    );
};

/**
 * Transforms the form data by filtering out specific field types and organizing the content into tabs.
 *
 * @param formData - The original form data to be transformed.
 * @returns The transformed form data with filtered fields and organized tabs.
 */
export const transformFormDataFn = (contentType: DotCMSContentType): Tab[] => {
    if (!contentType) {
        return [];
    }

    const tabs = transformLayoutToTabs('Content', contentType.layout);

    return tabs.map((tab) => ({
        ...tab,
        layout: tab.layout.map((row) => ({
            ...row,
            columns: row.columns.map((column) => ({
                ...column,
                fields: column.fields.filter((field) => !isFilteredType(field))
            }))
        }))
    }));
};

/**
 * Sorts an array of locales, placing translated locales first.
 *
 * @param {DotLanguage[]} locales - The array of locales to be sorted.
 * @returns {DotLanguage[]} The sorted array with translated locales first, followed by untranslated locales.
 */
export const sortLocalesTranslatedFirst = (locales: DotLanguage[]): DotLanguage[] => {
    const translatedLocales = locales.filter((locale) => locale.translated);
    const untranslatedLocales = locales.filter((locale) => !locale.translated);

    return [...translatedLocales, ...untranslatedLocales];
};

/* Generates a preview URL for a given contentlet.
 *
 * @param {DotCMSContentlet} contentlet - The contentlet object containing the necessary data.
 * @returns {string} The generated preview URL.
 */
export const generatePreviewUrl = (contentlet: DotCMSContentlet): string => {
    if (
        !contentlet.URL_MAP_FOR_CONTENT ||
        !contentlet.host ||
        contentlet.languageId === undefined
    ) {
        console.warn('Missing required contentlet attributes to generate preview URL');

        return '';
    }

    const baseUrl = `${window.location.origin}/dotAdmin/#/edit-page/content`;
    const params = new URLSearchParams();

    params.set('url', `${contentlet.URL_MAP_FOR_CONTENT}?host_id=${contentlet.host}`);
    params.set('language_id', contentlet.languageId.toString());
    params.set('com.dotmarketing.persona.id', 'modes.persona.no.persona');
    params.set('mode', UVE_MODE.EDIT);

    return `${baseUrl}?${params.toString()}`;
};

/**
 * Gets the UI state from sessionStorage or returns the initial state if not found
 */
export const getStoredUIState = (): UIState => {
    try {
        const storedState = sessionStorage.getItem(UI_STORAGE_KEY);
        if (storedState) {
            return JSON.parse(storedState);
        }
    } catch (e) {
        console.warn('Error reading UI state from sessionStorage:', e);
    }

    // Default values
    return {
        activeTab: 0,
        isSidebarOpen: true,
        activeSidebarTab: 0,
        isBetaMessageVisible: true
    };
};

/**
 * Saves the UI state to sessionStorage
 */
export const saveStoreUIState = (state: UIState): void => {
    try {
        sessionStorage.setItem(UI_STORAGE_KEY, JSON.stringify(state));
    } catch (e) {
        console.warn('Error saving UI state to sessionStorage:', e);
    }
};

/**
 * Prepares a contentlet for copying by ensuring it's not locked and removing any previous lock owner.
 *
 * @param contentlet - The original contentlet to be copied
 * @returns The contentlet with locked=false and no lockedBy property
 */
export const prepareContentletForCopy = (contentlet: DotCMSContentlet): DotCMSContentlet => ({
    ...contentlet,
    locked: false,
    lockedBy: undefined
});

/**
 * Extracts and parses custom field options from field variables.
 * Looks for 'customFieldOptions' key and parses its JSON value.
 *
 * @param fieldVariables - Array of field variables
 * @returns Parsed custom field options object
 *
 * @example
 * ```ts
 * const options = getCustomFieldOptions(field.fieldVariables);
 * console.log(options.showAsModal); // true
 * ```
 */
export const getCustomFieldOptions = (
    fieldVariables: DotCMSContentTypeFieldVariable[]
): Partial<CustomFieldConfig> => {
    const parsedVars = getFieldVariablesParsed<Record<string, string | boolean>>(fieldVariables);
    const { customFieldOptions } = parsedVars;

    return stringToJson(customFieldOptions as string);
};

/**
 * Creates a complete custom field configuration by merging custom options with defaults
 * and applying individual field variable overrides.
 *
 * @param fieldVariables - Array of field variables
 * @returns Complete custom field configuration with defaults applied
 *
 * @example
 * ```ts
 * const config = createCustomFieldConfig(field.fieldVariables);
 * console.log(config.width); // "90vw" or default "500px"
 * ```
 */
export const createCustomFieldConfig = (
    fieldVariables: DotCMSContentTypeFieldVariable[]
): CustomFieldConfig => {
    const customOptions = getCustomFieldOptions(fieldVariables);

    const individualVars =
        getFieldVariablesParsed<Record<string, string | boolean>>(fieldVariables);

    // Use the default configuration from constants
    const defaults: CustomFieldConfig = { ...DEFAULT_CUSTOM_FIELD_CONFIG };

    // Merge with custom options from JSON
    const mergedConfig: CustomFieldConfig = {
        ...defaults,
        ...customOptions
    };

    // Override with individual field variables (highest priority)
    if (individualVars.showAsModal !== undefined) {
        mergedConfig.showAsModal = individualVars.showAsModal as boolean;
    }

    if (individualVars.width) {
        mergedConfig.width = individualVars.width as string;
    }

    if (individualVars.height) {
        mergedConfig.height = individualVars.height as string;
    }

    return mergedConfig;
};

/**
 * Checks if a field should be flattened (array values joined with commas).
 * Used for multi-select fields that need to be converted to comma-separated strings.
 *
 * @param fieldValue - The field value to check
 * @param field - The field configuration
 * @returns True if the field should be flattened
 */
export const isFlattenedField = (
    fieldValue: string | string[] | Date | number | null | undefined,
    field: DotCMSContentTypeField
): fieldValue is string[] => {
    return (
        Array.isArray(fieldValue) && FLATTENED_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)
    );
};

/**
 * Checks if a field is a calendar field (date, datetime, time).
 * Used to determine if special timestamp processing is needed.
 *
 * @param field - The field configuration
 * @returns True if the field is a calendar field
 */
export const isCalendarField = (field: DotCMSContentTypeField): boolean => {
    return CALENDAR_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES);
};

/**
 * Processes calendar field values to ensure they are always numeric timestamps.
 * Handles conversion from Date objects, strings, and validates numeric values.
 *
 * @param fieldValue - The calendar field value (Date, number, string, or null/undefined)
 * @param fieldName - The field name (for logging purposes)
 * @returns Numeric timestamp or null/undefined
 */
export const processCalendarFieldValue = (
    fieldValue: string | string[] | Date | number | null | undefined,
    fieldName: string
): number | null | undefined => {
    // Handle null/undefined values
    if (fieldValue === null || fieldValue === undefined) {
        return fieldValue as null | undefined;
    }

    // Handle empty strings
    if (fieldValue === '') {
        return null;
    }

    // Convert Date objects to timestamps (normal case from calendar component)
    if (fieldValue instanceof Date) {
        return fieldValue.getTime();
    }

    // Keep numeric values as-is (already correct timestamps)
    if (typeof fieldValue === 'number') {
        return fieldValue;
    }

    // Convert string timestamps to numbers (edge case - from form state)
    if (typeof fieldValue === 'string') {
        const trimmedValue = fieldValue.trim();

        // Handle empty string after trim
        if (trimmedValue === '') {
            return null;
        }

        const numericValue = Number(trimmedValue);

        if (isNaN(numericValue)) {
            console.warn(`Calendar field ${fieldName} has invalid timestamp string:`, fieldValue);
            return null;
        }

        console.warn(
            `Calendar field ${fieldName} received string timestamp, converted to number:`,
            {
                original: fieldValue,
                converted: numericValue
            }
        );

        return numericValue;
    }

    // Handle unexpected cases (arrays, objects, etc.)
    console.error(`Calendar field ${fieldName} received unexpected value:`, {
        value: fieldValue,
        type: typeof fieldValue
    });

    return null;
};

/**
 * Processes a single field value based on its field type.
 * Applies appropriate transformations for different field types:
 * - Flattened fields: Joins arrays with commas
 * - Calendar fields: Converts to numeric timestamps
 * - Other fields: Returns as-is
 *
 * @param fieldValue - The raw field value
 * @param field - The field configuration
 * @returns The processed field value
 */
export const processFieldValue = (
    fieldValue: string | string[] | Date | number | null | undefined,
    field: DotCMSContentTypeField
): string | number | null | undefined => {
    // Handle flattened fields (multi-select, etc.)
    if (isFlattenedField(fieldValue, field)) {
        return (fieldValue as string[]).join(',');
    }

    // Handle calendar fields (date, datetime, time)
    if (isCalendarField(field)) {
        return processCalendarFieldValue(fieldValue, field.variable);
    }

    // For all other fields, return as-is
    return fieldValue as string | number | null | undefined;
};
