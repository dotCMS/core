import {
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutTab
} from '@dotcms/dotcms-models';

import {
    CALENDAR_FIELD_TYPES,
    FLATTENED_FIELD_TYPES,
    TAB_FIELD_CLAZZ,
    UNCASTED_FIELD_TYPES
} from '../models/dot-edit-content-field.constant';
import {
    DotEditContentFieldSingleSelectableDataType,
    FIELD_TYPES
} from '../models/dot-edit-content-field.enum';
import { DotEditContentFieldSingleSelectableDataTypes } from '../models/dot-edit-content-field.type';

// This function is used to cast the value to a correct type for the Angular Form if the field is a single selectable field
export const castSingleSelectableValue = (
    value: string,
    type: string
): DotEditContentFieldSingleSelectableDataTypes | null => {
    if (!value) {
        return null;
    }

    if (type === DotEditContentFieldSingleSelectableDataType.BOOL) {
        return value.toLowerCase().trim() === 'true';
    }

    if (
        type === DotEditContentFieldSingleSelectableDataType.INTEGER ||
        type === DotEditContentFieldSingleSelectableDataType.FLOAT
    ) {
        return Number(value);
    }

    return value;
};

// This function creates the model for the Components that use the Single Selectable Field, like the Select, Radio Button and Checkbox
export const getSingleSelectableFieldOptions = (
    options: string,
    dataType: string
): { label: string; value: DotEditContentFieldSingleSelectableDataTypes }[] => {
    const lines = (options?.split('\r\n') ?? []).filter((line) => line.trim() !== '');

    return lines?.map((line) => {
        const [label, value = label] = line.split('|').map((value) => value.trim());

        return { label, value: castSingleSelectableValue(value, dataType) };
    });
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

    return castSingleSelectableValue(value as string, field.dataType);
};

export const transformLayoutToTabs = (
    firstTabTitle: string,
    layout: DotCMSContentTypeLayoutRow[]
): DotCMSContentTypeLayoutTab[] => {
    const initialTab = [
        {
            title: firstTabTitle,
            layout: []
        }
    ];

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
        } else {
            // Otherwise, add the row to the layout of the last tab
            acc[lastTabIndex].layout.push(row);
        }

        return acc;
    }, initialTab);

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
            result[key] = value === 'true';

            return;
        }

        result[key] = value;
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
    }, []);
};
