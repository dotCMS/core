import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

export type FnResolutionValue = (
    contentlet: DotCMSContentlet,
    field: DotCMSContentTypeField
) => string | string[];

const defaultResolutionFn: FnResolutionValue = (contentlet, field) =>
    contentlet?.[field.variable] ?? field.defaultValue;

export const resolutionValue: Record<FIELD_TYPES, FnResolutionValue> = {
    [FIELD_TYPES.BINARY]: defaultResolutionFn,
    [FIELD_TYPES.BLOCK_EDITOR]: defaultResolutionFn,
    [FIELD_TYPES.CHECKBOX]: defaultResolutionFn,
    [FIELD_TYPES.CUSTOM_FIELD]: defaultResolutionFn,
    [FIELD_TYPES.DATE]: defaultResolutionFn,
    [FIELD_TYPES.DATE_AND_TIME]: defaultResolutionFn,
    [FIELD_TYPES.HOST_FOLDER]: (contentlet, field) => {
        if (contentlet?.hostName && contentlet?.url) {
            const path = `${contentlet?.hostName}${contentlet?.url}`;

            return path.slice(0, path.indexOf('/content'));
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
