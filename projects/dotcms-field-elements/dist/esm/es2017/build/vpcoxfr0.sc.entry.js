import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { b as checkProp, n as getDotOptionsFromFieldValue, a as getOriginalStatus, c as getClassNames, h as getHintId, d as getTagHint, e as getTagError, f as updateStatus, l as getStringFromDotKeyArray } from './chunk-62cd3eff.js';

const mapToKeyValue = ({ label, value }) => {
    return {
        key: label,
        value
    };
};
class DotKeyValueComponent {
    constructor() {
        this.disabled = false;
        this.hint = '';
        this.label = '';
        this.name = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.value = '';
        this.items = [];
    }
    valueWatch() {
        this.value = checkProp(this, 'value', 'string');
        this.items = getDotOptionsFromFieldValue(this.value).map(mapToKeyValue);
    }
    reset() {
        this.items = [];
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitChanges();
    }
    deleteItemHandler(event) {
        event.stopImmediatePropagation();
        this.items = this.items.filter((_item, index) => index !== event.detail);
        this.refreshStatus();
        this.emitChanges();
    }
    addItemHandler({ detail }) {
        this.items = [...this.items, detail];
        this.refreshStatus();
        this.emitChanges();
    }
    componentWillLoad() {
        this.validateProps();
        this.setOriginalStatus();
        this.emitStatusChange();
    }
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, label: this.label, required: this.required, name: this.name },
                h("key-value-form", { onLostFocus: this.blurHandler.bind(this), "add-button-label": this.formAddButtonLabel, disabled: this.isDisabled(), "key-label": this.formKeyLabel, "key-placeholder": this.formKeyPlaceholder, "value-label": this.formValueLabel, "value-placeholder": this.formValuePlaceholder }),
                h("key-value-table", { onClick: (e) => {
                        e.preventDefault();
                    }, "button-label": this.listDeleteLabel, disabled: this.isDisabled(), items: this.items })),
            getTagHint(this.hint),
            getTagError(this.showErrorMessage(), this.getErrorMessage())));
    }
    isDisabled() {
        return this.disabled || null;
    }
    blurHandler() {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }
    validateProps() {
        this.valueWatch();
    }
    setOriginalStatus() {
        this.status = getOriginalStatus(this.isValid());
    }
    isValid() {
        return !(this.required && !this.items.length);
    }
    showErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    getErrorMessage() {
        return this.isValid() ? '' : this.requiredMessage;
    }
    refreshStatus() {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
    }
    emitStatusChange() {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        const returnedValue = getStringFromDotKeyArray(this.items);
        this.valueChange.emit({
            name: this.name,
            value: returnedValue
        });
    }
    emitChanges() {
        this.emitStatusChange();
        this.emitValueChange();
    }
    static get is() { return "dot-key-value"; }
    static get properties() { return {
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "formAddButtonLabel": {
            "type": String,
            "attr": "form-add-button-label",
            "reflectToAttr": true
        },
        "formKeyLabel": {
            "type": String,
            "attr": "form-key-label",
            "reflectToAttr": true
        },
        "formKeyPlaceholder": {
            "type": String,
            "attr": "form-key-placeholder",
            "reflectToAttr": true
        },
        "formValueLabel": {
            "type": String,
            "attr": "form-value-label",
            "reflectToAttr": true
        },
        "formValuePlaceholder": {
            "type": String,
            "attr": "form-value-placeholder",
            "reflectToAttr": true
        },
        "hint": {
            "type": String,
            "attr": "hint",
            "reflectToAttr": true
        },
        "items": {
            "state": true
        },
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
        },
        "listDeleteLabel": {
            "type": String,
            "attr": "list-delete-label",
            "reflectToAttr": true
        },
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
        },
        "required": {
            "type": Boolean,
            "attr": "required",
            "reflectToAttr": true
        },
        "requiredMessage": {
            "type": String,
            "attr": "required-message",
            "reflectToAttr": true
        },
        "reset": {
            "method": true
        },
        "status": {
            "state": true
        },
        "value": {
            "type": String,
            "attr": "value",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["valueWatch"]
        }
    }; }
    static get events() { return [{
            "name": "valueChange",
            "method": "valueChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "statusChange",
            "method": "statusChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get listeners() { return [{
            "name": "delete",
            "method": "deleteItemHandler"
        }, {
            "name": "add",
            "method": "addItemHandler"
        }]; }
    static get style() { return ""; }
}

