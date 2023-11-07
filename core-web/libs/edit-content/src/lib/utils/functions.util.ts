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
