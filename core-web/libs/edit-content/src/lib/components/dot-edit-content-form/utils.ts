import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

export type FnResolutionValue = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField
) => string | string[];

/**
 * A function that provides a default resolution value for a contentlet field.
 *
 * @param {Object} contentlet - The contentlet object.
 * @param {Object} field - The field object.
 * @returns {*} The resolved value for the field.
 */
const defaultResolutionFn: FnResolutionValue = (contentlet, field) =>
    contentlet?.[field.variable] ?? field.defaultValue;

/**
 * The resolutionValue variable is a record that is responsible for mapping and transforming the
 * saved value in the contentlet to its corresponding form representation, based on the field type.
 * This enables each field type to properly process its own data.
 *
 */
export const resolutionValue: Record<FIELD_TYPES, FnResolutionValue> = {
    [FIELD_TYPES.BINARY]: defaultResolutionFn,
    [FIELD_TYPES.FILE]: defaultResolutionFn,
    [FIELD_TYPES.IMAGE]: defaultResolutionFn,
    [FIELD_TYPES.BLOCK_EDITOR]: defaultResolutionFn,
    [FIELD_TYPES.CHECKBOX]: defaultResolutionFn,
    [FIELD_TYPES.CONSTANT]: defaultResolutionFn,
    [FIELD_TYPES.CUSTOM_FIELD]: defaultResolutionFn,
    [FIELD_TYPES.DATE]: defaultResolutionFn,
    [FIELD_TYPES.DATE_AND_TIME]: defaultResolutionFn,
    [FIELD_TYPES.HIDDEN]: defaultResolutionFn,
    [FIELD_TYPES.HOST_FOLDER]: (contentlet, field) => {
        if (contentlet?.hostName && contentlet?.url) {
            const path = `${contentlet?.hostName}${contentlet?.url}`;
            const finalPath = path.slice(0, path.indexOf('/content'));

            return `//${finalPath}`;
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
    [FIELD_TYPES.TIME]: defaultResolutionFn,
    [FIELD_TYPES.WYSIWYG]: defaultResolutionFn,
    [FIELD_TYPES.CATEGORY]: (contentlet, field) => {
        const values = contentlet?.[field.variable];

        if (Array.isArray(values)) {
            return values.map((item) => Object.keys(item)[0]);
        }

        return field.defaultValue ?? [];
    }
};
