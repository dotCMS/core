import { h } from '../dotcmsfields.core.js';

import './chunk-62cd3eff.js';
import { g as shouldShowField, h as fieldMap } from './chunk-4205a04e.js';

class DotFormColumnComponent {
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
    static get style() { return "dot-form-column{-ms-flex:1;flex:1;margin:1rem}dot-form-column:first-child{margin-left:0}dot-form-column:last-child{margin-right:0}"; }
}

class DotFormRowComponent {
    render() {
        return this.row.columns.map((fieldColumn) => {
            return h("dot-form-column", { column: fieldColumn, "fields-to-show": this.fieldsToShow });
        });
    }
    static get is() { return "dot-form-row"; }
    static get properties() { return {
        "fieldsToShow": {
            "type": String,
            "attr": "fields-to-show",
            "reflectToAttr": true
        },
        "row": {
            "type": "Any",
            "attr": "row"
        }
    }; }
    static get style() { return "dot-form-row{display:-ms-flexbox;display:flex}"; }
}

export { DotFormColumnComponent as DotFormColumn, DotFormRowComponent as DotFormRow };
