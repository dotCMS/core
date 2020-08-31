import { DotFormFields } from './fields';
import { getStringFromDotKeyArray, isStringType } from '../../../utils';
export const DOT_ATTR_PREFIX = 'dot';
export function setAttributesToTag(element, fieldVariables) {
    fieldVariables.forEach(({ key, value }) => {
        element.setAttribute(key, value);
    });
}
const pipedValuesToObject = (values) => {
    return isStringType(values)
        ? values.split(',').reduce((acc, item) => {
            const [key, value] = item.split('|');
            return Object.assign({}, acc, { [key]: value });
        }, {})
        : null;
};
function isDotAttribute(name) {
    return name.startsWith(DOT_ATTR_PREFIX);
}
export function setDotAttributesToElement(element, attributes) {
    attributes.forEach(({ name, value }) => {
        element.setAttribute(name.replace(DOT_ATTR_PREFIX, ''), value);
    });
}
export function getDotAttributesFromElement(attributes, attrException) {
    const exceptions = attrException.map((attr) => attr.toUpperCase());
    return attributes.filter((item) => !exceptions.includes(item.name.toUpperCase()) && isDotAttribute(item.name));
}
export const shouldShowField = (field, fieldsToShow) => {
    const fields2Show = fieldsToShow ? fieldsToShow.split(',') : [];
    return !fields2Show.length || fields2Show.includes(field.variable);
};
export const getFieldVariableValue = (fieldVariables, key) => {
    const variable = fieldVariables.filter((item) => item.key.toUpperCase() === key.toUpperCase())[0];
    return variable && variable.value;
};
export const getErrorMessage = (message) => {
    const messageObj = JSON.parse(message);
    return messageObj.errors.length && messageObj.errors[0].message
        ? messageObj.errors[0].message
        : message;
};
export const getFieldsFromLayout = (layout) => {
    return layout.reduce((acc, { columns }) => acc.concat(...columns.map((col) => col.fields)), []);
};
const fieldParamsConversionFromBE = {
    'Key-Value': (field) => {
        if (field.defaultValue && typeof field.defaultValue !== 'string') {
            const valuesArray = Object.keys(field.defaultValue).map((key) => {
                return { key: key, value: field.defaultValue[key] };
            });
            field.defaultValue = getStringFromDotKeyArray(valuesArray);
        }
        return DotFormFields['Key-Value'](field);
    }
};
export const fieldCustomProcess = {
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
    Checkbox: DotFormFields.Checkbox,
    Binary: DotFormFields.Binary
};
