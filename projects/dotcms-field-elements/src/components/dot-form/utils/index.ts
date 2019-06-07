import { DotCMSContentTypeField } from '../models';
import { DotFormFields } from './fields';
import { getStringFromDotKeyArray, isStringType } from '../../../utils';

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
    'Time': DotFormFields.Time,
    'Textarea': DotFormFields.Textarea,
    'Text': DotFormFields.Text,
    'Tag': DotFormFields.Tag,
    'Select': DotFormFields.Select,
    'Radio': DotFormFields.Radio,
    'Multi-Select': DotFormFields['Multi-Select'],
    'Key-Value': fieldParamsConversionFromBE['Key-Value'],
    'Date-and-Time': DotFormFields['Date-and-Time'],
    'Date-Range': DotFormFields['Date-Range'],
    'Date': DotFormFields.Date,
    'Checkbox': DotFormFields.Checkbox
};
