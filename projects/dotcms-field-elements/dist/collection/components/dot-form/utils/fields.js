import { getFieldVariableValue, setAttributesToTag } from '.';
export const DotFormFields = {
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
