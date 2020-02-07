import { h } from './core-bb6a6489.js';
import { i as isStringType, g as getStringFromDotKeyArray } from './index-fca8faa0.js';

const DotFormFields = {
    Text: (field) => (h("dot-textfield", { hint: field.hint, label: field.name, name: field.variable, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, "regex-check": field.regexCheck, required: field.required, value: field.defaultValue })),
    Textarea: (field) => (h("dot-textarea", { hint: field.hint, label: field.name, name: field.variable, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, "regex-check": field.regexCheck, required: field.required, value: field.defaultValue })),
    Checkbox: (field) => (h("dot-checkbox", { hint: field.hint, label: field.name, name: field.variable, options: field.values, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required, value: field.defaultValue })),
    'Multi-Select': (field) => (h("dot-multi-select", { hint: field.hint, label: field.name, name: field.variable, options: field.values, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required, value: field.defaultValue })),
    'Key-Value': (field) => (h("dot-key-value", { "field-type": field.fieldType, hint: field.hint, label: field.name, name: field.variable, required: field.required, value: field.defaultValue })),
    Select: (field) => (h("dot-select", { hint: field.hint, label: field.name, name: field.variable, options: field.values, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required, value: field.defaultValue })),
    Radio: (field) => (h("dot-radio", { hint: field.hint, label: field.name, name: field.variable, options: field.values, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required, value: field.defaultValue })),
    Date: (field) => (h("dot-date", { hint: field.hint, label: field.name, name: field.variable, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required, value: field.defaultValue })),
    Time: (field) => (h("dot-time", { hint: field.hint, label: field.name, name: field.variable, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required, value: field.defaultValue })),
    'Date-and-Time': (field) => (h("dot-date-time", { hint: field.hint, label: field.name, name: field.variable, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required, value: field.defaultValue })),
    'Date-Range': (field) => (h("dot-date-range", { hint: field.hint, label: field.name, name: field.variable, required: field.required, value: field.defaultValue })),
    Tag: (field) => (h("dot-tags", { data: () => {
            return fetch('/api/v1/tags')
                .then((data) => data.json())
                .then((items) => Object.keys(items))
                .catch(() => []);
        }, hint: field.hint, label: field.name, name: field.variable, required: field.required, value: field.defaultValue })),
    Binary: (field) => (h("dot-binary-file", { accept: getFieldVariableValue(field.fieldVariables, 'accept'), "max-file-length": getFieldVariableValue(field.fieldVariables, 'maxFileLength'), hint: field.hint, label: field.name, name: field.variable, ref: (el) => {
            setAttributesToTag(el, field.fieldVariables);
        }, required: field.required }))
};

const DOT_ATTR_PREFIX = "dot";
/**
 * Sets attributes to the HtmlElement from fieldVariables array
 *
 * @param HTMLElement element
 * @param DotCMSContentTypeFieldVariable fieldVariables
 */
function setAttributesToTag(element, fieldVariables) {
    fieldVariables.forEach(({ key, value }) => {
        element.setAttribute(key, value);
    });
}
/**
 * Given a string formatted value "key|value,llave|valor" return an object.
 * @param values
 */
const pipedValuesToObject = (values) => {
    return isStringType(values)
        ? values.split(",").reduce((acc, item) => {
            const [key, value] = item.split("|");
            return Object.assign(Object.assign({}, acc), { [key]: value });
        }, {})
        : null;
};
function isDotAttribute(name) {
    return name.startsWith(DOT_ATTR_PREFIX);
}
/**
 * Sets attributes with "dot" prefix to the HtmlElement passed
 *
 * @param Element element
 * @param Attr[] attributes
 */
function setDotAttributesToElement(element, attributes) {
    attributes.forEach(({ name, value }) => {
        element.setAttribute(name.replace(DOT_ATTR_PREFIX, ""), value);
    });
}
/**
 * Returns "Dot" attributes from all element's attributes
 *
 * @param Attr[] attributes
 * @param string[] attrException
 * @returns Attr[]
 */
function getDotAttributesFromElement(attributes, attrException) {
    const exceptions = attrException.map((attr) => attr.toUpperCase());
    return attributes.filter((item) => !exceptions.includes(item.name.toUpperCase()) &&
        isDotAttribute(item.name));
}
/**
 * Returns if a field should be displayed from a comma separated list of fields
 * @param DotCMSContentTypeField field
 * @returns boolean
 */
const shouldShowField = (field, fieldsToShow) => {
    const fields2Show = fieldsToShow ? fieldsToShow.split(",") : [];
    return !fields2Show.length || fields2Show.includes(field.variable);
};
/**
 * Returns value of a Field Variable from a given key
 * @param DotCMSContentTypeFieldVariable[] fieldVariables
 * @param string key
 * @returns string
 */
const getFieldVariableValue = (fieldVariables, key) => {
    if (fieldVariables && fieldVariables.length) {
        const [variable] = fieldVariables.filter((item) => item.key.toUpperCase() === key.toUpperCase());
        return variable && variable.value;
    }
    return null;
};
/**
 * Parse a string to JSON and returns the message text
 * @param string message
 * @returns string
 */
const getErrorMessage = (message) => {
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
const getFieldsFromLayout = (layout) => {
    return layout.reduce((acc, { columns }) => acc.concat(...columns.map((col) => col.fields)), []);
};
const fieldParamsConversionFromBE = {
    "Key-Value": (field) => {
        if (field.defaultValue && typeof field.defaultValue !== "string") {
            const valuesArray = Object.keys(field.defaultValue).map((key) => {
                return { key: key, value: field.defaultValue[key] };
            });
            field.defaultValue = getStringFromDotKeyArray(valuesArray);
        }
        return DotFormFields["Key-Value"](field);
    }
};
const fieldCustomProcess = {
    "DOT-KEY-VALUE": pipedValuesToObject
};
const fieldMap = {
    Time: DotFormFields.Time,
    Textarea: DotFormFields.Textarea,
    Text: DotFormFields.Text,
    Tag: DotFormFields.Tag,
    Select: DotFormFields.Select,
    Radio: DotFormFields.Radio,
    "Multi-Select": DotFormFields["Multi-Select"],
    "Key-Value": fieldParamsConversionFromBE["Key-Value"],
    "Date-and-Time": DotFormFields["Date-and-Time"],
    "Date-Range": DotFormFields["Date-Range"],
    Date: DotFormFields.Date,
    Checkbox: DotFormFields.Checkbox,
    Binary: DotFormFields.Binary
};

export { DOT_ATTR_PREFIX as D, getErrorMessage as a, getFieldsFromLayout as b, shouldShowField as c, fieldMap as d, fieldCustomProcess as f, getDotAttributesFromElement as g, setDotAttributesToElement as s };
