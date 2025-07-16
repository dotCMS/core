import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { getRelationshipFromContentlet } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/utils';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

export type FnResolutionValue<T> = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField
) => T;

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
 * Resolution function for date/time fields that converts timestamps to Date objects
 * This ensures PrimeNG Calendar can properly display the values
 *
 * @param {DotCMSContentlet} contentlet - The contentlet object
 * @param {DotCMSContentTypeField} field - The field object
 * @returns {Date | null} Date object or null if no value
 */
const dateResolutionFn: FnResolutionValue<Date | null> = (contentlet, field) => {
    if (!contentlet) {
        // For new content, use defaultValue if it's a valid date string, otherwise null
        if (field.defaultValue) {
            try {
                return new Date(field.defaultValue);
            } catch {
                return null;
            }
        }
        return null;
    }

    const value = contentlet[field.variable];

    if (value === null || value === undefined || value === '') {
        return null;
    }

    // Handle timestamp (number) - represents server time
    if (typeof value === 'number') {
        // Create Date object directly from timestamp
        // This represents a specific moment in time (epoch time)
        return new Date(value);
    }

    // Handle date string
    if (typeof value === 'string') {
        const parsedDate = new Date(value);
        return isNaN(parsedDate.getTime()) ? null : parsedDate;
    }

    // Handle Date object
    if (value instanceof Date) {
        return value;
    }

    // Fallback: try to convert whatever value we have
    try {
        return new Date(value);
    } catch {
        return null;
    }
};

/**
 * The resolutionValue variable is a record that is responsible for mapping and transforming the
 * saved value in the contentlet to its corresponding form representation, based on the field type.
 * This enables each field type to properly process its own data.
 *
 */
export const resolutionValue: Record<FIELD_TYPES, FnResolutionValue<string | string[] | Date | null>> = {
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
    [FIELD_TYPES.HOST_FOLDER]: (contentlet, field) => {
        if (contentlet?.hostName && contentlet?.url) {
            const path = `${contentlet?.hostName}${contentlet?.url}`;
            const finalPath = path.slice(0, path.indexOf('/content'));

            return `${finalPath}`;
        }

        return field.defaultValue ?? '';
    },
    [FIELD_TYPES.JSON]: defaultResolutionFn,
    [FIELD_TYPES.KEY_VALUE]: defaultResolutionFn,
    [FIELD_TYPES.MULTI_SELECT]: defaultResolutionFn,
    [FIELD_TYPES.RADIO]: defaultResolutionFn,
    [FIELD_TYPES.SELECT]: defaultResolutionFn,
    [FIELD_TYPES.TAG]: defaultResolutionFn,
    [FIELD_TYPES.TEXT]: defaultResolutionFn,
    [FIELD_TYPES.TEXTAREA]: defaultResolutionFn,
    [FIELD_TYPES.WYSIWYG]: defaultResolutionFn,
    [FIELD_TYPES.CATEGORY]: (contentlet, field) => {
        const values = contentlet?.[field.variable];

        if (Array.isArray(values)) {
            return values.map((item) => Object.keys(item)[0]);
        }

        return field.defaultValue ?? [];
    },
    [FIELD_TYPES.RELATIONSHIP]: (contentlet, field) => {
        const relationship = getRelationshipFromContentlet({
            contentlet,
            variable: field.variable
        });

        return relationship.map((item) => item.identifier).join(',');
    },
    [FIELD_TYPES.LINE_DIVIDER]: () => ''
};
