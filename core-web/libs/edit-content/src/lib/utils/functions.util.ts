import {
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutTab,
    DotCMSDataTypes,
    DotLanguage,
    UI_STORAGE_KEY,
    DotCMSClazzes,
    DotCMSDataType
} from '@dotcms/dotcms-models';
import { UVE_MODE } from '@dotcms/types';

import { NON_FORM_CONTROL_FIELD_TYPES } from '../models/dot-edit-content-field.constant';
import { DotEditContentFieldSingleSelectableDataTypes } from '../models/dot-edit-content-field.type';
import { Tab } from '../models/dot-edit-content-form.interface';
import { UIState } from '../models/dot-edit-content.model';

// This function is used to cast the value to a correct type for the Angular Form if the field is a single selectable field
export const castSingleSelectableValue = (
    value: unknown,
    type: DotCMSDataType
): DotEditContentFieldSingleSelectableDataTypes | null => {
    // Early return for null/undefined/empty values
    if (value === null || value === undefined || value === '') {
        return null;
    }

    switch (type) {
        case DotCMSDataTypes.BOOLEAN: {
            // For boolean type, handle both boolean and string values
            return typeof value === 'boolean'
                ? value
                : String(value).toLowerCase().trim() === 'true';
        }

        case DotCMSDataTypes.INTEGER:

        // fallthrough
        case DotCMSDataTypes.FLOAT: {
            const num = Number(value);

            return isNaN(num) ? null : num;
        }

        default: {
            return String(value);
        }
    }
};

// This function creates the model for the Components that use the Single Selectable Field, like the Select, Radio Button and Checkbox
export const getSingleSelectableFieldOptions = (
    options: string,
    dataType: DotCMSDataType
): { label: string; value: DotEditContentFieldSingleSelectableDataTypes }[] => {
    const lines = (options?.split('\r\n') ?? []).filter((line) => line.trim() !== '');

    return lines
        .map((line) => {
            const [label, value = label] = line.split('|').map((value) => value.trim());

            const castedValue = castSingleSelectableValue(value, dataType);
            if (castedValue === null) {
                return null;
            }

            return { label, value: castedValue };
        })
        .filter(
            (
                item
            ): item is { label: string; value: DotEditContentFieldSingleSelectableDataTypes } =>
                item !== null
        );
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
        if (clazz === DotCMSClazzes.TAB_DIVIDER) {
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
    return NON_FORM_CONTROL_FIELD_TYPES.includes(field.fieldType);
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
