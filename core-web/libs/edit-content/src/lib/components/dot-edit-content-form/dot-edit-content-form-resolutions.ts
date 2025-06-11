import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { getRelationshipFromContentlet } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/utils';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
export type FnResolutionValue<T> = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField
) => T;

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @returns {*} The resolved value for the field.
 */
const emptyResolutionFn: FnResolutionValue<string> = () => '';

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const defaultResolutionFn: FnResolutionValue<string> = (contentlet, field) =>
    contentlet ? (contentlet[field.variable] ?? field.defaultValue) : field.defaultValue;

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const textFieldResolutionFn: FnResolutionValue<string> = (contentlet, field) => {
    const value = contentlet
        ? (contentlet[field.variable] ?? field.defaultValue)
        : field.defaultValue;

    // Remove leading "/" if present
    return typeof value === 'string' && value.startsWith('/') ? value.substring(1) : value;
};

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const hostFolderResolutionFn: FnResolutionValue<string> = (contentlet, field) => {
    if (contentlet?.hostName && contentlet?.url) {
        const path = `${contentlet?.hostName}${contentlet?.url}`;
        const finalPath = path.slice(0, path.indexOf('/content'));

        return `${finalPath}`;
    }

    return field.defaultValue ?? '';
};

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const categoryResolutionFn: FnResolutionValue<string[] | string> = (contentlet, field) => {
    const values = contentlet?.[field.variable];

    if (Array.isArray(values)) {
        return values.map((item) => Object.keys(item)[0]);
    }

    return field.defaultValue ?? [];
};

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const relationshipResolutionFn: FnResolutionValue<string> = (contentlet, field) => {
    const relationship = getRelationshipFromContentlet({
        contentlet,
        variable: field.variable
    });

    return relationship.map((item) => item.identifier).join(',');
};

/**
 * The resolutionValue variable is a record that is responsible for mapping and transforming the
 * saved value in the contentlet to its corresponding form representation, based on the field type.
 * This enables each field type to properly process its own data.
 *
 */
export const resolutionValue: Record<FIELD_TYPES, FnResolutionValue<string | string[] | Date>> = {
    [FIELD_TYPES.BINARY]: defaultResolutionFn,
    [FIELD_TYPES.FILE]: defaultResolutionFn,
    [FIELD_TYPES.IMAGE]: defaultResolutionFn,
    [FIELD_TYPES.BLOCK_EDITOR]: defaultResolutionFn,
    [FIELD_TYPES.CHECKBOX]: defaultResolutionFn,
    [FIELD_TYPES.CONSTANT]: defaultResolutionFn,
    [FIELD_TYPES.CUSTOM_FIELD]: defaultResolutionFn,
    [FIELD_TYPES.DATE]: defaultResolutionFn,
    [FIELD_TYPES.DATE_AND_TIME]: defaultResolutionFn,
    [FIELD_TYPES.TIME]: defaultResolutionFn,
    [FIELD_TYPES.HIDDEN]: defaultResolutionFn,
    [FIELD_TYPES.HOST_FOLDER]: hostFolderResolutionFn,
    [FIELD_TYPES.JSON]: defaultResolutionFn,
    [FIELD_TYPES.KEY_VALUE]: defaultResolutionFn,
    [FIELD_TYPES.MULTI_SELECT]: defaultResolutionFn,
    [FIELD_TYPES.RADIO]: defaultResolutionFn,
    [FIELD_TYPES.SELECT]: defaultResolutionFn,
    [FIELD_TYPES.TAG]: defaultResolutionFn,
    [FIELD_TYPES.TEXT]: textFieldResolutionFn,
    [FIELD_TYPES.TEXTAREA]: defaultResolutionFn,
    [FIELD_TYPES.WYSIWYG]: defaultResolutionFn,
    [FIELD_TYPES.CATEGORY]: categoryResolutionFn,
    [FIELD_TYPES.RELATIONSHIP]: relationshipResolutionFn,
    [FIELD_TYPES.LINE_DIVIDER]: emptyResolutionFn
};
