import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutColumn,
    DotCMSContentTypeFieldVariable
} from '@dotcms/dotcms-models';

import { DotFormFields } from './fields';

import { getStringFromDotKeyArray, isStringType } from '../../../utils';

export const DOT_ATTR_PREFIX = 'dot';

/**
 * Sets attributes to the HtmlElement from fieldVariables array
 *
 * @param HTMLElement element
 * @param DotCMSContentTypeFieldVariable fieldVariables
 */
export function setAttributesToTag(
    element: HTMLElement,
    fieldVariables: DotCMSContentTypeFieldVariable[]
): void {
    fieldVariables.forEach(({ key, value }) => {
        element.setAttribute(key, value);
    });
}

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

function isDotAttribute(name: string): boolean {
    return name.startsWith(DOT_ATTR_PREFIX);
}

/**
 * Sets attributes with "dot" prefix to the HtmlElement passed
 *
 * @param Element element
 * @param Attr[] attributes
 */
export function setDotAttributesToElement(element: Element, attributes: Attr[]): void {
    attributes.forEach(({ name, value }) => {
        element.setAttribute(name.replace(DOT_ATTR_PREFIX, ''), value);
    });
}

/**
 * Returns "Dot" attributes from all element's attributes
 *
 * @param Attr[] attributes
 * @param string[] attrException
 * @returns Attr[]
 */
export function getDotAttributesFromElement(attributes: Attr[], attrException: string[]): Attr[] {
    const exceptions = attrException.map((attr: string) => attr.toUpperCase());
    return attributes.filter(
        (item: Attr) => !exceptions.includes(item.name.toUpperCase()) && isDotAttribute(item.name)
    );
}

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
 * Returns value of a Field Variable from a given key
 * @param DotCMSContentTypeFieldVariable[] fieldVariables
 * @param string key
 * @returns string
 */
export const getFieldVariableValue = (
    fieldVariables: DotCMSContentTypeFieldVariable[],
    key: string
): string => {
    const variable = fieldVariables.filter(
        (item: DotCMSContentTypeFieldVariable) => item.key.toUpperCase() === key.toUpperCase()
    )[0];
    return variable && variable.value;
};

/**
 * Parse a string to JSON and returns the message text
 * @param string message
 * @returns string
 */
export const getErrorMessage = (message: string): string => {
    const messageObj = JSON.parse(message);
    return messageObj.errors.length && messageObj.errors[0].message
        ? messageObj.errors[0].message
        : message;
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
