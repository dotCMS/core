import { DotFormFields } from './fields';
import { getStringFromDotKeyArray, isStringType } from '../../../utils';
import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutColumn
} from 'dotcms-models';

/**
 * Given a string formatted value "key|value,llave|valor" return an object.
 * @param values
 */
const pipedValuesToObject = (values: string): { [key: string]: string } => {
    return isStringType(values)
        ? values.split(',').reduce((acc, item) => {
              const [key, value] = item.split('|');
              return {
                  ...acc,
                  [key]: value
              };
          }, {})
        : null;
};

/**
 * Returns if a field should be displayed from a comma separated list of fields
 * @param DotCMSContentTypeField field
 * @returns boolean
 */
export const shouldShowField = (field: DotCMSContentTypeField, fieldsToShow: string): boolean => {
    const fields2Show = fieldsToShow ? fieldsToShow.split(',') : [];
    return !fields2Show.length || fields2Show.includes(field.variable);
};

/**
 * Given a layout Object of fields, it returns a flat list of fields
 * @param DotCMSContentTypeLayoutRow[] layout
 * @returns DotCMSContentTypeField[]
 */
export const getFieldsFromLayout = (
    layout: DotCMSContentTypeLayoutRow[]
): DotCMSContentTypeField[] => {
    return layout.reduce(
        (acc: DotCMSContentTypeField[], { columns }: DotCMSContentTypeLayoutRow) =>
            acc.concat(...columns.map((col: DotCMSContentTypeLayoutColumn) => col.fields)),
        []
    );
};

const fieldParamsConversionFromBE = {
    'Key-Value': (field: DotCMSContentTypeField) => {
        if (field.defaultValue && typeof field.defaultValue !== 'string') {
            const valuesArray = Object.keys(field.defaultValue).map((key: string) => {
                return { key: key, value: field.defaultValue[key] };
            });
            field.defaultValue = getStringFromDotKeyArray(valuesArray);
        }
        return DotFormFields['Key-Value'](field);
    }
};

export const fieldParamsConversionToBE = {
    'DOT-KEY-VALUE': pipedValuesToObject
};

export const fieldMap = {
    Time: DotFormFields.Time,
    Textarea: DotFormFields.Textarea,
    Text: DotFormFields.Text,
    Tag: DotFormFields.Tag,
    Select: DotFormFields.Select,
    Radio: DotFormFields.Radio,
    'Multi-Select': DotFormFields['Multi-Select'],
    'Key-Value': fieldParamsConversionFromBE['Key-Value'],
    'Date-and-Time': DotFormFields['Date-and-Time'],
    'Date-Range': DotFormFields['Date-Range'],
    Date: DotFormFields.Date,
    Checkbox: DotFormFields.Checkbox
};
