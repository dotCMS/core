import { DotEditContentFieldSelectableDataType } from '../enums/dot-edit-content-field.enum';

const castValue = (value: string, type: string) => {
    if (type === DotEditContentFieldSelectableDataType.BOOL) {
        return value === 'true';
    }

    if (
        type === DotEditContentFieldSelectableDataType.INTEGER ||
        type === DotEditContentFieldSelectableDataType.FLOAT
    ) {
        return Number(value);
    }

    return value;
};

export const mapSelectableOptions = (
    options: string,
    dataType: string
): { label: string; value: string | boolean | number }[] => {
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
