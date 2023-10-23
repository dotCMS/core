import { DotEditContentFieldSingleSelectableDataType } from '../models/dot-edit-content-field.enum';
import { DotEditContentFieldSingleSelectableDataTypes } from '../models/dot-edit-content-field.type';

const castValue = (value: string, type: string): DotEditContentFieldSingleSelectableDataTypes => {
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

export const mapSelectableOptions = (
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
            return { label, value: castValue(label, dataType) };
        }

        return { label, value: castValue(value, dataType) };
    });

    return result;
};
