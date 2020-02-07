import { r as registerInstance } from './core-bb6a6489.js';
import './index-fca8faa0.js';
import { c as shouldShowField, d as fieldMap } from './index-b2e001f1.js';

const DotFormColumnComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
    }
    render() {
        return this.column.fields.map((field) => this.getField(field));
    }
    getField(field) {
        return shouldShowField(field, this.fieldsToShow) ? this.getFieldTag(field) : null;
    }
    getFieldTag(field) {
        return fieldMap[field.fieldType] ? fieldMap[field.fieldType](field) : '';
    }
    static get style() { return "dot-form-column {\n  -ms-flex: 1;\n  flex: 1;\n  margin: 1rem;\n}\ndot-form-column:first-child {\n  margin-left: 0;\n}\ndot-form-column:last-child {\n  margin-right: 0;\n}"; }
};

export { DotFormColumnComponent as dot_form_column };
