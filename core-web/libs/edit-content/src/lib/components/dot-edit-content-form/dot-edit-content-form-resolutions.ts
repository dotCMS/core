import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { getSingleSelectableFieldOptions } from '../../utils/functions.util';
import { getRelationshipFromContentlet } from '../../utils/relationshipFromContentlet';

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

    // TODO: Remove this once we have a proper solution for the text field from Backend (URL case)
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

    return field.defaultValue || '';
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
 * Resolution function for date/time fields
 * Backend always returns numeric timestamps when value exists, or the field is not included
 *
 * @param {DotCMSContentlet} contentlet - The contentlet object
 * @param {DotCMSContentTypeField} field - The field object
 * @returns {number | null} Numeric timestamp or null if no value
 */
const dateResolutionFn: FnResolutionValue<number | null> = (contentlet, field) => {
    if (!contentlet) {
        // For new content, let the calendar component handle defaultValue processing
        // The calendar component has proper logic for "now" and fixed dates with server timezone
        return null;
    }

    const value = contentlet[field.variable];

    // If field doesn't exist in contentlet or is explicitly null/undefined/empty
    if (value === null || value === undefined || value === '') {
        return null;
    }

    // Backend should always return number timestamps
    if (typeof value === 'number') {
        // Validate it's a reasonable timestamp (not NaN or invalid)
        return isNaN(value) || !isFinite(value) ? null : value;
    }

    // Handle edge cases where backend might return string timestamps
    if (typeof value === 'string') {
        const numericValue = Number(value);
        if (!isNaN(numericValue) && isFinite(numericValue)) {
            return numericValue;
        }

        console.warn(`Calendar field received unexpected string value from backend:`, {
            fieldVariable: field.variable,
            value: value,
            type: typeof value
        });
        return null;
    }

    // Handle unexpected Date objects (shouldn't happen from backend)
    if (value instanceof Date) {
        const timestamp = value.getTime();
        if (!isNaN(timestamp)) {
            console.warn(`Calendar field received Date object instead of timestamp from backend:`, {
                fieldVariable: field.variable,
                value: value,
                convertedTimestamp: timestamp
            });
            return timestamp;
        }
        return null;
    }

    // Log unexpected value types
    console.error(`Calendar field received unexpected value type from backend:`, {
        fieldVariable: field.variable,
        value: value,
        type: typeof value
    });

    return null;
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

const selectResolutionFn: FnResolutionValue<string> = (contentlet, field) => {
    const value = contentlet
        ? (contentlet[field.variable] ?? field.defaultValue)
        : field.defaultValue;
    if (value === null || value === undefined || value === '') {
        const options = getSingleSelectableFieldOptions(field?.values || '', field.dataType);
        return options[0]?.value;
    }
    return value;
};

/**
 * The resolutionValue variable is a record that is responsible for mapping and transforming the
 * saved value in the contentlet to its corresponding form representation, based on the field type.
 * This enables each field type to properly process its own data.
 *
 */
export const resolutionValue: Record<
    FIELD_TYPES,
    FnResolutionValue<string | string[] | Date | number | null>
> = {
    [FIELD_TYPES.BINARY]: defaultResolutionFn,
    [FIELD_TYPES.FILE]: defaultResolutionFn,
    [FIELD_TYPES.IMAGE]: defaultResolutionFn,
    [FIELD_TYPES.BLOCK_EDITOR]: defaultResolutionFn,
    [FIELD_TYPES.CHECKBOX]: defaultResolutionFn,
    [FIELD_TYPES.CONSTANT]: defaultResolutionFn,
    [FIELD_TYPES.CUSTOM_FIELD]: defaultResolutionFn,
    [FIELD_TYPES.DATE]: dateResolutionFn,
    [FIELD_TYPES.DATE_AND_TIME]: dateResolutionFn,
    [FIELD_TYPES.TIME]: dateResolutionFn,
    [FIELD_TYPES.HIDDEN]: defaultResolutionFn,
    [FIELD_TYPES.HOST_FOLDER]: hostFolderResolutionFn,
    [FIELD_TYPES.JSON]: defaultResolutionFn,
    [FIELD_TYPES.KEY_VALUE]: defaultResolutionFn,
    [FIELD_TYPES.MULTI_SELECT]: defaultResolutionFn,
    [FIELD_TYPES.RADIO]: defaultResolutionFn,
    [FIELD_TYPES.SELECT]: selectResolutionFn,
    [FIELD_TYPES.TAG]: defaultResolutionFn,
    [FIELD_TYPES.TEXT]: textFieldResolutionFn,
    [FIELD_TYPES.TEXTAREA]: defaultResolutionFn,
    [FIELD_TYPES.WYSIWYG]: defaultResolutionFn,
    [FIELD_TYPES.CATEGORY]: categoryResolutionFn,
    [FIELD_TYPES.RELATIONSHIP]: relationshipResolutionFn,
    [FIELD_TYPES.LINE_DIVIDER]: emptyResolutionFn
};
