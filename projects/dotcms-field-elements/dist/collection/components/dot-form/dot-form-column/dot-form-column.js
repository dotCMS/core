import { fieldMap, shouldShowField } from '../utils';
export class DotFormColumnComponent {
    render() {
        return this.column.fields.map((field) => this.getField(field));
    }
    getField(field) {
        return shouldShowField(field, this.fieldsToShow) ? this.getFieldTag(field) : '';
    }
    getFieldTag(field) {
        return fieldMap[field.fieldType] ? fieldMap[field.fieldType](field) : '';
    }
    static get is() { return "dot-form-column"; }
    static get properties() { return {
        "column": {
            "type": "Any",
            "attr": "column"
        },
        "fieldsToShow": {
            "type": String,
            "attr": "fields-to-show",
            "reflectToAttr": true
        }
    }; }
    static get style() { return "/**style-placeholder:dot-form-column:**/"; }
}
