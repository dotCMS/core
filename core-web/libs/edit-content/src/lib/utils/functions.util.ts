import { DotEditContentFieldDataType } from '../enums/dot-edit-content-field.enum';

export const mapOptions = (
    input: string,
    dataType: string
): { label: string; value: string | boolean | number }[] => {
    const lines = input?.split('\r\n');

    if (lines.length === 0) {
        return [];
    }

    const result = lines?.map((line) => {
        const [label, value] = line.split('|');
        if (!value) {
            return { label, value: label };
        }

        let newValue: string | number | boolean = value;
        if (
            dataType === DotEditContentFieldDataType.INTEGER ||
            dataType === DotEditContentFieldDataType.FLOAT
        ) {
            newValue = Number(value);
        }

        if (dataType === DotEditContentFieldDataType.BOOL) {
            newValue = value === 'true';
        }

        return { label, value: newValue };
    });

    return result;
};
