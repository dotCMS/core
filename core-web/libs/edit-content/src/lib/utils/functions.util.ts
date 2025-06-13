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
 * Supports dotCMS formats as per official documentation:
 * - Multi-line pipe format: "foo|1\r\nbar|2\r\nthird item|c"
 * - Special case: "|true" creates checkbox without label
 * - Simple comma format: "1,2,3" (when no pipes present or single-line with pipes)
 */
export const getSingleSelectableFieldOptions = (
    options: string,
    dataType: string
): { label: string; value: DotEditContentFieldSingleSelectableDataTypes }[] => {
    if (!options?.trim()) return [];

    const hasLineBreaks = /\r\n|\n|\r/.test(options);
    const hasPipes = /\|/.test(options);

    let items: string[] = [];
    let isPipeFormat = false;

    if (hasPipes && hasLineBreaks) {
        // Multi-line pipe format (standard dotCMS format)
        items = options.split(/\r\n|\n|\r/).filter((line) => line.trim());
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
            .filter((v) => v);
        isPipeFormat = false;
    }

    return items
        .map((item) => {
            let label: string;
            let value: string;

            if (isPipeFormat) {
                // Pipe format: "foo|1" -> label="foo", value="1"
                // Special case: "|true" -> label="", value="true" (checkbox without label)
                const parts = item.split('|');
                label = (parts[0] || '').trim(); // Allow empty label for "|true" case
                value = parts[1]?.trim() || parts[0]?.trim() || '';
            } else {
                // Comma format: "1" -> label="1", value="1"
                label = item;
                value = item;
            }

            // Skip only if value is empty (allow empty labels for "|true" case)
            if (!value) return null;

            const castedValue = castSingleSelectableValue(value, dataType);

            return castedValue !== null ? { label, value: castedValue } : null;
        })
        .filter(Boolean) as {
        label: string;
        value: DotEditContentFieldSingleSelectableDataTypes;
    }[];
};

// This function is used to cast the value to a correct type for the Angular Form
export const getFinalCastedValue = (
    value: object | string | undefined,
    field: DotCMSContentTypeField
) => {
    if (CALENDAR_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)) {
        const parseResult = new Date(value as string);

        // When we create a field, we can set the default value to "now" so, it will cast to Invalid Date. But an undefined value can also be casted to Invalid Date.
        // So if the getTime() method returns NaN that means the value is invalid and it's either undefined or "now". Otherwise just return the parsed date.
        return isNaN(parseResult.getTime()) ? value && new Date() : parseResult;
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
    } catch (e) {
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
