import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import {
    CALENDAR_FIELD_TYPES,
    FLATTENED_FIELD_TYPES
} from '../models/dot-edit-content-field.constant';
import {
    DotEditContentFieldSingleSelectableDataType,
    FIELD_TYPES
} from '../models/dot-edit-content-field.enum';
import { DotEditContentFieldSingleSelectableDataTypes } from '../models/dot-edit-content-field.type';

export const castSingleSelectableValue = (
    value: string,
    type: string
): DotEditContentFieldSingleSelectableDataTypes | null => {
    if (!value) {
        return null;
    }

    if (type === DotEditContentFieldSingleSelectableDataType.BOOL) {
        return value === 'true';
    }

    if (
        type === DotEditContentFieldSingleSelectableDataType.INTEGER ||
        type === DotEditContentFieldSingleSelectableDataType.FLOAT
    ) {
        return Number(value);
    }

    return value;
};

export const getSingleSelectableFieldOptions = (
    options: string,
    dataType: string
): { label: string; value: DotEditContentFieldSingleSelectableDataTypes }[] => {
    const lines = options?.split('\r\n');

    if (lines.length === 0) {
        return [];
    }

    const result = lines?.map((line) => {
        const [label, value] = line.split('|');
        if (!value) {
            return { label, value: castSingleSelectableValue(label, dataType) };
        }

        return { label, value: castSingleSelectableValue(value, dataType) };
    });

    return result;
};

// This function is used to cast the value to a correct type for the Angular Form
export const getFinalCastedValue = (value: string | undefined, field: DotCMSContentTypeField) => {
    if (CALENDAR_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)) {
        const parseResult = new Date(value);

        // When we create a field, we can set the default value to "now" so, it will cast to Invalid Date. But an undefined value can also be casted to Invalid Date.
        // So if the getTime() method returns NaN that means the value is invalid and it's either undefined or "now". Otherwise just return the parsed date.
        return isNaN(parseResult.getTime()) ? value && new Date() : parseResult;
    }

    if (FLATTENED_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)) {
        return value?.split(',').map((value) => value.trim());
    }

    return castSingleSelectableValue(value, field.dataType);
};
