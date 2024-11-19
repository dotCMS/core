import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutTab,
    DotCMSWorkflow,
    DotCMSWorkflowAction,
    WorkflowStep
} from '@dotcms/dotcms-models';

import { ContentState } from '../feature/edit-content/store/features/content.feature';
import { WorkflowState } from '../feature/edit-content/store/features/workflow.feature';
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
import { NON_FORM_CONTROL_FIELD_TYPES } from '../models/dot-edit-content-form.enum';
import { Tab } from '../models/dot-edit-content-form.interface';
import { SIDEBAR_LOCAL_STORAGE_KEY } from '../models/dot-edit-content.constant';

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
 * Retrieves the sidebar state from the local storage.
 *
 * This function accesses the local storage using a predefined key `SIDEBAR_LOCAL_STORAGE_KEY`
 * and returns the parsed state of the sidebar. If the value in local storage is 'true',
 * it returns `true`; otherwise, it returns `false`. If there is no value stored under
 * the key, it defaults to returning `true`.
 *
 * @returns {boolean} The state of the sidebar, either `true` (opened) or `false` (closed).
 */
export const getPersistSidebarState = (): boolean => {
    const localStorageData = localStorage.getItem(SIDEBAR_LOCAL_STORAGE_KEY);

    return localStorageData ? localStorageData === 'true' : true;
};

/**
 * Function to persist the state of the sidebar in local storage.
 *
 * @param {string} value - The state of the sidebar to persist.
 *                         Typically a string representing whether the sidebar is open or closed.
 */
export const setPersistSidebarState = (value: string) => {
    localStorage.setItem(SIDEBAR_LOCAL_STORAGE_KEY, value);
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
 * Parses an array of workflow data and returns a new object with key-value pairs.
 *
 * @param {Object[]} data - The array of workflow data to be parsed.
 * @returns {Object} - The parsed object with key-value pairs.
 */
export const parseWorkflows = (
    data: {
        scheme: DotCMSWorkflow;
        action: DotCMSWorkflowAction;
        firstStep: WorkflowStep;
    }[]
) => {
    if (!Array.isArray(data)) {
        return {};
    }

    return data.reduce((acc, { scheme, action, firstStep }) => {
        if (!acc[scheme.id]) {
            acc[scheme.id] = {
                scheme: {
                    ...scheme
                },
                actions: [],
                firstStep
            };
        }

        acc[scheme.id].actions.push(action);

        return acc;
    }, {});
};

/**
 * Determines if workflow action buttons should be shown based on content and scheme state
 * Shows workflow buttons when:
 * - Content type has only one workflow scheme OR
 * - Content is existing AND has a selected workflow scheme OR
 * - Content is new and has selected a workflow scheme
 *
 * @param schemes - Available workflow schemes object
 * @param contentlet - Current contentlet (if exists)
 * @param currentSchemeId - Selected workflow scheme ID
 * @returns boolean indicating if workflow actions should be shown
 */
export function shouldShowWorkflowActions(
    schemes: WorkflowState['schemes'],
    contentlet: any | null,
    currentSchemeId: string | null
): boolean {
    const hasOneScheme = Object.keys(schemes).length === 1;
    const isExisting = !!contentlet;
    const hasSelectedScheme = !!currentSchemeId;

    if (hasOneScheme) {
        return true;
    }

    if (isExisting && hasSelectedScheme) {
        return true;
    }

    if (!isExisting && hasSelectedScheme) {
        return true;
    }

    return false;
}

/**
 * Determines if workflow selection warning should be shown
 * Shows warning when:
 * - Content is new (no contentlet exists) AND
 * - Content type has multiple workflow schemes AND
 * - No workflow scheme has been selected
 *
 * @param schemes - Available workflow schemes object
 * @param contentlet - Current contentlet (if exists)
 * @param currentSchemeId - Selected workflow scheme ID
 * @returns boolean indicating if workflow selection warning should be shown
 */
export function shouldShowWorkflowWarning(
    schemes: WorkflowState['schemes'],
    contentlet: ContentState['contentlet'],
    currentSchemeId: string | null
): boolean {
    const isNew = !contentlet;
    const hasNoSchemeSelected = !currentSchemeId;
    const hasMultipleSchemas = Object.keys(schemes).length > 1;

    return isNew && hasMultipleSchemas && hasNoSchemeSelected;
}

/**
 * Gets the appropriate workflow actions based on content state
 * Returns:
 * - Empty array if no scheme is selected
 * - Current content actions for existing content
 * - Sorted scheme actions for new content (with 'Save' action first)
 *
 * @param schemes - Available workflow schemes object
 * @param contentlet - Current contentlet (if exists)
 * @param currentSchemeId - Selected workflow scheme ID
 * @param currentContentActions - Current content specific actions
 * @returns Array of workflow actions
 */
export function getWorkflowActions(
    schemes: WorkflowState['schemes'],
    contentlet: ContentState['contentlet'],
    currentSchemeId: string | null,
    currentContentActions: DotCMSWorkflowAction[]
): DotCMSWorkflowAction[] {
    const isNew = !contentlet;

    // If no scheme is selected, return empty array
    if (!currentSchemeId || !schemes[currentSchemeId]) {
        return [];
    }

    // For existing content, use specific contentlet actions
    if (!isNew && currentContentActions.length) {
        return currentContentActions;
    }

    // For new content, use scheme actions sorted with 'Save' first
    return Object.values(schemes[currentSchemeId].actions).sort((a, b) => {
        if (a.name === 'Save') return -1;
        if (b.name === 'Save') return 1;
        return a.name.localeCompare(b.name);
    });
}
