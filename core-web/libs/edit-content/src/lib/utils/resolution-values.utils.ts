import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSFieldTypes,
    ContentTypeJSONField,
    ContentTypeHostFolderField,
    ContentTypeCategoryField,
    ContentTypeRelationshipField,
    ContentTypeDateField,
    ContentTypeDateTimeField,
    ContentTypeTimeField,
    ContentTypeSelectField,
    ContentTypeRadioField,
    DotCMSDataTypes
} from '@dotcms/dotcms-models';
import { getRelationshipFromContentlet } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/utils';

/**
 * Function type definition for resolving and casting field values from contentlets.
 *
 * @param contentlet - The contentlet object containing field values
 * @param field - The content type field definition
 * @returns The resolved and properly cast value for the field
 */
export type FnResolutionValue = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField
) => unknown;

const getValueResolutionFn = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField
): unknown => {
    const value = contentlet?.[field.variable] ?? field.defaultValue;

    if (value === null || value === undefined) {
        return null;
    }

    return value;
};

const jsonResolutionFn = (contentlet: DotCMSContentlet, field: ContentTypeJSONField) => {
    const value = getValueResolutionFn(contentlet, field);

    if (value && typeof value === 'object') {
        return JSON.stringify(value, null, 2);
    }

    return value;
};

/**
 * Resolves the host-folder path for a contentlet.
 *
 * @param contentlet - The contentlet object
 * @param field - The field object
 * @returns The resolved host-folder path or default value
 */
const hostFolderResolutionFn = (
    contentlet: DotCMSContentlet,
    field: ContentTypeHostFolderField
): string => {
    if (contentlet?.hostName && contentlet?.url) {
        const path = `${contentlet?.hostName}${contentlet?.url}`;
        const finalPath = path.slice(0, path.indexOf('/content'));

        return `${finalPath}`;
    }

    const value = getValueResolutionFn(contentlet, field);

    if (typeof value === 'string') {
        return value;
    }

    return '';
};

/**
 * Resolves category values from a contentlet.
 *
 * @param contentlet - The contentlet object
 * @param field - The field object
 * @returns Array of category keys or default value
 */
const categoryResolutionFn = (contentlet: DotCMSContentlet, field: ContentTypeCategoryField) => {
    const values = contentlet?.[field.variable];

    if (Array.isArray(values)) {
        return values.map((item) => Object.keys(item)[0]);
    }

    return field.defaultValue ?? [];
};

/**
 * Resolves relationship values from a contentlet.
 *
 * @param contentlet - The contentlet object
 * @param field - The field object
 * @returns Array of related content identifiers
 */
const relationshipResolutionFn = (
    contentlet: DotCMSContentlet,
    field: ContentTypeRelationshipField
): string => {
    const relationship = getRelationshipFromContentlet({
        contentlet,
        variable: field.variable
    });

    return relationship.map((item) => item.identifier).join(',');
};

/**
 * Resolves calendar field values and properly casts to Date objects.
 *
 * @param contentlet - The contentlet object
 * @param field - The field object
 * @returns Properly cast Date object or null
 */
const dateResolutionFn = (
    contentlet: DotCMSContentlet,
    field: ContentTypeDateField | ContentTypeDateTimeField | ContentTypeTimeField
): Date | null => {
    const value = getValueResolutionFn(contentlet, field);

    if (!value) {
        return null;
    }

    if (typeof value === 'string') {
        const parseResult = new Date(value);
        const today = new Date();

        return isNaN(parseResult.getTime()) ? today : parseResult;
    }

    if (typeof value === 'number') {
        const date = new Date(value);

        return isNaN(date.getTime()) ? null : date;
    }

    return null;
};

/**
 * Resolves multi-value field types that need to be split into arrays.
 *
 * @param contentlet - The contentlet object
 * @param field - The field object
 * @returns Array of values
 */
const flattenedFieldResolutionFn = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField
): string[] => {
    const value = getValueResolutionFn(contentlet, field);

    if (!value) {
        return [];
    }

    if (Array.isArray(value)) {
        return value.map((item) => item.trim());
    }

    if (typeof value === 'string') {
        return value.split(',').map((item) => item.trim());
    }

    return [];
};

const selectResolutionFn = (
    contentlet: DotCMSContentlet,
    field: ContentTypeSelectField | ContentTypeRadioField
): boolean | string | number | null => {
    const value = getValueResolutionFn(contentlet, field);

    if (value === '') {
        return null;
    }

    if (field.dataType === DotCMSDataTypes.BOOLEAN) {
        return typeof value === 'boolean' ? value : String(value).toLowerCase().trim() === 'true';
    }

    if (field.dataType === DotCMSDataTypes.FLOAT || field.dataType === DotCMSDataTypes.INTEGER) {
        const num = Number(value);

        return isNaN(num) ? null : num;
    }

    return String(value);
};

/**
 * Maps field types to their corresponding resolution functions.
 *
 * This record is responsible for mapping and transforming the
 * saved value in the contentlet to its corresponding form representation, based on the field type.
 * This enables each field type to properly process its own data.
 */
export const resolutionValue: Record<DotCMSFieldTypes, FnResolutionValue> = {
    [DotCMSFieldTypes.BINARY]: getValueResolutionFn,
    [DotCMSFieldTypes.FILE]: getValueResolutionFn,
    [DotCMSFieldTypes.IMAGE]: getValueResolutionFn,
    [DotCMSFieldTypes.CONSTANT]: getValueResolutionFn,
    [DotCMSFieldTypes.CUSTOM_FIELD]: getValueResolutionFn,
    [DotCMSFieldTypes.HIDDEN]: getValueResolutionFn,
    [DotCMSFieldTypes.DATE]: dateResolutionFn,
    [DotCMSFieldTypes.BLOCK_EDITOR]: getValueResolutionFn,
    [DotCMSFieldTypes.KEY_VALUE]: getValueResolutionFn,
    [DotCMSFieldTypes.DATE_AND_TIME]: dateResolutionFn,
    [DotCMSFieldTypes.TIME]: dateResolutionFn,
    [DotCMSFieldTypes.RADIO]: selectResolutionFn,
    [DotCMSFieldTypes.SELECT]: selectResolutionFn,
    [DotCMSFieldTypes.TEXT]: getValueResolutionFn,
    [DotCMSFieldTypes.TEXTAREA]: getValueResolutionFn,
    [DotCMSFieldTypes.WYSIWYG]: getValueResolutionFn,
    [DotCMSFieldTypes.HOST_FOLDER]: hostFolderResolutionFn,
    [DotCMSFieldTypes.JSON]: jsonResolutionFn,
    [DotCMSFieldTypes.CHECKBOX]: flattenedFieldResolutionFn,
    [DotCMSFieldTypes.MULTI_SELECT]: flattenedFieldResolutionFn,
    [DotCMSFieldTypes.TAG]: flattenedFieldResolutionFn,
    [DotCMSFieldTypes.CATEGORY]: categoryResolutionFn,
    [DotCMSFieldTypes.RELATIONSHIP]: relationshipResolutionFn,
    [DotCMSFieldTypes.LINE_DIVIDER]: () => '',
    [DotCMSFieldTypes.ROW]: () => '',
    [DotCMSFieldTypes.COLUMN]: () => '',
    [DotCMSFieldTypes.TAB_DIVIDER]: () => '',
    [DotCMSFieldTypes.COLUMN_BREAK]: () => ''
};