const DEFAULT_VALUE = { key: '', value: '' };
class DotKeyValueComponent$1 {
    constructor() {
        this.disabled = false;
        this.addButtonLabel = 'Add';
        this.keyPlaceholder = '';
        this.valuePlaceholder = '';
        this.keyLabel = 'Key';
        this.valueLabel = 'Value';
        this.inputs = Object.assign({}, DEFAULT_VALUE);
    }
    render() {
        const buttonDisabled = this.isButtonDisabled();
        return (h("form", { onSubmit: this.addKey.bind(this) },
            h("label", null,
                this.keyLabel,
                h("input", { disabled: this.disabled, name: "key", onBlur: (e) => this.lostFocus.emit(e), onInput: (event) => this.setValue(event), placeholder: this.keyPlaceholder, type: "text", value: this.inputs.key })),
            h("label", null,
                this.valueLabel,
                h("input", { disabled: this.disabled, name: "value", onBlur: (e) => this.lostFocus.emit(e), onInput: (event) => this.setValue(event), placeholder: this.valuePlaceholder, type: "text", value: this.inputs.value })),
            h("button", { class: "key-value-form__save__button", type: "submit", disabled: buttonDisabled }, this.addButtonLabel)));
    }
    isButtonDisabled() {
        return !this.isFormValid() || (this.disabled || null);
    }
    isFormValid() {
        return !!(this.inputs.key.length && this.inputs.value.length);
    }
    setValue(event) {
        event.stopImmediatePropagation();
        const target = event.target;
        this.inputs = Object.assign({}, this.inputs, { [target.name]: target.value.toString() });
    }
    addKey(event) {
        event.preventDefault();
        event.stopImmediatePropagation();
        if (this.inputs.key && this.inputs.value) {
            this.add.emit(this.inputs);
            this.clearForm();
            this.focusKeyInputField();
        }
    }
    clearForm() {
        this.inputs = Object.assign({}, DEFAULT_VALUE);
    }
    focusKeyInputField() {
        const input = this.el.querySelector('input[name="key"]');
        input.focus();
    }
    static get is() { return "key-value-form"; }
    static get properties() { return {
        "addButtonLabel": {
            "type": String,
            "attr": "add-button-label",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "inputs": {
            "state": true
        },
        "keyLabel": {
            "type": String,
            "attr": "key-label",
            "reflectToAttr": true
        },
        "keyPlaceholder": {
            "type": String,
            "attr": "key-placeholder",
            "reflectToAttr": true
        },
        "valueLabel": {
            "type": String,
            "attr": "value-label",
            "reflectToAttr": true
        },
        "valuePlaceholder": {
            "type": String,
            "attr": "value-placeholder",
            "reflectToAttr": true
        }
    }; }
    static get events() { return [{
            "name": "add",
            "method": "add",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "lostFocus",
            "method": "lostFocus",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "key-value-form form{display:-ms-flexbox;display:flex;-ms-flex-align:center;align-items:center}key-value-form form button{margin:0}key-value-form form input{margin:0 1rem 0 .5rem}key-value-form form label{-ms-flex-align:center;align-items:center;display:-ms-flexbox;display:flex}key-value-form form label,key-value-form form label input{-ms-flex-positive:1;flex-grow:1}"; }
}

class KeyValueTableComponent {
    constructor() {
        this.items = [];
        this.disabled = false;
        this.buttonLabel = 'Delete';
        this.emptyMessage = 'No values';
    }
    render() {
        return (h("table", null,
            h("tbody", null, this.renderRows(this.items))));
    }
    onDelete(index) {
        this.delete.emit(index);
    }
    getRow(item, index) {
        const label = `${this.buttonLabel} ${item.key}, ${item.value}`;
        return (h("tr", null,
            h("td", null,
                h("button", { "aria-label": label, disabled: this.disabled || null, onClick: () => this.onDelete(index), class: "dot-key-value__delete-button" }, this.buttonLabel)),
            h("td", null, item.key),
            h("td", null, item.value)));
    }
    renderRows(items) {
        return this.isValidItems(items) ? items.map(this.getRow.bind(this)) : this.getEmptyRow();
    }
    getEmptyRow() {
        return (h("tr", null,
            h("td", null, this.emptyMessage)));
    }
    isValidItems(items) {
        return Array.isArray(items) && !!items.length;
    }
    static get is() { return "key-value-table"; }
    static get properties() { return {
        "buttonLabel": {
            "type": String,
            "attr": "button-label",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "emptyMessage": {
            "type": String,
            "attr": "empty-message",
            "reflectToAttr": true
        },
        "items": {
            "type": "Any",
            "attr": "items"
        }
    }; }
    static get events() { return [{
            "name": "delete",
            "method": "delete",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
}

export { DotKeyValueComponent as DotKeyValue, DotKeyValueComponent$1 as KeyValueForm, KeyValueTableComponent as KeyValueTable };
